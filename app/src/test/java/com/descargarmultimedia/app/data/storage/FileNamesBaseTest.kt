package com.descargarmultimedia.app.data.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNamesBaseTest {

    @Test
    fun `limpia un nombre sin extension`() {
        assertEquals("AC_DC_ Back In Black", FileNames.sanitizeBase("AC/DC: Back In Black"))
        assertEquals("Nombre", FileNames.sanitizeBase("Nombre."))
    }

    @Test
    fun `un nombre en blanco recibe uno por defecto`() {
        assertEquals("descarga", FileNames.sanitizeBase("   "))
    }
}
