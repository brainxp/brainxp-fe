package com.example.brainxp.blocking

import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.DeviceRole

fun dueForBindingCheck(
    role: DeviceRole,
    now: Long,
    lastCheckAt: Long?,
    windowMs: Long,
): Boolean = role == DeviceRole.CHILD && (lastCheckAt == null || now - lastCheckAt >= windowMs)

fun releasesDevice(binding: DeviceBinding?): Boolean = binding != null && !binding.bound
