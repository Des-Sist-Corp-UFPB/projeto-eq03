$ErrorActionPreference = "Stop"
$loginBody = @{
    email = "admin@salao.com"
    password = "Admin@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $response.token

$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "--- TEST USER CRUD ---"
Write-Host "1. GET /v1/users"
try {
    $users = Invoke-RestMethod -Uri "http://localhost:8080/v1/users" -Method Get -Headers $headers
    Write-Host "SUCCESS. Found $($users.Count) users."
} catch {
    Write-Host "FAILED. $_"
}

Write-Host "2. POST /v1/users"
$newUser = @{
    name = "Test User"
    email = "test.user@teste.com"
    phone = "11999999999"
    password = "Test@User1"
    roleId = 4
} | ConvertTo-Json
try {
    $createdUser = Invoke-RestMethod -Uri "http://localhost:8080/v1/users" -Method Post -Body $newUser -ContentType "application/json" -Headers $headers
    Write-Host "SUCCESS. Created user with ID $($createdUser.id)"
} catch {
    Write-Host "FAILED. $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.ReadToEnd() | Write-Host
    }
}
