@echo off
rem Vizitor - stop the API service (scheduled task VizitorAPI)
schtasks /End /TN "VizitorAPI" >nul 2>&1 && (echo [ ok ] VizitorAPI stopped) || (echo [!!] could not stop VizitorAPI)
timeout /t 2 >nul
