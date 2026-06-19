# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC, написанный на Kotlin с использованием Jetbrains Compose Desktop.

## Структура проекта

```
src/main/kotlin/net/bullmc/client/
├── Main.kt                          # Точка входа
├── theme/
│   └── Theme.kt                     # Темы оформления (7 штук)
├── ui/
│   ├── component/                   # Переиспользуемые компоненты
│   │   ├── Sidebar.kt
│   │   ├── TopBanner.kt
│   │   ├── FriendsPanel.kt
│   │   └── BottomCards.kt
│   └── screen/                      # Экраны
│       ├── LoaderScreen.kt
│       ├── SettingsScreen.kt
│       ├── LogScreen.kt
│       └── ModSettingsScreen.kt
├── core/
│   ├── launcher/                    # Запуск и скачивание Minecraft
│   │   ├── Launcher.kt
│   │   ├── MinecraftDownloader.kt
│   │   └── MinecraftLauncher.kt
│   ├── loader/                      # Mod-лоадеры (Fabric, Forge, NeoForge, Quilt)
│   │   ├── LoaderManager.kt
│   │   └── LoaderTypes.kt
│   ├── mod/                         # Моды (Modrinth API)
│   │   ├── ModrinthApi.kt
│   │   ├── ModDownloader.kt
│   │   └── ModSettings.kt
│   ├── auth/
│   │   └── Auth.kt
│   ├── profile/
│   │   └── Profile.kt               # Система профилей
│   └── util/
│       ├── LauncherPaths.kt
│       ├── CacheManager.kt
│       └── JreDetector.kt
├── api/
│   └── ServerApi.kt
└── db/
    └── DatabaseManager.kt
```

## Установка и запуск

### Требования
- JDK 17 или выше

### Запуск разработки
```bash
./gradlew run
```

### Сборка
```bash
./gradlew packageDistributionForCurrentOS
```
