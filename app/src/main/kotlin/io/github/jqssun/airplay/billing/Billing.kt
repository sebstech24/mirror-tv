package io.github.jqssun.airplay.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.get
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "Screenbridge Unlimited": one non-consumable Play product. A plain "did this Google account buy it"
 * check, no obfuscation or DRM (the code is GPL; anyone may build it without the limit).
 */
object Billing : PurchasesUpdatedListener {
    const val PRODUCT_ID = "screenbridge_unlimited"
    private const val TAG = "Billing"
    // not in Prefs.all, so the adb settings receiver can't set it
    private const val CACHE_KEY = "unlimited_owned"

    private lateinit var _app: Context
    private lateinit var _prefs: SharedPreferences
    private lateinit var _client: BillingClient
    private val _scope = MainScope()
    private var _details: ProductDetails? = null
    private var _playOwned: Boolean? = null

    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()
    // localized price from Play ("$7.99"); null until Play answers
    private val _price = MutableStateFlow<String?>(null)
    val price = _price.asStateFlow()
    // bought with a slow payment method (cash, bank): unlocks once Play confirms
    private val _pending = MutableStateFlow(false)
    val pending = _pending.asStateFlow()

    fun debuggable(ctx: Context) = ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun init(context: Context) {
        _app = context.applicationContext
        _prefs = _app.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        _client = BillingClient.newBuilder(_app)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        _recompute()
        // debug fake-unlock flips live
        _prefs.registerOnSharedPreferenceChangeListener(_prefsListener)
        refresh()
    }

    private val _prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.DEBUG_FAKE_UNLOCKED.key) _recompute()
    }

    private fun _recompute() {
        val fake = debuggable(_app) && _prefs[Prefs.DEBUG_FAKE_UNLOCKED]
        _unlocked.value = SessionLimiter.unlocked(_playOwned, _prefs.getBoolean(CACHE_KEY, false), fake)
    }

    /** Ask Play what this account owns (app start, every resume, Restore). [onDone] gets null if Play is unreachable. */
    fun refresh(onDone: ((Boolean?) -> Unit)? = null) {
        if (!::_client.isInitialized) return
        if (!_client.isReady) {
            _client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingResponseCode.OK) _scope.launch { _query(onDone) }
                    else { Log.w(TAG, "setup failed: ${result.debugMessage}"); _scope.launch { onDone?.invoke(null) } }
                }
                override fun onBillingServiceDisconnected() {}
            })
            return
        }
        _scope.launch { _query(onDone) }
    }

    private suspend fun _query(onDone: ((Boolean?) -> Unit)?) {
        val purchases = _client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        if (purchases.billingResult.responseCode == BillingResponseCode.OK) _handle(purchases.purchasesList, authoritative = true)
        onDone?.invoke(if (purchases.billingResult.responseCode == BillingResponseCode.OK) _playOwned else null)

        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(ProductType.INAPP).build()
        val details = _client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        _details = details.productDetailsList?.firstOrNull { it.productId == PRODUCT_ID }
        _price.value = _offer(_details)?.formattedPrice
    }

    // cheapest offer: picks up the launch discount when one runs
    private fun _offer(d: ProductDetails?) =
        d?.oneTimePurchaseOfferDetailsList?.minByOrNull { it.priceAmountMicros } ?: d?.oneTimePurchaseOfferDetails

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> _scope.launch { _handle(purchases.orEmpty(), authoritative = false) }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingResponseCode.USER_CANCELED -> Unit
            else -> Log.w(TAG, "purchase failed ${result.responseCode}: ${result.debugMessage}")
        }
    }

    private suspend fun _handle(purchases: List<Purchase>, authoritative: Boolean) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val bought = mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _pending.value = mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (authoritative || bought.isNotEmpty()) {
            _playOwned = bought.isNotEmpty()
            _prefs.edit { putBoolean(CACHE_KEY, _playOwned!!) }
            _recompute()
        }
        // Play refunds purchases left unacknowledged for 3 days; a failure is retried on the next refresh
        bought.filterNot { it.isAcknowledged }.forEach {
            val r = _client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
            if (r.responseCode != BillingResponseCode.OK) Log.w(TAG, "acknowledge failed: ${r.debugMessage}")
        }
    }

    /** Opens Google Play's purchase sheet. */
    fun buy(activity: Activity) {
        val d = _details
        val offer = _offer(d)
        if (d == null || !_client.isReady) {
            Toast.makeText(activity, R.string.unlimited_play_unavailable, Toast.LENGTH_LONG).show()
            refresh()
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
            .apply { offer?.offerToken?.let(::setOfferToken) }.build()
        val r = _client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        if (r.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) refresh()
        else if (r.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "launch failed ${r.responseCode}: ${r.debugMessage}")
            Toast.makeText(activity, R.string.unlimited_play_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    fun restore(activity: Activity) = refresh { owned ->
        val msg = when (owned) {
            true -> R.string.unlimited_restored
            false -> R.string.unlimited_not_found
            null -> R.string.unlimited_play_unavailable
        }
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }
}
