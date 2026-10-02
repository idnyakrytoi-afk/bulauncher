# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC.

[![BullCraft на Millida](https://millida.net/rating/servers/bullcraft/banner.svg)](https://millida.net/rating/servers/bullcraft)

## Возможности

- **Microsoft Auth** — OAuth2 авторизация для серверов с проверкой
- **Профили** — отдельные настройки лоадера/модов для каждого профиля
- **Multi-instance** — несколько папок игры с разными модами
- **Mod-лоадеры** — Fabric, Forge, NeoForge, Quilt с автоустановкой
- **Fabric API** — скачивается автоматически
- **Моды** — каталог из Modrinth, автообновление
- **Сборки сообщества** — каталог готовых сборок от игроков: установка в один клик, публикация своей сборки, экспорт/импорт `.bullbuild`, общий индекс через GitHub
- **Импорт модпаков** — поддержка MRPACK
- **Анимации** — splash с прогрессом, каскадное появление списков, hover/press эффекты, пульсирующие статусы, анимированный прогресс загрузки, shimmer-заглушки
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
├── Animation.kt        — переиспользуемые анимации (переходы, hover, пульс, shimmer)
├── FileUtils.kt
├── api/ServerApi.kt
├── core/
│   ├── auth/        — Auth, MicrosoftAuth
│   ├── builds/      — CommunityBuild, BuildsRepository, BuildInstaller
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
    └── screen/      — BuildsScreen, LoaderScreen, SettingsScreen, LogScreen,
                        ModSettingsScreen, AuthScreen, ServerBrowserScreen
```

## Сборки сообщества

Экран **Builds** в сайдбаре:

- **Каталог** — встроенные сборки + сборки из общего индекса
  (`https://raw.githubusercontent.com/idnyakrytoi-afk/bulauncher/main/builds.json`).
- **Установка** — создаёт профиль с нужной версией/лоадером и докачивает моды из Modrinth.
- **Публикация** — кнопка «+ Опубликовать» сохраняет текущий профиль как сборку
  (в `~/.bullmc-client/builds.json`).
- **Шеринг** — «Экспорт» создаёт файл `.bullbuild`; другой игрок импортирует его
  через `BuildsRepository.importFromFile` (или файл добавляется в общий индекс PR-ом).

## Запуск

```bash
./gradlew run
```

## Сборка .exe

```powershell
./build_launcher.ps1
```

или вручную:

```bash
./gradlew createDistributable
```

Готовый лаунчер: `build/compose/binaries/main/app/BullMCClient/BullMCClient.exe`
(распространяется всей папкой `BullMCClient` — внутри портативный runtime, Java не нужна).

Установщик `.exe`/`.msi` (требует WiX, скачивается автоматически):

```bash
./gradlew packageExe
```
