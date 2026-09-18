@echo off
rem Vizitor - start the API service (scheduled task VizitorAPI)
schtasks /Run /TN "VizitorAPI" >nul 2>&1 && (echo [ ok ] VizitorAPI started) || (echo [!!] could not start VizitorAPI)
timeout /t 2 >nul
