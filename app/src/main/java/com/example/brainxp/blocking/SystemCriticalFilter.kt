package com.example.brainxp.blocking

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean = false,
)

data class ProtectedPackages(
    val own: String,
    val launcher: String? = null,
    val settings: String? = null,
    val dialer: String? = null,
    val emergency: Set<String> = emptySet(),
) {
    val all: Set<String>
        get() = (setOfNotNull(own, launcher, settings, dialer) + emergency).filter { it.isNotBlank() }.toSet()
}

object SystemCriticalFilter {
    fun isProtected(
        packageName: String,
        protected: ProtectedPackages,
    ): Boolean = packageName in protected.all

    fun selectable(
        apps: List<InstalledApp>,
        protected: ProtectedPackages,
    ): List<InstalledApp> =
        apps
            .filterNot { isProtected(it.packageName, protected) }
            .sortedWith(compareByDescending<InstalledApp> { it.isGame }.thenBy { it.label.lowercase() })
}
