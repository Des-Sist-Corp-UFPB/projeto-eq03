$loginBody = @{ email = "admin@salao.com"; password = "Admin@123" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $response.token
$parts = $token.Split('.')
$payloadBytes = [System.Convert]::FromBase64String($parts[1].PadRight($parts[1].Length + (4 - $parts[1].Length % 4) % 4, '='))
[System.Text.Encoding]::UTF8.GetString($payloadBytes)