# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC с AI-системой античита.

## Возможности

### Лаунчер
- **Microsoft Auth** — OAuth2 авторизация для серверов с проверкой
- **Профили** — отдельные настройки лоадера/модов для каждого профиля
- **Multi-instance** — несколько папок игры с разными модами
- **Mod-лоадеры** — Fabric, Forge, NeoForge, Quilt с автоустановкой
- **Fabric API** — скачивается автоматически
- **Моды** — каталог из Modrinth, автообновление
- **Импорт модпаков** — поддержка MRPACK
- **Skin preview** — показывает скин по нику
- **Crash reporter** — парсинг логов, диагностика крашей
- **Server browser** — список серверов с пингом
- **Discord Rich Presence** — статус в Discord
- **7 тем** — GitHub Dark, Amethyst, Ice, Emerald, Rose, Ocean, Lavender
- **Auto-download Java** — скачивает JDK 21 если не найдена

### AI Античит (Internal Cheat Detection)
- **Scoring Engine** — весовая модель для обнаружения интернал читов без ML-зависимостей
- **Pre-launch сканирование** — проверка модов, процессов, native-библиотек перед запуском
- **Runtime мониторинг** — периодическое сканирование во время игры (каждые 30 сек)
- **Java Agent** — перехват загрузки классов на уровне bytecode с AI-скорингом
- **UI интеграция** — индикатор угрозы в TopBanner, карточка AntiCheat в BottomCards

#### Детекты AI-движка
| Детект | Описание |
|--------|----------|
| `INJECTED_CHEAT_DLL` | Инжектированные чит-DLL в процессе Minecraft |
| `SUSPICIOUS_MODULE` | Модули с подозрительными секциями (.vmp, .upx, .themida) |
| `CHEAT_DLL_IN_DIR` | Чит-DLL в директории игры |
| `HOOK_LIBRARY` | Библиотеки хуков (minhook, detours, easyhook) |
| `INJECTION_TOOL` | Инструменты инъекции по API-сигнатурам |
| `CODE_CAVE` | NOP/INT3 sleds в bytecode |
| `PROCESS_MANIPULATION` | Манипуляция памятью процессов |
| `NETWORK_HOOK` | Сетевые хуки (Winsock перехват) |
| `DEBUGGER_DETECTED` | Активные отладчики |
| `UNSIGNED_LIBRARY` | Библиотеки без валидной подписи |
| `REFLECTIVE_INJECTION` | Reflective injection паттерны |

#### Java Agent (GameAgent)
- **Blacklist классов** — блокировка загрузки известных чит-классов
- **Vanilla protection** — защита ванильных классов от модификации
- **AI bytecode scoring** — анализ:
  - Injection паттернов (defineClass, Class.forName)
  - Reflection паттернов (Method.invoke, sun.misc.Unsafe)
  - Memory manipulation (ByteBuffer, Unsafe.put*)
  - Anti-detection (anti-debug, process listing)
  - Network patterns (Socket, ObjectInputStream)
  - Obfuscation (короткие имена, crypto, byte patterns)
  - Timing patterns (System.nanoTime + Thread.sleep)
- **Runtime monitoring** — пере-scan загруженных классов каждые 60 сек

#### Анти-тампер
- SHA-256 верификация agent JAR перед загрузкой
- Автоматическое переизвлечение при обнаружении повреждения
- Блокировка запуска при невозможности восстановить агент

#### Обфускация
- AES-шифрование чувствительных строк (blacklist имена, паттерны)
- StringObfuscator для защиты от реверс-инжиниринга

## Структура

```
src/main/kotlin/net/bullmc/client/
├── Main.kt
├── Animation.kt
├── FileUtils.kt
├── api/ServerApi.kt
├── core/
│   ├── anticheat/
│   │   ├── AntiCheatManager.kt      # Оркестратор
│   │   ├── AntiCheatScanner.kt      # Сканер модов/процессов
│   │   ├── CheatDatabase.kt         # Блэклист/вайтлист
│   │   ├── GameAgent.kt             # Java Agent + AI scoring
│   │   ├── InternalCheatDetector.kt # AI scoring engine
│   │   ├── MemorySignatureScanner.kt# Сигнатуры хуков
│   │   ├── ModuleAnalyzer.kt        # PE анализ
│   │   ├── StringObfuscator.kt      # Обфускация строк
│   │   └── ViolationReporter.kt     # Отправка отчётов
│   ├── auth/        — Auth, MicrosoftAuth
│   ├── launcher/    — Launcher, MinecraftDownloader, MinecraftLauncher
│   ├── loader/      — LoaderManager, LoaderTypes
│   ├── mod/         — ModrinthApi, ModDownloader, ModSettings, ModpackImporter
│   ├── profile/     — Profile, InstanceManager
│   └── util/        — LauncherPaths, JavaDownloader, SkinFetcher, ServerPing,
│                       CrashAnalyzer, DiscordRPC, CacheManager, JreDetector
├── db/DatabaseManager.kt
├── theme/Theme.kt
└── ui/
    ├── component/   — Sidebar, TopBanner, FriendsPanel, BottomCards, SkinPreview
    └── screen/      — LoaderScreen, SettingsScreen, LogScreen, ModSettingsScreen,
                        AuthScreen, ServerBrowserScreen

src/test/kotlin/net/bullmc/client/core/anticheat/   # 81 тест
├── AntiCheatScannerTest.kt
├── CheatDatabaseTest.kt
├── InternalCheatDetectorTest.kt
├── MemorySignatureScannerTest.kt
└── ModuleAnalyzerTest.kt
```

## Тесты

```bash
./gradlew test
```

81 тест покрывает:
- CheatDatabase — blacklist/whitelist модов, native libs, tweak classes, JVM args
- MemorySignatureScanner — cheat DLL signatures, hook patterns, injection APIs
- ModuleAnalyzer — PE header, suspicious sections, entropy, file hashing
- InternalCheatDetector — threat levels, score thresholds, detection categories
- AntiCheatScanner — полный pipeline сканирования

## Запуск

```bash
./gradlew run
```

## Сборка

```bash
./gradlew packageDistributionForCurrentOS
```

## Сборка Agent JAR

```bash
./gradlew buildAgentJar
```

Собирает `bin/bullmc-anticheat-agent.jar` — Java Agent для перехвата загрузки классов.
