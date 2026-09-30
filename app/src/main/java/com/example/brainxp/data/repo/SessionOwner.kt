package com.example.brainxp.data.repo

internal fun ownerChanged(
    lastOwnerId: String?,
    incomingOwnerId: String?,
): Boolean = lastOwnerId == null || incomingOwnerId == null || lastOwnerId != incomingOwnerId
