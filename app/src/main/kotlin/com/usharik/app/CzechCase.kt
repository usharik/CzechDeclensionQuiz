package com.usharik.app

enum class CzechCase(
    @JvmField val index: Int,
    @JvmField val displayName: String,
    @JvmField val helperWord: String,
    @JvmField val question: String,
) {
    NOMINATIV(0, "Nominativ", "", "kdo? co?"),
    GENITIV(1, "Genitiv", "bez", "koho? čeho?"),
    DATIV(2, "Dativ", "ke", "komu? čemu?"),
    AKUZATIV(3, "Akuzativ", "vidim", "koho? co?"),
    VOKATIV(4, "Vokativ", "", "-"),
    LOKAL(5, "Lokál", "o", "(o) kom? (o) čem?"),
    INSTRUMENTAL(6, "Instrumentál", "s", "kým? čím?");

    companion object {
        @JvmStatic fun fromIndex(index: Int): CzechCase = entries.firstOrNull { it.index == index }
            ?: throw IllegalArgumentException("Invalid case index: $index. Valid range is 0-6.")
    }
}
