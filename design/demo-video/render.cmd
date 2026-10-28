@echo off
rem MAIN PC: final render (run through the job hub, not on the Mac)
cd /d "%~dp0"
if not exist renders mkdir renders
call npx --yes hyperframes@0.8.189 render --quality delivery --fps 30 --output renders\telebeam-demo-3-clicks.mp4
if errorlevel 1 exit /b 1
echo MC_OUTPUT %~dp0renders\telebeam-demo-3-clicks.mp4
