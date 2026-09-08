package com.example.brainxp.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.repo.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val notifications: NotificationRepository,
    ) : ViewModel() {
        val state: StateFlow<NotificationsUiState> =
            notifications
                .observe()
                .map { records -> NotificationsUiState(notifications = records, loading = false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), NotificationsUiState())

        private val effectChannel = Channel<NotificationsEffect>(Channel.BUFFERED)
        val effects: Flow<NotificationsEffect> = effectChannel.receiveAsFlow()

        fun onEvent(event: NotificationsEvent) {
            when (event) {
                is NotificationsEvent.Open -> open(event.id)
            }
        }

        private fun open(id: String) {
            val opened = state.value.notifications.firstOrNull { it.id == id } ?: return
            viewModelScope.launch {
                notifications.markRead(id)
                val material = opened.materialId
                if (opened.opensQuestions && material != null) {
                    effectChannel.send(NotificationsEffect.OpenQuestions(material))
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
