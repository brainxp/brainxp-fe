package com.example.brainxp.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.domain.model.ActivityEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

const val ACTIVITY_LOG_LIMIT = 100

@HiltViewModel
class ActivityLogViewModel
    @Inject
    constructor(
        log: ActivityLogRepository,
    ) : ViewModel() {
        val events: StateFlow<List<ActivityEvent>> =
            log
                .observeRecent(ACTIVITY_LOG_LIMIT)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
