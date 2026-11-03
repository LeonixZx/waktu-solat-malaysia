param([string]$Source = 'https://www.e-solat.gov.my/')
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$destination = Join-Path $projectRoot 'app\src\main\assets\prayer_zones.json'
$stateNames = @{
    JHR='Johor'; KDH='Kedah'; KTN='Kelantan'; MLK='Melaka'; NGS='Negeri Sembilan';
    PHG='Pahang'; PLS='Perlis'; PNG='Pulau Pinang'; PRK='Perak'; SBH='Sabah';
    SGR='Selangor'; SWK='Sarawak'; TRG='Terengganu'; WLY='Wilayah Persekutuan'
}
$page = (Invoke-WebRequest -Uri $Source -UseBasicParsing -TimeoutSec 30).Content
$options = [regex]::Matches($page, '<option\s+value=[''"](?<code>[A-Z]{3}[0-9]{2})[''"][^>]*>(?<label>[^<]+)</option>')
$records = @($options | ForEach-Object {
    $code = $_.Groups['code'].Value
    $label = [Net.WebUtility]::HtmlDecode($_.Groups['label'].Value)
    $districts = ($label -replace ('^' + $code + '\s*-\s*'), '').Trim()
    if (-not $stateNames.ContainsKey($code.Substring(0,3))) { throw "Unknown zone prefix: $code" }
    [ordered]@{ code=$code; state=$stateNames[$code.Substring(0,3)]; districts=$districts }
} | Sort-Object { $_.code } -Unique)
if ($records.Count -lt 50) { throw 'Official zone list is incomplete; existing data was not replaced.' }
$catalog = [ordered]@{
    source=$Source
    retrievedUtc=[DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
    description='Geographic prayer-zone facts retrieved directly from JAKIM; not from an APK.'
    zones=$records
}
[IO.File]::WriteAllText($destination, ($catalog | ConvertTo-Json -Depth 6), [Text.UTF8Encoding]::new($false))
Write-Output "Saved $($records.Count) official zones to $destination"
