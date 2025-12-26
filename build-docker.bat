@echo off
echo ========================================
echo Building Flight Management System Docker Images
echo ========================================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and add it to your PATH
    pause
    exit /b 1
)

REM Check if Docker is available
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker is not installed or not in PATH
    echo Please install Docker Desktop and ensure it's running
    pause
    exit /b 1
)

echo Step 1: Building Maven projects...
echo.

REM Build eureka-server
echo [1/5] Building eureka-server...
cd eureka-server
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build eureka-server
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

REM Build config-server
echo [2/5] Building config-server...
cd config-server
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build config-server
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

REM Build flight-service
echo [3/5] Building flight-service...
cd flight-service
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build flight-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

REM Build booking-service
echo [4/5] Building booking-service...
cd booking-service
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build booking-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

REM Build api-gateway
echo [5/5] Building api-gateway...
cd api-gateway
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build api-gateway
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

echo Step 2: Verifying JAR files exist...
echo.

REM Verify JAR files exist before building Docker images
if not exist "eureka-server\target\eureka-server-0.0.1-SNAPSHOT.jar" (
    echo ERROR: eureka-server JAR file not found!
    pause
    exit /b 1
)
if not exist "config-server\target\config-server-0.0.1-SNAPSHOT.jar" (
    echo ERROR: config-server JAR file not found!
    pause
    exit /b 1
)
if not exist "flight-service\target\flight-service-0.0.1-SNAPSHOT.jar" (
    echo ERROR: flight-service JAR file not found!
    pause
    exit /b 1
)
if not exist "booking-service\target\booking-service-0.0.1-SNAPSHOT.jar" (
    echo ERROR: booking-service JAR file not found!
    pause
    exit /b 1
)
if not exist "api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar" (
    echo ERROR: api-gateway JAR file not found!
    pause
    exit /b 1
)

echo All JAR files found. Proceeding with Docker build...
echo.

REM Build Docker images using docker-compose
echo Building all Docker images...
docker-compose build

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to build Docker images
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Set environment variables (RESEND_API_KEY, RESEND_FROM_EMAIL, FRONTEND_URL)
echo 2. Run: docker-compose up -d
echo 3. Check logs: docker-compose logs -f
echo.
pause

