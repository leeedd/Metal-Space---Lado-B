# Baixa o FFmpeg (versao LGPL, Windows 64 bits) usado pela radio do jogo e coloca em ffmpeg\ffmpeg.exe.
# O ffmpeg.exe tem mais de 100 MB e por isso nao vai para o GitHub: rode este script uma vez em cada computador.
# Fonte: builds oficiais indicados em ffmpeg.org (github.com/BtbN/FFmpeg-Builds). Confere o SHA-256 antes de usar.
$ErrorActionPreference = 'Stop'
$base    = 'https://github.com/BtbN/FFmpeg-Builds/releases/download/latest'
$arquivo = 'ffmpeg-n9.0-latest-win64-lgpl-9.0.zip'
$pasta   = Join-Path $PSScriptRoot 'ffmpeg'
$tmp     = Join-Path ([System.IO.Path]::GetTempPath()) ('metalspace_ffmpeg_' + [guid]::NewGuid())
New-Item -ItemType Directory -Force -Path $tmp, $pasta | Out-Null
try {
    $zip = Join-Path $tmp $arquivo
    Write-Host "Baixando $arquivo (cerca de 170 MB)..."
    Invoke-WebRequest -Uri "$base/$arquivo" -OutFile $zip -UseBasicParsing
    $somas = (Invoke-WebRequest -Uri "$base/checksums.sha256" -UseBasicParsing).Content
    $linha = ($somas -split "`n") | Where-Object { $_ -match [regex]::Escape($arquivo) } | Select-Object -First 1
    if (-not $linha) { throw "Soma SHA-256 de $arquivo nao encontrada na release" }
    $esperado = ($linha -split '\s+')[0].ToLower()
    $obtido = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLower()
    if ($esperado -ne $obtido) { throw "SHA-256 nao confere: esperado $esperado, obtido $obtido" }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $exe = Get-ChildItem -Path $tmp -Recurse -Filter 'ffmpeg.exe' | Select-Object -First 1
    $lic = Get-ChildItem -Path $tmp -Recurse -Filter 'LICENSE.txt' | Select-Object -First 1
    Copy-Item $exe.FullName (Join-Path $pasta 'ffmpeg.exe') -Force
    if ($lic) { Copy-Item $lic.FullName (Join-Path $pasta 'LICENCA_FFMPEG_LGPL3.txt') -Force }
    Write-Host "Pronto: $(Join-Path $pasta 'ffmpeg.exe')"
} finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}
