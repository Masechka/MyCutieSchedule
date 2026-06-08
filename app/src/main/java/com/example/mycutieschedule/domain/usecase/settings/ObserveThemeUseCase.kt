package com.example.mycutieschedule.domain.usecase.settings

import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = repository.observeThemeMode()
}
