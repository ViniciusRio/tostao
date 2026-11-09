package com.viniciusrio.tostao.ui.components

import com.viniciusrio.tostao.domain.PieSlice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SliceAtTest {
    private fun s(c: Long) = PieSlice("x", c, 0, 0, listOf(null))
    private val half = listOf(s(50), s(50))

    @Test fun picksSliceByClockwiseAngleFromTop() {
        assertEquals(0, sliceAt(half, 90f))
        assertEquals(1, sliceAt(half, 270f))
    }

    @Test fun gapsBetweenSlicesSelectNothing() {
        assertNull(sliceAt(half, 0.2f))
        assertNull(sliceAt(half, 180f))
        assertNull(sliceAt(half, 359.9f))
    }

    @Test fun singleSliceHasNoGap() {
        assertEquals(0, sliceAt(listOf(s(10)), 0f))
    }
}
