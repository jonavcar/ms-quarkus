@echo off
title Menu de Kafka
cd /d D:\tools\kafka

:menu
cls
echo ================================================
echo        MENU DE ADMINISTRACION DE KAFKA
echo ================================================
echo.
echo   1. Iniciar Kafka
echo   2. Detener Kafka
echo   3. Reiniciar Kafka
echo   4. Verificar estado (puerto 9092)
echo   5. Ver topics
echo   6. Crear topic de prueba
echo   7. Abrir productor de consola
echo   8. Abrir consumidor de consola
echo   9. Limpiar logs
echo   0. Salir
echo.
echo ================================================
set /p opcion=Selecciona una opcion: 

if "%opcion%"=="1" goto iniciar
if "%opcion%"=="2" goto detener
if "%opcion%"=="3" goto reiniciar
if "%opcion%"=="4" goto verificar
if "%opcion%"=="5" goto listar
if "%opcion%"=="6" goto crear
if "%opcion%"=="7" goto productor
if "%opcion%"=="8" goto consumidor
if "%opcion%"=="9" goto limpiar
if "%opcion%"=="0" exit
goto menu

:iniciar
echo.
echo Iniciando Kafka...
start "Kafka Server" cmd /k ".\bin\windows\kafka-server-start.bat .\config\kraft-server.properties"
timeout /t 3 >nul
goto menu

:detener
echo.
echo Deteniendo Kafka...
.\bin\windows\kafka-server-stop.bat
timeout /t 3 >nul
goto menu

:reiniciar
echo.
echo Reiniciando Kafka...
.\bin\windows\kafka-server-stop.bat
timeout /t 10 >nul
start "Kafka Server" cmd /k ".\bin\windows\kafka-server-start.bat .\config\kraft-server.properties"
timeout /t 3 >nul
goto menu

:verificar
echo.
echo Verificando puerto 9092...
netstat -ano | findstr :9092
if errorlevel 1 (
    echo Puerto 9092 no esta en uso - Kafka detenido
) else (
    echo Puerto 9092 en uso - Kafka ejecutandose
)
echo.
pause
goto menu

:listar
echo.
echo Listando topics...
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
echo.
pause
goto menu

:crear
echo.
set /p topicname=Nombre del topic: 
.\bin\windows\kafka-topics.bat --create --topic %topicname% --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
echo.
pause
goto menu

:productor
echo.
set /p topicname=Nombre del topic: 
start "Kafka Producer" cmd /k ".\bin\windows\kafka-console-producer.bat --topic %topicname% --bootstrap-server localhost:9092"
goto menu

:consumidor
echo.
set /p topicname=Nombre del topic: 
start "Kafka Consumer" cmd /k ".\bin\windows\kafka-console-consumer.bat --topic %topicname% --from-beginning --bootstrap-server localhost:9092"
goto menu

:limpiar
echo.
echo ADVERTENCIA: Esto eliminara todos los logs y datos de Kafka
set /p confirm=Estas seguro? (S/N): 
if /i "%confirm%"=="S" (
    echo Limpiando logs...
    rmdir /s /q "D:\tools\kafka\data\kraft-logs"
    echo Logs eliminados. Necesitas reformatear antes de iniciar.
    echo Ejecuta: .\bin\windows\kafka-storage.bat format -t [UUID] -c .\config\kraft-server.properties --standalone
)
echo.
pause
goto menu
