package com.example.mycutieschedule.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

/**
 * Доменная модель события в календаре.
 *
 * Все поля `val` — модель полностью иммутабельная. Аннотация [Stable]
 * сообщает Compose-компилятору, что инстансы можно сравнивать как value-типы
 * (равные по `equals` события дают равный output композиции). Это позволяет
 * сильно сократить количество ненужных рекомпозиций при скролле списков
 * событий в Day / Week / Month View — Compose пропускает повторное
 * выполнение функций-композаблов, в которые передаются неизменившиеся Event'ы.
 *
 * Бизнес-правила (проверяются в use case-ах, не здесь):
 *   - [title] не должно быть пустым
 *   - [startDateTime] строго раньше [endDateTime]
 *   - [color] — одно из значений [EventColor]
 *
 * Firestore: события хранятся в подколлекции `users/{ownerId}/events/{id}`.
 * Room: таблица `event_table` (см. EventEntity).
 *
 * Время хранится как `LocalDateTime` (wall-clock, без таймзоны) — то, что
 * видит пользователь. AlarmManager-планировщик
 * [com.example.mycutieschedule.data.notification.EventReminderScheduler]
 * преобразует его в epoch через `ZoneId.systemDefault` перед записью в alarm.
 *
 * @param id               Уникальный ID. Пустая строка = новое событие
 *                         (UUID назначит AddEventUseCase).
 * @param title            Короткое название для карточки события в календаре.
 * @param description      Опциональное расширенное описание.
 * @param startDateTime    Начало (включительно).
 * @param endDateTime      Конец (исключая). Должно быть строго после [startDateTime].
 * @param color            Один из предопределённых розовых оттенков.
 * @param reminder         Опциональное напоминание (минут до начала + тип).
 * @param recurrence       Опциональное правило повтора. Null = одиночное событие.
 * @param ownerId          Firebase Auth UID создателя события (или viewer'а для
 *                         импортированных — см. ShareRepositoryImpl).
 * @param isImported       True если событие скопировано из чужого общего расписания.
 * @param sourceScheduleId Share-код источника (только при [isImported] = true).
 * @param recurrenceGroupId Общий ID всей цепочки повторений (мастер + сгенерированные).
 *                          Null для одиночных событий.
 * @param isRecurrenceGenerated True если событие автоматически сгенерировано
 *                              из мастера повторов. У самого мастера = false.
 * @param createdAt        Когда событие было создано.
 * @param updatedAt        Время последнего изменения — используется для
 *                         last-write-wins разрешения конфликтов при Firestore-sync.
 */
@Stable
data class Event(
    val id: String = "",
    val title: String,
    val description: String = "",
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val color: EventColor = EventColor.PEONY,
    val reminder: Reminder? = null,
    val recurrence: Recurrence? = null,
    val ownerId: String,
    val isImported: Boolean = false,
    val sourceScheduleId: String? = null,
    val recurrenceGroupId: String? = null,
    val isRecurrenceGenerated: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
