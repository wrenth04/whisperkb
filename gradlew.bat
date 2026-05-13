@echo off
setlocal
set ROOT_DIR=%~dp0
bash "%ROOT_DIR%gradlew" %*
