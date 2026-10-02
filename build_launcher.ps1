# ==============================================================================
# BULauncher Build Script
# Собирает Minecraft лаунчер в .exe файл с Microsoft аутентификацией
# ==============================================================================

$ErrorActionPreference = "Stop"
$ProjectPath = "."

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  BULauncher Build Script" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Функция для вывода сообщений
function Write-Status {
    param([string]$Message, [string]$Color = "White")
    Write-Host "> $Message" -ForegroundColor $Color
}

# Шаг 1: Проверка Java
Write-Status "Проверка Java..."

$javaPath = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $javaPath) {
    Write-Status "Java НЕ найден!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Варианты решения:" -ForegroundColor Yellow
    Write-Host "1. Скачать и установить JDK 17 или 21: https://adoptium.net/" -ForegroundColor White
    Write-Host "2. Или использовать OpenJDK от JetBrains (автоматически)" -ForegroundColor White
    Write-Host ""
    
    $download = Read-Host "Скачать OpenJDK автоматически? (y/n)"
    if ($download -eq "y") {
        # Скачиваем OpenJDK 17 от JetBrains
        $jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B10/OpenJDK17U-jdk_x64_windows_hotspot_17.0.13_10.zip"
        $jdkZip = Join-Path $env:TEMP "openjdk17.zip"
        
        Write-Status "Скачивание OpenJDK 17..."
        Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip
        
        $jdkExtract = Join-Path $env:TEMP "jdk17"
        Expand-Archive -Path $jdkZip -DestinationPath $env:TEMP -Force
        
        # Находим папку с JDK
        $jdkFolder = Get-ChildItem -Path $env:TEMP -Directory | Where-Object { $_.Name -match "^jdk-17" } | Select-Object -First 1
        if ($null -eq $jdkFolder) {
            Write-Status "Не удалось найти JDK!" -ForegroundColor Red
            exit 1
        }
        
        $env:JAVA_HOME = $jdkFolder.FullName
        $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
        Write-Status "Java установлен: $($jdkFolder.FullName)" -ForegroundColor Green
    } else {
        Write-Status "Установите Java и запустите скрипт снова" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Status "Java найден: $($javaPath.Source)" -ForegroundColor Green
    
    # Пытаемся найти JAVA_HOME
    if (-not $env:JAVA_HOME) {
        $javaExe = $javaPath.Source
        $possibleJdkPaths = @(
            (Get-Item $javaExe).Parent.Parent.FullName,
            (Get-Item $javaExe).Parent.FullName
        )
        foreach ($path in $possibleJdkPaths) {
            if (Test-Path "$path\bin\javac.exe") {
                $env:JAVA_HOME = $path
                break
            }
        }
    }
}

# Проверка версии Java
try {
    $javaVersion = & java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Status "Версия: $javaVersion" -ForegroundColor Green
} catch {
    Write-Status "Не удалось определить версию Java" -ForegroundColor Yellow
}

# Шаг 2: Проверка Gradle wrapper
Write-Status ""
Write-Status "Проверка Gradle..."

if (-not (Test-Path "gradlew.bat")) {
    Write-Status "Gradle wrapper НЕ найден!" -ForegroundColor Red
    exit 1
}

# Шаг 3: Очистка предыдущей сборки
Write-Status ""
Write-Status "Очистка предыдущей сборки..." -ForegroundColor Cyan
& .\gradlew.bat clean --console=plain 2>&1 | Out-Null

# Шаг 4: Сборка jar, app-image, .exe и .msi
# createDistributable = портативная папка с BullMCClient.exe
# packageExe/packageMsi = установщики Windows
Write-Status ""
Write-Status "СБОРКА BullMCClient: jar, portable app, exe, msi..." -ForegroundColor Yellow
Write-Host ""

try {
    $buildOutput = & .\gradlew.bat clean test jar createDistributable packageExe packageMsi --console=plain 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Status ""
        Write-Status "==========================================" -ForegroundColor Green
        Write-Status "СБОРКА УСПЕШНА!" -ForegroundColor Green
        Write-Status "==========================================" -ForegroundColor Green
        Write-Host ""
        
        $releaseDir = Join-Path $ProjectPath "build\release"
        if (Test-Path $releaseDir) {
            Remove-Item -LiteralPath $releaseDir -Recurse -Force
        }
        New-Item -ItemType Directory -Path $releaseDir | Out-Null

        # Поиск jar
        $jarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" -File -ErrorAction SilentlyContinue
        foreach ($jar in $jarFiles) {
            Copy-Item -LiteralPath $jar.FullName -Destination $releaseDir -Force
        }

        # Поиск портативного .exe
        $appDir = Join-Path $ProjectPath "build\compose\binaries\main\app\BullMCClient"
        $exeFile = Join-Path $appDir "BullMCClient.exe"
        if (Test-Path $exeFile) {
            $sizeMb = [Math]::Round((Get-Item $exeFile).Length / 1MB, 2)
            Write-Status "Готовый файл:" -ForegroundColor Cyan
            Write-Host "  $((Resolve-Path $exeFile).Path)"
            Write-Host "  Размер: $sizeMb MB"
            Write-Host ""
            Write-Status "Папка приложения: $((Resolve-Path $appDir).Path)" -ForegroundColor Cyan

            $portableZip = Join-Path $releaseDir "BullMCClient-portable-$((Get-Date).ToString('yyyyMMdd-HHmm')).zip"
            Compress-Archive -Path "$appDir\*" -DestinationPath $portableZip -Force
        } else {
            # Ищем в других местах
            $altPaths = @(
                "build\compose\binaries\main\app",
                "build\compose\binaries\main\debug",
                "build\compose\binaries\main\release",
                "build\compose" 
            )
            foreach ($path in $altPaths) {
                if (Test-Path $path) {
                    $altExes = Get-ChildItem -Path $path -Filter "*.exe" -Recurse
                    if ($altExes) {
                        Write-Status "Найден .exe:" -ForegroundColor Cyan
                        foreach ($exe in $altExes) {
                            Write-Host "  $($exe.FullName)"
                        }
                        break
                    }
                }
            }
        }

        # Копируем установщики .exe/.msi из Compose. Portable .exe отдельно не копируем:
        # он работает только внутри zip-папки с runtime.
        $installers = @()
        $installerDirs = @(
            "build\compose\binaries\main\exe",
            "build\compose\binaries\main\msi"
        )
        foreach ($installerDir in $installerDirs) {
            if (Test-Path $installerDir) {
                $installers += Get-ChildItem -Path $installerDir -File -Recurse -Include "*.exe","*.msi" -ErrorAction SilentlyContinue
            }
        }
        foreach ($installer in $installers) {
            Copy-Item -LiteralPath $installer.FullName -Destination $releaseDir -Force
        }

        $nsis = @(
            "C:\Program Files (x86)\NSIS\makensis.exe",
            "C:\Program Files\NSIS\makensis.exe"
        ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

        if ($nsis) {
            Write-Status "Сборка кастомного BullCraft installer..." -ForegroundColor Yellow
            Push-Location "installer"
            try {
                & $nsis ".\BullCraftInstaller.nsi" | Out-Null
            } finally {
                Pop-Location
            }
        } else {
            Write-Status "NSIS не найден: BullCraft-Installer не собран. Установите NSIS.NSIS через winget." -ForegroundColor Yellow
        }
        
        Write-Host ""
        Write-Status "Готовые артефакты релиза:" -ForegroundColor Cyan
        Get-ChildItem -LiteralPath $releaseDir -File | ForEach-Object {
            $sizeMb = [Math]::Round($_.Length / 1MB, 2)
            Write-Host "  $($_.FullName) ($sizeMb MB)"
        }
        Write-Host ""
        Write-Status "Папку build\release можно прикреплять к GitHub Releases." -ForegroundColor Green
    } else {
        Write-Status ""
        Write-Status "==========================================" -ForegroundColor Red
        Write-Status "ОШИБКА СБОРКИ!" -ForegroundColor Red
        Write-Status "==========================================" -ForegroundColor Red
        Write-Host $buildOutput
        exit 1
    }
} catch {
    Write-Status "Ошибка: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Status "Сборка завершена!" -ForegroundColor Cyan
