# Genere le certificat TLS auto-signe utilise par le proxy Nginx local.
param(
  [string[]]$DnsName = @(),
  [string[]]$IpAddress = @('127.0.0.1')
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$sslDirectory = Join-Path $projectRoot 'nginx\ssl'
New-Item -ItemType Directory -Force -Path $sslDirectory | Out-Null
if (-not $DnsName -or $DnsName.Count -eq 0) {
  $envFile = Join-Path $projectRoot '.env'
  if (Test-Path $envFile) {
    $configuredName = Get-Content $envFile |
      Where-Object { $_ -match '^\s*FINTRACK_DNS_NAME\s*=' } |
      Select-Object -First 1
    if ($configuredName -match '^\s*FINTRACK_DNS_NAME\s*=\s*(.+?)\s*$') {
      $DnsName = @($Matches[1])
    }
  }
  if ((-not $DnsName -or $DnsName.Count -eq 0) -and $env:FINTRACK_DNS_NAME) {
    $DnsName = @($env:FINTRACK_DNS_NAME)
  }
}
$dnsNames = @('localhost') + ($DnsName | Where-Object { $_ -and $_.Trim() } | ForEach-Object { $_.Trim() })
$dnsNames = $dnsNames | Select-Object -Unique
$subjectCommonName = $dnsNames | Where-Object { $_ -ne 'localhost' } | Select-Object -First 1
if (-not $subjectCommonName) {
  $subjectCommonName = 'localhost'
}
$subjectAltNames = ($dnsNames | ForEach-Object { "DNS:$_" }) + ($IpAddress | Where-Object { $_ -and $_.Trim() } | ForEach-Object { "IP:$($_.Trim())" })
$subjectAltName = $subjectAltNames -join ','

docker run --rm `
  -v "${sslDirectory}:/ssl" `
  alpine:3.20 `
  sh -c "apk add --no-cache openssl >/dev/null && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /ssl/fintrack.key -out /ssl/fintrack.crt -subj '/CN=$subjectCommonName' -addext 'subjectAltName=$subjectAltName'"

if ($LASTEXITCODE -ne 0) {
  throw 'La generation du certificat TLS local a echoue.'
}

Write-Host "Certificat TLS local genere dans $sslDirectory"
Write-Host "SAN: $subjectAltName"
