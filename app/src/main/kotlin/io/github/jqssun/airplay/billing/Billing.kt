package io.github.jqssun.airplay.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.net.Uri
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
import io.github.jqssun.airplay.billing.Entitlement.LIFETIME_ID
import io.github.jqssun.airplay.billing.Entitlement.MONTHLY_BASE_PLAN
import io.github.jqssun.airplay.billing.Entitlement.MONTHLY_ID
import io.github.jqssun.airplay.get
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Screenbridge Unlimited: the Monthly subscription or the Lifetime one-time product, either one unlocks.
 * A plain "does this Google account own it" check, no obfuscation or DRM (the code is GPL; anyone may
 * build it without the limit).
 */
object Billing : PurchasesUpdatedListener {
    private const val TAG = "Billing"
    // not in Prefs.all, so the adb settings receiver can't set it
    private const val CACHE_KEY = "unlimited_owned"

    /** Monthly as Play offers it to this account: [trialDays] only when the free trial applies. */
    data class MonthlyOffer(val price: String, val trialDays: Int?)

    private lateinit var _app: Context
    private lateinit var _prefs: SharedPreferences
    private lateinit var _client: BillingClient
    private val _scope = MainScope()
    private var _monthlyDetails: ProductDetails? = null
    private var _monthlyToken: String? = null
    private var _lifetimeDetails: ProductDetails? = null
    private var _playOwned: Boolean? = null

    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()
    // which product unlocked it (Lifetime wins over Monthly); null when free or only known from the cache
    private val _plan = MutableStateFlow<String?>(null)
    val plan = _plan.asStateFlow()
    // prices come from Play, localized; null until Play answers
    private val _monthly = MutableStateFlow<MonthlyOffer?>(null)
    val monthly = _monthly.asStateFlow()
    private val _lifetimePrice = MutableStateFlow<String?>(null)
    val lifetimePrice = _lifetimePrice.asStateFlow()
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
        _unlocked.value = Entitlement.unlocked(_playOwned, _prefs.getBoolean(CACHE_KEY, false), fake)
    }

    /** Ask Play what this account owns (app start, every resume, Restore). [onDone] gets null if Play is unreachable. */
    fun refresh(onDone: ((Boolean?) -> Unit)? = null) {
        if (!::_client.isInitialized) return
        if (!_client.isReady) {
            _client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingResponseCode.OK) _scope.launch { _query(onDone) }
                    else { Log.w(TAG, "setup failed: ${result.debugMessage}"); _scope.launch { _noPlay(); onDone?.invoke(null) } }
                }
                override fun onBillingServiceDisconnected() {}
            })
            return
        }
        _scope.launch { _query(onDone) }
    }

    private suspend fun _query(onDone: ((Boolean?) -> Unit)?) {
        val subs = _client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
        val inapp = _client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        val ok = subs.billingResult.responseCode == BillingResponseCode.OK && inapp.billingResult.responseCode == BillingResponseCode.OK
        _handle(subs.purchasesList + inapp.purchasesList, authoritative = ok)
        onDone?.invoke(if (ok) _playOwned else null)

        _monthlyDetails = _details(MONTHLY_ID, ProductType.SUBS)
        _lifetimeDetails = _details(LIFETIME_ID, ProductType.INAPP)
        val offer = _monthlyDetails?.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == MONTHLY_BASE_PLAN }
            // Play lists the trial offer only to accounts that may still use it
            .sortedByDescending { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            .firstOrNull()
        _monthlyToken = offer?.offerToken
        _monthly.value = offer?.let { o ->
            val phases = o.pricingPhases.pricingPhaseList
            MonthlyOffer(
                price = phases.last().formattedPrice,
                trialDays = phases.firstOrNull { it.priceAmountMicros == 0L }?.let { Entitlement.trialDays(it.billingPeriod) },
            )
        }
        _lifetimePrice.value = _lifetimeDetails?.oneTimePurchaseOfferDetails?.formattedPrice
        if (_monthly.value == null && _lifetimePrice.value == null) _noPlay()
    }

    private suspend fun _details(id: String, type: String): ProductDetails? {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
        val r = _client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
        return r.productDetailsList?.firstOrNull { it.productId == id }
    }

    // debug builds without a Play-signed install get sample prices so the paywall layout can be checked
    private fun _noPlay() {
        if (!debuggable(_app) || !_prefs[Prefs.DEBUG_SAMPLE_PRICES]) return
        _monthly.value = MonthlyOffer("$4.99", 7)
        _lifetimePrice.value = "$49.99"
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> _scope.launch { _handle(purchases.orEmpty(), authoritative = false) }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingResponseCode.USER_CANCELED -> Unit
            else -> Log.w(TAG, "purchase failed ${result.responseCode}: ${result.debugMessage}")
        }
    }

    private suspend fun _handle(purchases: List<Purchase>, authoritative: Boolean) {
        val mine = purchases.filter { p -> p.products.any { it == MONTHLY_ID || it == LIFETIME_ID } }
        val bought = mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val ids = bought.flatMap { it.products }.toSet()
        _pending.value = mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (authoritative || bought.isNotEmpty()) {
            val wasUnlocked = _unlocked.value
            _playOwned = Entitlement.unlocks(ids)
            _plan.value = if (LIFETIME_ID in ids) LIFETIME_ID else if (MONTHLY_ID in ids) MONTHLY_ID else null
            _prefs.edit { putBoolean(CACHE_KEY, _playOwned!!) }
            _recompute()
            if (!authoritative && !wasUnlocked && _unlocked.value) Toast.makeText(_app, R.string.unlimited_thanks, Toast.LENGTH_LONG).show()
        }
        // Play refunds purchases left unacknowledged for 3 days; a failure is retried on the next refresh
        bought.filterNot { it.isAcknowledged }.forEach {
            val r = _client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
            if (r.responseCode != BillingResponseCode.OK) Log.w(TAG, "acknowledge failed: ${r.debugMessage}")
        }
    }

    /** Opens Google Play's purchase sheet for [productId] (Monthly or Lifetime). */
    fun buy(activity: Activity, productId: String) {
        val d = if (productId == MONTHLY_ID) _monthlyDetails else _lifetimeDetails
        val token = if (productId == MONTHLY_ID) _monthlyToken else null
        if (d == null || !_client.isReady || (productId == MONTHLY_ID && token == null)) {
            Toast.makeText(activity, R.string.unlimited_play_unavailable, Toast.LENGTH_LONG).show()
            refresh()
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
            .apply { token?.let(::setOfferToken) }.build()
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

    /** Google Play's page for cancelling or changing the Monthly subscription. */
    fun manageSubscription(activity: Activity) {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=$MONTHLY_ID&package=${activity.packageName}")
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: Exception) { Toast.makeText(activity, R.string.unlimited_manage_in_play, Toast.LENGTH_LONG).show() }
    }
}
