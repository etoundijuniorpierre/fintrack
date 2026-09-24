param(
  [Parameter(Mandatory = $true)][string]$DbUser,
  [string]$Container = 'fintrack-postgresql',
  [ValidateRange(1, 1000000)][int]$Limit = 100000,
  [string]$ObjectKeysPath
)
$ErrorActionPreference = 'Stop'

# Lecture seule : aucune correction ni suppression des donnees historiques.
function Read-Rows([string]$Database, [string]$Sql) {
  $result = docker exec -e 'PGOPTIONS=-c default_transaction_read_only=on' $Container psql -p 5434 -U $DbUser -d $Database -At -v ON_ERROR_STOP=1 -c $Sql
  if ($LASTEXITCODE -ne 0) { throw "Echec de lecture de $Database" }
  return @((($result -join "") | ConvertFrom-Json))
}

$attachments = Read-Rows 'documentservicedb' "SELECT coalesce(json_agg(t),'[]') FROM (SELECT id,incident_id,comment_id,category,file_size,storage_path FROM attachments_metadata ORDER BY id LIMIT $($Limit + 1)) t"
$incidents = Read-Rows 'incidentservicedb' "SELECT coalesce(json_agg(t),'[]') FROM (SELECT id FROM incidents ORDER BY id LIMIT $($Limit + 1)) t"
$comments = Read-Rows 'incidentservicedb' "SELECT coalesce(json_agg(t),'[]') FROM (SELECT id,incident_id FROM incident_comments ORDER BY id LIMIT $($Limit + 1)) t"
if ($attachments.Count -gt $Limit -or $incidents.Count -gt $Limit -or $comments.Count -gt $Limit) {
  throw 'Inventaire incomplet : augmentez explicitement -Limit. Aucun resultat partiel ne sera classe comme orphelin.'
}
$incidentIds = @{}
foreach ($row in $incidents) { $incidentIds[[string]$row.id] = $true }
$commentIncidents = @{}
foreach ($row in $comments) { $commentIncidents[[string]$row.id] = [string]$row.incident_id }
$keys = @{}
$issues = @(
  foreach ($row in $attachments) {
    $keys[[string]$row.storage_path] = $true
    $reasons = @()
    if ($row.file_size -le 0) { $reasons += 'empty_metadata' }
    if ($row.incident_id -and !$incidentIds.ContainsKey([string]$row.incident_id)) { $reasons += 'missing_incident' }
    if (!$row.incident_id -and $row.category -ne 'USER_AVATAR') { $reasons += 'missing_target' }
    if ($row.comment_id) {
      if (!$commentIncidents.ContainsKey([string]$row.comment_id)) { $reasons += 'missing_comment' }
      elseif ($commentIncidents[[string]$row.comment_id] -ne [string]$row.incident_id) { $reasons += 'wrong_comment_incident' }
    }
    if ($reasons.Count) {
      [pscustomobject]@{ attachment_id = $row.id; storage_key = $row.storage_path; reasons = $reasons }
    }
  }
)
$unreferencedKeys = @()
if ($ObjectKeysPath) {
  $unreferencedKeys = @(Get-Content -LiteralPath $ObjectKeysPath | Where-Object { $_ -and !$keys.ContainsKey($_) })
}
[pscustomobject]@{
  generated_at = [DateTime]::UtcNow.ToString('o')
  mode = 'read_only_candidates_requiring_confirmation'
  attachment_count = $attachments.Count
  issues = $issues
  object_inventory_supplied = [bool]$ObjectKeysPath
  unreferenced_object_keys = $unreferencedKeys
} | ConvertTo-Json -Depth 5
