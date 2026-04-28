# start-local.ps1 - 로컬 개발 환경 시작 스크립트 (Docker 없이)
# 사전 요구사항: Java 21, Node.js, Python 3.10+, Redis (아래 참고)

$ProjectRoot = $PSScriptRoot

Write-Host ""
Write-Host "=== Restaurant MSA 로컬 개발 환경 ===" -ForegroundColor Cyan
Write-Host ""

# .env 파일에서 환경변수 로드
$envFile = "$ProjectRoot\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match "^[A-Z]" } | ForEach-Object {
        $parts = $_ -split "=", 2
        if ($parts.Length -eq 2) {
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
            Write-Host "  env: $key 로드됨" -ForegroundColor DarkGray
        }
    }
} else {
    Write-Host "  .env 파일 없음, 기본값 사용" -ForegroundColor Yellow
}

# SPRING_PROFILES_ACTIVE 설정 (Start-Process로 생성되는 자식 프로세스에 상속됨)
$env:SPRING_PROFILES_ACTIVE = "local"
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "supersecretkeymustbe32charslong!!"
}

Write-Host ""

# ──────────────────────────────────────────────────────
# 1. Redis 확인 / 시작
# ──────────────────────────────────────────────────────
Write-Host "[1] Redis 확인 중..." -ForegroundColor Yellow

$redisPort = 6379
$redisTcpTest = (Test-NetConnection -ComputerName localhost -Port $redisPort -WarningAction SilentlyContinue).TcpTestSucceeded

if ($redisTcpTest) {
    Write-Host "    Redis 이미 실행 중 (port $redisPort)" -ForegroundColor Green
} else {
    # Docker가 있으면 Redis만 컨테이너로 띄움
    $dockerAvailable = $null
    try { $dockerAvailable = docker version --format "{{.Server.Version}}" 2>$null } catch {}

    if ($dockerAvailable) {
        Write-Host "    Docker로 Redis 시작 중..." -ForegroundColor Yellow
        $existing = docker ps -a --filter "name=restaurant-redis" -q 2>$null
        if ($existing) {
            docker start restaurant-redis | Out-Null
        } else {
            docker run -d --name restaurant-redis -p 6379:6379 redis:7-alpine | Out-Null
        }
        Start-Sleep -Seconds 2
        Write-Host "    Redis 시작됨 (port $redisPort)" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "  [!] Redis가 실행되지 않고 있습니다." -ForegroundColor Red
        Write-Host "  해결 방법 중 하나를 선택하세요:" -ForegroundColor Yellow
        Write-Host "    A) Docker Desktop 설치 후 재실행" -ForegroundColor White
        Write-Host "    B) Memurai (Windows용 Redis) 설치: https://www.memurai.com/" -ForegroundColor White
        Write-Host "    C) WSL2에서 sudo apt install redis-server && redis-server" -ForegroundColor White
        Write-Host ""
        Read-Host "Redis를 수동으로 시작 후 Enter를 누르세요"
    }
}

Write-Host ""

# ──────────────────────────────────────────────────────
# 2. SQLite 데이터 디렉토리 생성
# ──────────────────────────────────────────────────────
foreach ($svc in @("auth-service","menu-service","order-service","review-service")) {
    New-Item -ItemType Directory -Force -Path "$ProjectRoot\$svc\data" | Out-Null
}

# ──────────────────────────────────────────────────────
# 3. Spring Boot 서비스 시작 (별도 창)
# ──────────────────────────────────────────────────────
Write-Host "[2] Spring Boot 서비스 시작 중..." -ForegroundColor Yellow

$springServices = @(
    @{ name = "API Gateway";     module = "api-gateway";     port = 8080 },
    @{ name = "Auth Service";    module = "auth-service";    port = 8081 },
    @{ name = "Menu Service";    module = "menu-service";    port = 8082 },
    @{ name = "Order Service";   module = "order-service";   port = 8083 },
    @{ name = "Review Service";  module = "review-service";  port = 8084 }
)

foreach ($svc in $springServices) {
    $title = "$($svc.name) :$($svc.port)"
    $cmd = "cd '$ProjectRoot'; `$host.UI.RawUI.WindowTitle = '$title'; Write-Host '=== $($svc.name) (port $($svc.port)) ===' -ForegroundColor Cyan; .\gradlew.bat :$($svc.module):bootRun; Read-Host 'Press Enter to close'"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
    Write-Host "    $($svc.name) 시작됨 (port $($svc.port))" -ForegroundColor Green
    Start-Sleep -Milliseconds 500
}

# ──────────────────────────────────────────────────────
# 4. AI 서비스 시작 (별도 창)
# ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "[3] AI 서비스 시작 중..." -ForegroundColor Yellow

$anthropicKey = $env:ANTHROPIC_API_KEY
$aiCmd = "cd '$ProjectRoot\ai-service'; `$host.UI.RawUI.WindowTitle = 'AI Service :8085'; `$env:ORDER_SERVICE_URL='http://localhost:8083'; `$env:REVIEW_SERVICE_URL='http://localhost:8084'; `$env:ANTHROPIC_API_KEY='$anthropicKey'; Write-Host '=== AI Service (port 8085) ===' -ForegroundColor Cyan; pip install -r requirements.txt -q; uvicorn main:app --host 0.0.0.0 --port 8085 --reload; Read-Host 'Press Enter to close'"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $aiCmd
Write-Host "    AI Service 시작됨 (port 8085)" -ForegroundColor Green

# ──────────────────────────────────────────────────────
# 5. 프론트엔드 시작 (별도 창)
# ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "[4] 프론트엔드 시작 중..." -ForegroundColor Yellow

$frontends = @(
    @{ name = "Frontend Customer"; dir = "frontend-customer"; port = 5173 },
    @{ name = "Frontend Admin";    dir = "frontend-admin";    port = 5174 }
)

foreach ($fe in $frontends) {
    $feDir = "$ProjectRoot\$($fe.dir)"
    $feCmd = "cd '$feDir'; `$host.UI.RawUI.WindowTitle = '$($fe.name) :$($fe.port)'; Write-Host '=== $($fe.name) (port $($fe.port)) ===' -ForegroundColor Cyan; if (-not (Test-Path node_modules)) { Write-Host 'npm install 중...' -ForegroundColor Yellow; npm install }; npm run dev; Read-Host 'Press Enter to close'"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $feCmd
    Write-Host "    $($fe.name) 시작됨 (port $($fe.port))" -ForegroundColor Green
}

# ──────────────────────────────────────────────────────
# 안내 출력
# ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " 서비스 접속 주소" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  고객 프론트엔드  : http://localhost:5173" -ForegroundColor White
Write-Host "  관리자 프론트엔드: http://localhost:5174" -ForegroundColor White
Write-Host "  API Gateway      : http://localhost:8080" -ForegroundColor White
Write-Host "  Auth Service     : http://localhost:8081" -ForegroundColor White
Write-Host "  Menu Service     : http://localhost:8082" -ForegroundColor White
Write-Host "  Order Service    : http://localhost:8083" -ForegroundColor White
Write-Host "  Review Service   : http://localhost:8084" -ForegroundColor White
Write-Host "  AI Service       : http://localhost:8085" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Spring Boot 서비스는 시작까지 30~60초 소요됩니다." -ForegroundColor Yellow
Write-Host "  API 확인: http://localhost:8080/auth/health 등" -ForegroundColor DarkGray
Write-Host ""
