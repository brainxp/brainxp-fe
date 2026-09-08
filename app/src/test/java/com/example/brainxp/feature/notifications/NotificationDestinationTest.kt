package com.example.brainxp.feature.notifications

import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.NotificationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun record(
    kind: NotificationKind,
    materialId: String? = "m-1",
) = AppNotification(
    id = "n-1",
    kind = kind,
    materialId = materialId,
    materialTitle = "Hukum Newton",
    questionCount = 7,
    createdAt = 0L,
)

class NotificationDestinationTest {
    @Test
    fun `ready questions open the quiz they belong to`() {
        assertEquals(
            NotificationsEffect.OpenQuestions("m-1"),
            destinationOf(record(NotificationKind.QUESTIONS_READY)),
        )
    }

    @Test
    fun `a turned down material opens its reason instead of doing nothing`() {
        assertEquals(
            NotificationsEffect.OpenRejection("m-1"),
            destinationOf(record(NotificationKind.MATERIAL_REJECTED)),
        )
    }

    @Test
    fun `every kind of notification leads somewhere`() {
        val stranded = NotificationKind.entries.filter { kind -> destinationOf(record(kind)) == null }

        assertEquals("these notifications would be dead ends: $stranded", emptyList<NotificationKind>(), stranded)
    }

    @Test
    fun `a notification with no material stays put rather than opening the wrong screen`() {
        assertNull(destinationOf(record(NotificationKind.QUESTIONS_READY, materialId = null)))
    }
}
