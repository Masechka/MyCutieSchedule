<div align="center">

# 🩷 MyCutieSchedule

**Современное Android-приложение для управления личным расписанием**
с офлайн-режимом, синхронизацией через Firebase и фирменным розовым дизайном.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_+_Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-Educational-pink)](#-лицензия)

</div>

---

## 📑 Содержание

- [🌸 Возможности](#-возможности)
- [📱 Скриншоты](#-скриншоты)
- [🏗 Архитектура](#-архитектура)
- [🛠 Технологический стек](#-технологический-стек)
- [📁 Структура проекта](#-структура-проекта)
- [🗄 Схема локальной БД](#-схема-локальной-бд)
- [🔔 Напоминания и алармы](#-напоминания-и-алармы)
- [🎨 Темы оформления](#-темы-оформления)
- [🤝 Совместные расписания](#-совместные-расписания)
- [📝 Лицензия и авторство](#-лицензия)

---

## 🌸 Возможности

### 📅 Календарь и события
- **Три представления:** месяц (с цветовым отображением нагрузки), неделя (Mon–Sun, временная сетка), день (детальный таймлайн).
- **Создание и редактирование событий:** заголовок, описание, время начала и конца, цвет (8 розовых оттенков), напоминание, повторы.
- **Заметки на день:** свободный текст в Day View, сохраняется автоматически.
- **Перекрывающиеся события:** алгоритм раскладки в параллельные колонки внутри одного дня.
- **Адаптивный диапазон часов:** Week / Day View автоматически расширяют видимый диапазон, если есть события за пределами 07:00–21:00.

### 🔁 Повторяющиеся события
Поддерживаются 6 типов повторов:
| Тип | Горизонт генерации | Описание |
|-----|--------------------|----------|
| **DAILY** | 3 месяца | Каждый день |
| **WEEKLY** | 6 месяцев | Каждую неделю (с шагом N недель) |
| **по дням недели** | 6 месяцев | Конкретные дни недели (пн, ср, пт …) |
| **MONTHLY** | 12 месяцев | Каждый месяц (с переносом на последний день, если 31-е) |
| **YEARLY** | 12 месяцев | Каждый год |
| **CUSTOM_DATES** | 12 месяцев | Конкретные выбранные даты |

Жёсткий лимит безопасности — **500 экземпляров на серию**. Удаление умное: можно убрать одно событие или всю цепочку начиная с этой даты.

### 🤝 Совместные расписания (Shared Schedules)
- Любой пользователь может **сгенерировать 6-символьный share-код** и поделиться им.
- Другие пользователи импортируют расписание по коду → получают **живое зеркало** событий владельца в своём календаре.
- **Реал-тайм синхронизация:** изменения владельца (добавление, редактирование, удаление события) автоматически прилетают viewer'ам через отдельный snapshot listener на `users/{ownerId}/events`.
- **Локальные настройки** на каждое импортированное расписание:
  - Локальный алиас (переименование без воздействия на владельца).
  - Видимость в календаре (скрыть / показать).
  - Учёт в нагрузке (`includeInWorkload`) — независимо от видимости.
- **Read-only режим:** импортированные события нельзя редактировать; кнопки save / delete скрыты, поля доступны только для просмотра.
- **Серо-розовая палитра** для импортированных событий — визуально отличается от своих.

### 🔔 Напоминания
- Локальные нотификации через **AlarmManager + BroadcastReceiver**.
- На Android 12+ используется `setExactAndAllowWhileIdle`; на старых — `setAndAllowWhileIdle` (Doze-friendly).
- **Восстановление после reboot:** `BootCompletedReceiver` перепланирует все будущие алармы.
- Канал уведомлений `event_reminders` с `IMPORTANCE_HIGH`.
- Runtime-запрос `POST_NOTIFICATIONS` на Android 13+.

### 🔄 Offline-first
- **Room — единственный источник истины для UI.** Всё чтение мгновенное и работает без сети.
- **Firestore — реплика для синхронизации между устройствами.** Запись идёт сначала в Room, потом в облако (best-effort).
- **Сильная защита от потери данных:**
  - Last-write-wins разрешение конфликтов по `updatedAt`.
  - 60-секундное safety-window против стирания свежих локальных правок.
  - Bootstrap fetch при логине + постоянный snapshot listener.

### 🔐 Аутентификация
- Регистрация и вход через Firebase Authentication (email + password).
- Смена пароля с обязательной **re-аутентификацией** через `EmailAuthProvider` (требование Firebase для sensitive операций).
- Logout с подтверждением; локальные данные остаются на устройстве после выхода.

### 🎨 Темы
- **Светлая розовая** (бренд) и **тёмная розовая**.
- Material 3 ColorScheme + ExtendedColors (heatmap intensity, today indicator, weekend day color).
- Переключение из настроек, сохраняется в DataStore, переживает рестарт.
- Splash screen с тёмным вариантом для system dark mode.

---

## 📱 Скриншоты

> 💡 *Скриншоты лежат в папке `docs/screenshots/` (добавляются вручную после первого запуска)*

| Месяц | Неделя | День |
|-------|--------|------|
| ![Month](docs/screenshots/month.png) | ![Week](docs/screenshots/week.png) | ![Day](docs/screenshots/day.png) |

| Редактор события | Общие расписания | Настройки |
|------------------|------------------|-----------|
| ![Editor](docs/screenshots/editor.png) | ![Shared](docs/screenshots/shared.png) | ![Settings](docs/screenshots/settings.png) |

---

## 🏗 Архитектура

Проект построен на принципах **Clean Architecture** с тремя слоями + поддерживающими модулями. UI следует паттерну **MVVM** с `StateFlow` для one-way data flow.

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                      │
│  Jetpack Compose UI · ViewModel · UiState · One-shot Events │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                          │
│      UseCase · Repository interface · Domain models         │
│           (Чистый Kotlin, без Android-зависимостей)         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                            │
│  Repository impl · Room DAO/Entity · Firestore DTO          │
│        Mapper · SyncManager · NotificationScheduler         │
└─────────────────────────────────────────────────────────────┘
                ↕                                ↕
        ┌──────────────┐                ┌────────────────┐
        │   Room DB    │                │   Firestore    │
        │ (источник    │  ← Sync Mgr →  │   (зеркало)    │
        │   истины)    │                │                │
        └──────────────┘                └────────────────┘
```

### Ключевые принципы

- **Offline-first.** UI читает только Room; Firestore работает как асинхронное зеркало.
- **Reactive.** Flow от Room → ViewModel.StateFlow → Compose. Изменения в БД автоматически прилетают на экраны.
- **Single source of truth.** Каждый кусок данных существует ровно в одном месте.
- **Dependency rule.** Слой data зависит от domain (через интерфейсы), но не наоборот. Domain ничего не знает про Android.
- **Dependency Injection через Hilt** — все DI-связки декларативны.

### Ключевые компоненты

| Компонент | Ответственность |
|-----------|-----------------|
| `EventRepositoryImpl` | offline-first CRUD для событий, Room first → Firestore best-effort |
| `EventSyncManager` | живой snapshot listener `users/{uid}/events` + bootstrap fetch + last-write-wins |
| `SharedSchedulesSyncManager` | по одному listener'у на каждое импортированное расписание |
| `ShareRepositoryImpl` | генерация share-кодов, импорт, локальные настройки расписаний |
| `EventReminderScheduler` | AlarmManager-обёртка с graceful fallback на inexact alarms |
| `GenerateRecurringEventsUseCase` | разворачивание мастера повторов в N экземпляров с горизонтами и safety cap |
| `MyCutieScheduleTheme` | Material 3 ColorScheme + ExtendedColors + Spacing + Elevation tokens |

---

## 🛠 Технологический стек

### Core
- **Kotlin** 2.0+ (с Coroutines, Flow, Serialization)
- **Min SDK 26** (Android 8.0) · **Target SDK 36**
- **Gradle Kotlin DSL** + Version Catalog (`libs.versions.toml`)

### UI
- **Jetpack Compose** + **Material 3**
- **Compose Navigation** 2.8+ (type-safe routes через `@Serializable`)
- **Compose Foundation** (HorizontalPager для свайпов между месяцами/неделями/днями)
- Custom `ExtendedColors` для heatmap-палитры и кастомных токенов

### Данные
- **Room** (локальная БД, версия схемы 5, 4 миграции)
- **Firebase Authentication** (email + password)
- **Cloud Firestore** с локальным persistence cache (`PersistentCacheSettings`)
- **DataStore Preferences** (тема, локальные настройки)

### Инфраструктура
- **Hilt** — Dependency Injection
- **KSP** — Annotation processing для Hilt и Room
- **Timber** — структурированный логгинг
- **AlarmManager** + **BroadcastReceiver** — локальные напоминания
- **WorkManager-style goAsync()** в `BootCompletedReceiver`

### Тестирование & инструменты
- Compose Preview для UI-компонентов
- Lint rules
- (готово к расширению JUnit + Mockk)

---

## 📁 Структура проекта

```
MyCutieSchedule/
├── app/src/main/
│   ├── java/com/example/mycutieschedule/
│   │   │
│   │   ├── core/                       # Общие утилиты, не зависящие от слоёв
│   │   │   ├── extensions/             #  - расширения LocalDateTime, etc.
│   │   │   └── util/                   #  - Result wrapper, RussianPlurals
│   │   │
│   │   ├── domain/                     # ЧИСТЫЙ KOTLIN — БЕЗ ANDROID-ЗАВИСИМОСТЕЙ
│   │   │   ├── model/                  #  - Event, SharedSchedule, User, ThemeMode...
│   │   │   ├── repository/             #  - интерфейсы (контракты)
│   │   │   └── usecase/                #  - business logic
│   │   │       ├── auth/               #     · SignIn, SignUp, Logout, ChangePassword
│   │   │       ├── event/              #     · Add, Update, Delete, Generate*Recurring*
│   │   │       ├── share/              #     · Share, GetSharedSchedule, Import...
│   │   │       └── settings/           #     · ObserveTheme, ChangeTheme
│   │   │
│   │   ├── data/                       # РЕАЛИЗАЦИИ + Android-специфика
│   │   │   ├── local/                  # Room — локальная БД
│   │   │   │   ├── dao/                #  - DAO с Flow-запросами
│   │   │   │   ├── entity/             #  - @Entity таблицы
│   │   │   │   └── database/           #  - AppDatabase + миграции
│   │   │   ├── remote/                 # Firestore
│   │   │   │   ├── api/                #  - интерфейсы datasource
│   │   │   │   ├── datasource/         #  - реализации с snapshot listener
│   │   │   │   ├── dto/                #  - DTO для сериализации
│   │   │   │   └── util/               #  - FirebaseErrorMapper
│   │   │   ├── mapper/                 # Domain ↔ Entity ↔ DTO
│   │   │   ├── repository/             # Реализации Repository-интерфейсов
│   │   │   ├── sync/                   # ЦЕНТРАЛЬНАЯ синхронизация
│   │   │   │   ├── EventSyncManager           # свои события ↔ Firestore
│   │   │   │   └── SharedSchedulesSyncManager # импортированные ← владельцы
│   │   │   └── notification/           # Напоминания
│   │   │       ├── EventReminderScheduler     # AlarmManager
│   │   │       ├── ReminderReceiver           # BroadcastReceiver → notification
│   │   │       ├── BootCompletedReceiver      # восстановление после reboot
│   │   │       └── ReminderNotificationChannel
│   │   │
│   │   ├── di/                         # Hilt modules
│   │   │   ├── AppModule.kt
│   │   │   ├── CoroutineModule.kt      #  - @ApplicationScope
│   │   │   ├── DatabaseModule.kt       #  - Room + миграции
│   │   │   ├── FirebaseModule.kt       #  - Auth + Firestore с PersistentCache
│   │   │   ├── RemoteDataSourceModule.kt
│   │   │   └── RepositoryModule.kt
│   │   │
│   │   ├── presentation/               # UI слой
│   │   │   ├── AppContent.kt           #  - корневой composable + theme + nav
│   │   │   ├── AppViewModel.kt         #  - auth state + theme state
│   │   │   ├── components/             #  - переиспользуемые: EmptyState, Shimmer, LoadingPlaceholder
│   │   │   ├── navigation/             #  - Screen.kt, AppNavGraph.kt, NavTransitions.kt
│   │   │   ├── screens/                #  - UI каждого экрана + ViewModel
│   │   │   │   ├── auth/               #     · Welcome, SignIn, SignUp, ChangePassword
│   │   │   │   ├── calendar/           #     · Month, Week, Day Views + ViewModels
│   │   │   │   ├── event/              #     · EventEditor
│   │   │   │   ├── main/               #     · MainShell (bottom-nav)
│   │   │   │   ├── profile/            #     · Profile (logout, account info)
│   │   │   │   ├── settings/           #     · Settings (theme picker)
│   │   │   │   └── share/              #     · SharedSchedules + Details
│   │   │   └── theme/                  #  - Color, Theme, Spacing, Elevation, Typography, Shapes
│   │   │
│   │   ├── MainActivity.kt             # Единственный Activity + permission request
│   │   └── MyCutieScheduleApp.kt       # Application + Hilt + sync managers
│   │
│   ├── res/
│   │   ├── drawable/                   # ic_launcher_*, splash_background
│   │   ├── drawable-night/             # splash для system dark mode
│   │   ├── mipmap-anydpi-v26/          # adaptive icons
│   │   └── values/                     # strings, themes
│   │
│   └── AndroidManifest.xml
│
├── docs/                               # Документация (опционально)
│   └── screenshots/
│
├── build.gradle.kts                    # Корневой Gradle
├── settings.gradle.kts
├── gradle/libs.versions.toml           # Version Catalog
├── google-services.json                # ← Firebase config (НЕ коммитить!)
└── README.md
```

---

## 🗄 Схема локальной БД

Room database **`mycutieschedule.db`**, текущая версия **5**, 4 миграции:

| Версия | Изменение |
|--------|-----------|
| 1 | Базовая схема: `event_table`, `day_note_table`, `shared_schedule_table` |
| 1→2 | `event_table`: добавлены колонки повторов (`recurrence_interval_weeks`, `recurrence_custom_dates`, `recurrence_group_id`, `is_recurrence_generated`) |
| 2→3 | `shared_schedule_table`: добавлена `local_alias_name` (локальный алиас расписания) |
| 3→4 | `shared_schedule_table`: добавлена `is_visible_in_calendar` (флаг видимости) |
| 4→5 | `shared_schedule_table`: добавлена `include_in_workload` (учёт в нагрузке) |

### Таблицы

- **`event_table`** — события. Datetime хранится как `Long` (UTC epoch millis), время — wall-clock. Индексы: `owner_id`, `start_datetime`.
- **`day_note_table`** — текстовые заметки на день.
- **`shared_schedule_table`** — метаданные импортированных расписаний (события лежат в `event_table` с `is_imported=1`). Индексы: `viewer_id`, `share_code`.

---

## 🔔 Напоминания и алармы

```
   user сохраняет событие с reminder
                ↓
   EventRepositoryImpl.addEvent / updateEvent
                ↓
   EventReminderScheduler.schedule(event)
                ↓
   AlarmManager.setExactAndAllowWhileIdle (или setAndAllowWhileIdle на отказе SCHEDULE_EXACT_ALARM)
                ↓
   ⏰ при наступлении срока
                ↓
   ReminderReceiver (BroadcastReceiver)
                ↓
   NotificationCompat + NotificationManagerCompat → пуш
```

### Восстановление после рестарта

`BootCompletedReceiver` слушает 4 системных события (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`) и через `goAsync()` + корутину пересчитывает все будущие напоминания из Room.

### Permissions

В `AndroidManifest.xml`:
- `POST_NOTIFICATIONS` — runtime-запрос на 13+
- `RECEIVE_BOOT_COMPLETED` — для восстановления
- `USE_EXACT_ALARM` (13+, auto-grant для календарных приложений)
- `SCHEDULE_EXACT_ALARM` (12 fallback, может быть revoked)

---

## 🎨 Темы оформления

Текущая версия поддерживает **2 темы**:
- 🌷 **Розовая светлая** (бренд)
- 🌙 **Розовая тёмная**

Управление в **Settings → Тема**. Выбор сохраняется в DataStore Preferences и применяется через корневой `MyCutieScheduleTheme(darkTheme: Boolean)` в `AppContent`.

### Дизайн-система
- **ColorScheme** Material 3 (light + dark)
- **ExtendedColors** для специальных токенов:
  - `eventColors` — палитра события (8 розовых оттенков)
  - `eventImported` — серо-розовый для импортированных
  - `calendarIntensity` — 4 уровня тепловой карты Month View
  - `todayIndicator`, `selectedDayBackground`, `weekendDayColor`
- **Spacing tokens** (extraSmall 4dp → massive 64dp)
- **Elevation tokens** (card, dialog, modal)
- **Typography** (CutieTypography — Material 3 styles)
- **Shapes** (CutieShapes — пирожковые скругления)

---

## 🤝 Совместные расписания

### Поток

```
ВЛАДЕЛЕЦ                                     VIEWER
─────────                                    ──────
1. Открывает «Общие расписания»
2. Создаёт share-код (6 символов)
3. sharedSchedules/{code} = {userId, ...}
4. users/{userId}.shareCode = code
5. Отправляет код любым способом
                                             6. Открывает «Общие расписания»
                                             7. Вводит код
                                             8. Читает sharedSchedules/{code}
                                             9. Скачивает users/{ownerId}/events
                                             10. Сохраняет в Room с:
                                                 - owner_id = viewerId
                                                 - is_imported = 1
                                                 - source_schedule_id = code
                                            
                                            ↻ SharedSchedulesSyncManager
                                              регистрирует snapshot listener
                                              на users/{ownerId}/events

11. Редактирует событие
12. EventRepositoryImpl → Room + Firestore   ⟶ listener срабатывает у viewer'а
                                             ⟶ зеркало в Room обновляется
                                             ⟶ UI viewer'а обновляется живо
```

### Особенности
- **Уникальность кода:** 6 символов, базируются на UUID → коллизия практически невозможна.
- **Отзыв доступа:** `revokeShareCode` удаляет `users/{userId}.shareCode` → security rule `get(...).data.shareCode != null` начинает возвращать false → viewer'ы получают PERMISSION_DENIED при попытке прочесть события.
- **Локальные настройки viewer'а:** алиас, видимость, учёт в нагрузке — НИКОГДА не пишутся в Firestore. Это исключительно viewer's UI preferences в Room.

---

## 📊 Производительность и оптимизации

| Где | Оптимизация |
|-----|-------------|
| Compose | `Event` помечен `@Stable` — Compose skip-ит рекомпозицию карточек при равных событиях |
| LazyColumn | Везде расставлены `key = { it.id }` — корректное reuse элементов при скролле |
| Sync | `distinctUntilChanged` по userId — нет повторных перезапусков sync на одинаковых auth-событиях |
| Sync | 60-сек safety window против удаления свежих локальных правок |
| Generation | Per-type горизонты повторов + жёсткий cap 500 событий |
| Firestore | `PersistentCacheSettings` — мгновенное чтение из кэша при offline |
| Room | Индексы на `owner_id`, `start_datetime`, `viewer_id`, `share_code` |
| ViewModel | `SharingStarted.WhileSubscribed(5_000)` — Flow не пересоздаётся при ротации экрана |

---

## 🧭 Roadmap (что можно добавить)

- [ ] Виджет на главный экран (один день / неделя)
- [ ] Calendar-event widget (Resizeable)
- [ ] Google Calendar import (`.ics`)
- [ ] FCM push для уведомлений о новых событиях в shared schedules
- [ ] Темы Leopard / Pink Leopard (luxury palette)
- [ ] Cloud Functions для серверной валидации share-кодов
- [ ] Unit / Instrumented тесты
- [ ] CI/CD (GitHub Actions)

---

## 📝 Лицензия

Учебный проект. Разработан в рамках дипломной работы.
Использование разрешено для образовательных и портфолио-целей.

---

## 👤 Автор

**Студент** · разработка Android + дипломная работа

<div align="center">

---

Сделано с 🩷 на Kotlin и Jetpack Compose.

</div>
