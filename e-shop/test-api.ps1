# Quick test: register, get token, call /api/users/me (no copy-paste of token)
$base = "http://localhost:9090"

# 1. Register (or login if already registered)
$body = '{"email":"test@example.com","password":"password123"}'
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $body -ErrorAction SilentlyContinue
if (-not $login) {
    $login = Invoke-RestMethod -Uri "$base/api/auth/register" -Method POST -ContentType "application/json" -Body $body
}
$token = $login.accessToken
Write-Host "Token received (length $($token.Length))"

# 2. Call protected endpoint with the full token
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "$base/api/users/me" -Method GET -Headers $headers
