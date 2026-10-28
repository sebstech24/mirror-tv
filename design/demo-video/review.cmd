@echo off
rem MAIN PC: lint + still frames for review (run through the job hub, not on the Mac)
cd /d "%~dp0"
if not exist review mkdir review
call npx --yes hyperframes@0.8.189 check --json > review\check.json 2>&1
type review\check.json
call npx --yes hyperframes@0.8.189 snapshot --at 1.6,4.2,5.9,7.7,9.6,10.75,11.75,13.4,15.2,16.55,17.6,20.5 --output review\snaps
echo MC_PROGRESS 1.0
