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
│       │       ├── Theme.kt         # Темы оформления
│       │       ├── ui/              # UI компоненты
│       │       ├── core/            # Бизнес-логика
│       │       └── api/             # API клиент
│       └── resources/
└── README.md
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
