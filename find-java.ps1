# Finds a Java that can build the mod (21..24) and prints its folder.
# Minecraft 1.21.1 needs Gradle itself to run on Java 21+; Gradle 8.14 can't run on Java 25+.
# If there is none, downloads a portable Java 21 into .\jdk (only once, ~200 MB).
$ProgressPreference = 'SilentlyContinue'
$local = Join-Path $PSScriptRoot 'jdk'
$candidates = @($local)
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$roots = @("$env:USERPROFILE\.jdks", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium",
           "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu", "$env:ProgramFiles\BellSoft", "$env:ProgramFiles\Amazon Corretto")
$candidates += Get-ChildItem $roots -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
$cmd = Get-Command java -ErrorAction SilentlyContinue
if ($cmd) { $candidates += (Split-Path (Split-Path $cmd.Source)) }

foreach ($c in $candidates) {
    $release = Join-Path $c 'release'
    if (-not (Test-Path $release) -or -not (Test-Path (Join-Path $c 'bin\java.exe'))) { continue }
    $m = Select-String -Path $release -Pattern '^JAVA_VERSION="(\d+)' | Select-Object -First 1
    if (-not $m) { continue }
    $v = [int]$m.Matches[0].Groups[1].Value
    if ($v -ge 21 -and $v -le 24) { Write-Output $c; exit 0 }
}

Write-Host "No suitable Java found - downloading Java 21 (about 200 MB, only the first time)..."
$zip = Join-Path $env:TEMP 'celltostars-jdk21.zip'
Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $zip -UseBasicParsing
$tmp = Join-Path $PSScriptRoot 'jdk-download'
if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
Expand-Archive $zip $tmp -Force
$inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
Move-Item $inner.FullName $local
Remove-Item $tmp -Recurse -Force
Remove-Item $zip
Write-Output $local
