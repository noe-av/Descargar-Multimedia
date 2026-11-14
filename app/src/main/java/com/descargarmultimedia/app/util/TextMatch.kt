package com.descargarmultimedia.app.util

import java.text.Normalizer

/** Normaliza textos para compararlos sin importar mayúsculas, tildes ni signos. */
object TextMatch {

    private val DIACRITICS = Regex("""\p{M}+""")
    private val NOT_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")

    /** "Beyoncé – Halo (Live)" -> "beyonce halo live". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .replace(NOT_ALPHANUMERIC, " ")
            .trim()

    /** Como [normalize] pero sin espacios, para comparar "Lady Gaga" con "LadyGagaVEVO". */
    fun compact(text: String): String = normalize(text).replace(" ", "")

    /** true si [phrase] aparece en [text] como palabras completas ("home" no está en "homeless"). */
    fun containsPhrase(text: String, phrase: String): Boolean {
        val wanted = normalize(phrase)
        if (wanted.isEmpty()) return false
        return " ${normalize(text)} ".contains(" $wanted ")
    }
}
