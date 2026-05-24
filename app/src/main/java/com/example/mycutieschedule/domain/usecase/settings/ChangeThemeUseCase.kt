package com.example.mycutieschedule.domain.usecase.settings

import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.domain.repository.SettingsRepository
import javax.inject.Inject

class ChangeThemeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
