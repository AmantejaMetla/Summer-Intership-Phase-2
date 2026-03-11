param(
    [string]$EnvFile = "$PSScriptRoot\auth-service.env.local",
    [switch]$UseMysqlProfile
)

$ErrorActionPreference = "Stop"

function Import-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Env file not found: $Path`nCopy tools/auth-service.env.example to tools/auth-service.env.local and fill values."
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

Import-EnvFile -Path $EnvFile

$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location "$projectRoot\auth-service"

if ($UseMysqlProfile) {
    $env:SPRING_PROFILES_ACTIVE = "mysql"
}

Write-Host "Starting auth-service with local env from: $EnvFile"
mvn spring-boot:run
