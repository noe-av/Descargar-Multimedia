package com.descargarmultimedia.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formatea minutos y segundos`() {
        assertEquals("0:05", TimeFormat.eta(5))
        assertEquals("1:15", TimeFormat.eta(75))
    }

    @Test
    fun `formatea horas`() {
        assertEquals("1:02:05", TimeFormat.eta(3725))
    }

    @Test
    fun `no acepta negativos`() {
        assertEquals("0:00", TimeFormat.eta(-3))
    }
}
