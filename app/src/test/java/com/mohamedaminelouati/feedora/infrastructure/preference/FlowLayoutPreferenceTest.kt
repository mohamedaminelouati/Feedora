package com.mohamedaminelouati.feedora.infrastructure.preference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowLayoutPreferenceTest {

    @Test
    fun testDefaultIsList() {
        assertEquals(FlowLayoutPreference.LIST, FlowLayoutPreference.default)
        assertFalse(FlowLayoutPreference.default.isGrid())
        assertTrue(FlowLayoutPreference.GRID.isGrid())
    }

    @Test
    fun testToggleOperator() {
        val list = FlowLayoutPreference.LIST
        val grid = !list
        assertEquals(FlowLayoutPreference.GRID, grid)
        assertEquals(FlowLayoutPreference.LIST, !grid)
    }
}
