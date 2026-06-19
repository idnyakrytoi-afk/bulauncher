<<<<<<< HEAD
# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC, написанный на Kotlin с использованием Jetbrains Compose Desktop.

## Структура проекта

```
bullmc-client/
├── build.gradle.kts           # Конфигурация зависимостей
├── settings.gradle.kts        # Настройки проекта
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── net/bullmc/client/
│       │       ├── Main.kt          # Точка входа
│       │       ├── ui/              # UI компоненты
│       │       │   ├── Sidebar.kt
│       │       │   ├── TopBanner.kt
│       │       │   ├── FriendsPanel.kt
│       │       │   └── BottomCards.kt
│       │       ├── core/            # Бизнес-логика
│       │       │   ├── Launcher.kt
│       │       │   └── Auth.kt
│       │       └── api/             # API клиент
│       │           └── ServerApi.kt
│       └── resources/               # Статические ресурсы
└── README.md
```

## Установка и запуск

### Требования
- JDK 11 или выше
- Gradle (или используй `gradlew`)

### Запуск разработки
```bash
./gradlew run
```

### Сборка распределяемого файла
```bash
./gradlew packageDistributionForCurrentOS
```

## Разработка

### Установка расширений в VS Code
1. Скачай расширение **Kotlin** от `fwcd`
2. Скачай расширение **Gradle for Java** от `vscjava`

### Структура кода

- **Main.kt** - точка входа, создает главное окно
- **ui/** - компоненты интерфейса (Sidebar, TopBanner, FriendsPanel, BottomCards)
- **core/** - основная логика (запуск игры, авторизация)
- **api/** - взаимодействие с сервером (получение списка друзей, статуса и т.д.)

## TODOs

- [ ] Реализовать загрузку фонового изображения
- [ ] Интегрировать API для получения новостей
- [ ] Реализовать скачивание обновлений
- [ ] Добавить поддержку модов
- [ ] Реализовать систему уведомлений

## Лицензия

Частное использование только для проекта BullMC
=======
# BullMC Client Launcher

Десктопный лаунчер для сервера BullMC, написанный на Kotlin с использованием Jetbrains Compose Desktop.

## Структура проекта

```
bullmc-client/
├── build.gradle.kts           # Конфигурация зависимостей
├── settings.gradle.kts        # Настройки проекта
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── net/bullmc/client/
│       │       ├── Main.kt          # Точка входа
│       │       ├── ui/              # UI компоненты
│       │       │   ├── Sidebar.kt
│       │       │   ├── TopBanner.kt
│       │       │   ├── FriendsPanel.kt
│       │       │   └── BottomCards.kt
│       │       ├── core/            # Бизнес-логика
│       │       │   ├── Launcher.kt
│       │       │   └── Auth.kt
│       │       └── api/             # API клиент
│       │           └── ServerApi.kt
│       └── resources/               # Статические ресурсы
└── README.md
```

## Установка и запуск

### Требования
- JDK 11 или выше
- Gradle (или используй `gradlew`)

### Запуск разработки
```bash
./gradlew run
```

### Сборка распределяемого файла
```bash
./gradlew packageDistributionForCurrentOS
```

## Разработка

### Установка расширений в VS Code
1. Скачай расширение **Kotlin** от `fwcd`
2. Скачай расширение **Gradle for Java** от `vscjava`

### Структура кода

- **Main.kt** - точка входа, создает главное окно
- **ui/** - компоненты интерфейса (Sidebar, TopBanner, FriendsPanel, BottomCards)
- **core/** - основная логика (запуск игры, авторизация)
- **api/** - взаимодействие с сервером (получение списка друзей, статуса и т.д.)

## TODOs

- [ ] Реализовать загрузку фонового изображения
- [ ] Интегрировать API для получения новостей
- [ ] Реализовать скачивание обновлений
- [ ] Добавить поддержку модов
- [ ] Реализовать систему уведомлений

## Лицензия

Частное использование только для проекта BullMC
>>>>>>> 2f0acdb280b4e28db51b7ed9603dbeda5603cc11
