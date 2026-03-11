# Run from project root. Starts backend (8 services) + frontend (1 window).
$root = $PSScriptRoot
if (-not $root) { $root = Get-Location | Select-Object -ExpandProperty Path }

function Import-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line)) { return }
        if ($line.StartsWith("#")) { return }
        $parts = $line.Split("=", 2)
        if ($parts.Count -ne 2) { return }
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim("'").Trim('"')
        if (-not [string]::IsNullOrWhiteSpace($name)) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

# Optional local MySQL credentials for mysql-profile services.
Import-EnvFile "$root\tools\mysql.env.local"
# Optional local auth SMTP/TOTP settings.
Import-EnvFile "$root\tools\auth-service.env.local"

$services = @(
    @{ Name = "Eureka"; Path = "eureka-server"; Profile = $false },
    @{ Name = "Gateway"; Path = "api-gateway"; Profile = $false },
    @{ Name = "Auth"; Path = "auth-service"; Profile = $true },
    @{ Name = "User"; Path = "user-service"; Profile = $true },
    @{ Name = "Product"; Path = "product-service"; Profile = $true },
    @{ Name = "Order"; Path = "order-service"; Profile = $true },
    @{ Name = "Inventory"; Path = "inventory-service"; Profile = $true },
    @{ Name = "Payment"; Path = "payment-service"; Profile = $true },
    @{ Name = "Cart"; Path = "cart-service"; Profile = $true }
)

foreach ($s in $services) {
    $cmd = "Set-Location '$root\$($s.Path)'; "
    if ($s.Profile) {
        # Env var avoids PowerShell eating -D and Maven "Unknown lifecycle phase"
        $cmd += "`$env:SPRING_PROFILES_ACTIVE='mysql'; mvn spring-boot:run"
    } else {
        $cmd += "mvn spring-boot:run"
    }
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
    Start-Sleep -Seconds 2
}

# Frontend: install deps if needed, then start dev server (proxies /api to gateway)
$frontendCmd = "Set-Location '$root\frontend'; if (-not (Test-Path 'node_modules')) { Write-Host 'Installing frontend dependencies...'; npm install }; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd
Start-Sleep -Seconds 2

Write-Host "Started $($services.Count) backend + 1 frontend window."
Write-Host "Wait 1-2 min for backends. Then open: Eureka http://localhost:8761  Gateway http://localhost:9090  Frontend http://localhost:3000"
Write-Host "If mysql-profile services fail with Access denied, set tools\mysql.env.local from tools\mysql.env.example."
Write-Host "For OTP email/TOTP dev config, set tools\auth-service.env.local from tools\auth-service.env.example."
