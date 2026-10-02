param(
    [string]$Repo = "idnyakrytoi-afk/bulauncher",
    [string]$Tag = "v0.1.1",
    [string]$Name = "BullMC Client v0.1.1",
    [string]$Body = "BullMC Client release build.",
    [switch]$Prerelease
)

$ErrorActionPreference = "Stop"

$token = $env:GITHUB_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Set GITHUB_TOKEN before running this script. Never commit a GitHub token into the repo."
}

$releaseDir = Join-Path $PSScriptRoot "build\release"
if (-not (Test-Path $releaseDir)) {
    throw "Release folder not found: $releaseDir. Run .\build_launcher.ps1 first."
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept" = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}

$bodyJson = @{
    tag_name = $Tag
    name = $Name
    body = $Body
    draft = $false
    prerelease = [bool]$Prerelease
} | ConvertTo-Json

$release = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/$Repo/releases" `
    -Method Post `
    -Headers $headers `
    -Body $bodyJson `
    -ContentType "application/json"

Write-Host "Release created: $($release.html_url)"

$assets = Get-ChildItem -LiteralPath $releaseDir -File -Include *.jar,*.exe,*.msi,*.zip
foreach ($asset in $assets) {
    $assetName = [uri]::EscapeDataString($asset.Name)
    $uploadUrl = "https://uploads.github.com/repos/$Repo/releases/$($release.id)/assets?name=$assetName"
    Write-Host "Uploading $($asset.Name)..."
    Invoke-RestMethod `
        -Uri $uploadUrl `
        -Method Post `
        -Headers $headers `
        -ContentType "application/octet-stream" `
        -InFile $asset.FullName | Out-Null
}

Write-Host "Done."
