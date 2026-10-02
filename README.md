# 👋 Привет! Я idnyakrytoi-afk — делаю BullMC Client Launcher

## 🧑‍💻 Обо мне

- 🎮 Разрабатываю собственный Minecraft-лаунчер для BullMC.
- 🚀 Хочу сделать быстрый, красивый и удобный клиент с модами, сборками и нормальной диагностикой.
- 🎬 YouTube: [@idnyakrytoi-afk](https://www.youtube.com/@idnyakrytoi-afk)
- ⭐ Цель проекта — лаунчер, который не просто запускает игру, а помогает игроку чинить моды, краши и сборки.

## 💻 Технологический стек

**Языки и платформа:**

![Kotlin](https://img.shields.io/badge/KOTLIN-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/JAVA-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/GRADLE-02303A?style=for-the-badge&logo=gradle&logoColor=white)

**Frontend / Desktop UI:**

![Compose](https://img.shields.io/badge/COMPOSE_DESKTOP-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material](https://img.shields.io/badge/MATERIAL_UI-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

**Backend и интеграции:**

![Ktor](https://img.shields.io/badge/KTOR-087CFA?style=for-the-badge&logo=kotlin&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLITE-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Microsoft](https://img.shields.io/badge/MICROSOFT_AUTH-5E5E5E?style=for-the-badge&logo=microsoft&logoColor=white)
![Discord](https://img.shields.io/badge/DISCORD_RPC-5865F2?style=for-the-badge&logo=discord&logoColor=white)

**Инструменты:**

![Git](https://img.shields.io/badge/GIT-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GITHUB-181717?style=for-the-badge&logo=github&logoColor=white)
![Windows](https://img.shields.io/badge/WINDOWS-0078D4?style=for-the-badge&logo=windows&logoColor=white)

## 📁 Портфолио

- 🐂 **BullMC Client Launcher** — десктопный лаунчер для Minecraft-сервера BullMC.
- 📦 **Community Builds** — каталог готовых сборок от игроков с установкой в один клик.
- 🧩 **Mod Browser** — поиск и установка модов из Modrinth / CurseForge.
- 🛠️ **Crash Reporter** — анализ логов и подсказки по ошибкам запуска.
- 🎨 **Theme System** — набор тем, анимации, splash screen и кастомные UI-компоненты.

## 🔗 Проект

BullMC Client Launcher — это лаунчер, который объединяет авторизацию, профили, моды, сборки и запуск Minecraft в одном приложении.

**Возможности:**

- **Microsoft Auth** — OAuth2-авторизация для лицензионных аккаунтов.
- **Профили** — отдельные настройки версии, RAM, модов и папки игры.
- **Multi-instance** — разные игровые директории под разные сборки.
- **Mod-лоадеры** — Fabric, Forge, NeoForge, Quilt с автоустановкой.
- **Моды** — каталог Modrinth, CurseForge, автообновление и импорт MRPACK.
- **Сборки сообщества** — установка, экспорт и импорт `.bullbuild`.
- **Skin preview** — предпросмотр скина по нику.
- **Crash reporter** — диагностика крашей и подсказки по решению.
- **Server browser** — список серверов и ping.
- **Discord Rich Presence** — статус игры в Discord.
- **Темы и анимации** — GitHub Dark, Amethyst, Ice, Emerald, Rose, Ocean, Lavender.
- **Auto-download Java** — автоматическая загрузка JDK, если Java не найдена.

## 🧱 Структура

```text
src/main/kotlin/net/bullmc/client/
├── Main.kt
├── Animation.kt
├── api/ServerApi.kt
├── core/
│   ├── auth/        — Auth, MicrosoftAuth
│   ├── builds/      — CommunityBuild, BuildsRepository, BuildInstaller
│   ├── launcher/    — Launcher, MinecraftDownloader, MinecraftLauncher
│   ├── loader/      — LoaderManager, LoaderTypes
│   ├── mod/         — ModrinthApi, ModDownloader, ModpackImporter
│   ├── profile/     — Profile, InstanceManager
│   └── util/        — JavaDownloader, CrashAnalyzer, DiscordRPC, ServerPing
├── theme/Theme.kt
└── ui/
    ├── component/   — Sidebar, TopBanner, FriendsPanel, SkinPreview
    └── screen/      — BuildsScreen, LoaderScreen, SettingsScreen, LogScreen
```

## ⚙️ Запуск

```bash
./gradlew run
```

## 📦 Сборка релиза

```powershell
./build_launcher.ps1
```

После сборки готовые файлы лежат в:

```text
build/release/
├── bullmc-client-0.1.1.jar
├── BullMCClient-0.1.1.exe
├── BullMCClient-0.1.1.msi
└── BullMCClient-portable-*.zip
```

## 📌 Другие ссылки

- 🔥 GitHub Releases: [скачать последнюю версию](https://github.com/idnyakrytoi-afk/bulauncher/releases)
- 🎬 YouTube: [@idnyakrytoi-afk](https://www.youtube.com/@idnyakrytoi-afk)
- 🐙 GitHub: [idnyakrytoi-afk](https://github.com/idnyakrytoi-afk)
