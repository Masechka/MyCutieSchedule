package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Returns a single event by its ID, or null if it does not exist.
 *
 * Used by [com.example.mycutieschedule.presentation.screens.event.EventEditorViewModel]
 * to load an existing event into the editor form.
 */
class GetEventByIdUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(eventId: String): Event? =
        if (eventId.isBlank()) null else repository.getEventById(eventId)
}
