package com.ruru.practice.domain.usecase

import com.ruru.practice.data.entity.MeditationSessionEntity
import com.ruru.practice.data.repository.PracticeRepository
import javax.inject.Inject

class SaveMeditationUseCase @Inject constructor(
    private val repository: PracticeRepository
) {
    suspend operator fun invoke(
        session: MeditationSessionEntity
    ) {
        repository.saveMeditation(session)
    }
}
