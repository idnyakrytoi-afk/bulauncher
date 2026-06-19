# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC.

## Возможности

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

## Структура

```
src/main/kotlin/net/bullmc/client/
├── Main.kt
├── theme/Theme.kt
├── ui/
│   ├── component/   — Sidebar, TopBanner, FriendsPanel, BottomCards, SkinPreview
│   └── screen/      — LoaderScreen, SettingsScreen, LogScreen, ModSettingsScreen,
│                       AuthScreen, ServerBrowserScreen
├── core/
│   ├── auth/        — Auth, MicrosoftAuth
│   ├── launcher/    — Launcher, MinecraftDownloader, MinecraftLauncher
│   ├── loader/      — LoaderManager, LoaderTypes
│   ├── mod/         — ModrinthApi, ModDownloader, ModSettings, ModpackImporter, ModUpdateChecker
│   ├── profile/     — Profile, InstanceManager
│   └── util/        — LauncherPaths, JavaDownloader, SkinFetcher, ServerPing,
│                       CrashAnalyzer, DiscordRPC, CacheManager, JreDetector
├── api/ServerApi.kt
└── db/DatabaseManager.kt
```

## Запуск

```bash
./gradlew run
```

## Сборка

```bash
./gradlew packageDistributionForCurrentOS
```
