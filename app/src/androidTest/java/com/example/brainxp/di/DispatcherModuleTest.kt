package com.example.brainxp.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DispatcherModuleTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun qualifiersResolveToTheirIntendedDispatchers() {
        assertEquals(Dispatchers.IO, ioDispatcher)
        assertEquals(Dispatchers.Default, defaultDispatcher)
        assertEquals(Dispatchers.Main, mainDispatcher)
    }

    @Test
    fun ioAndDefaultAreDistinct() {
        assertNotEquals(ioDispatcher, defaultDispatcher)
    }
}
