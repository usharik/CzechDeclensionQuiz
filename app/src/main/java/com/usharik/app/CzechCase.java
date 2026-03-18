package com.usharik.app;

public enum CzechCase {
    NOMINATIV(0, "Nominativ", "kdo? co?"),
    GENITIV(1, "Genitiv", "koho? čeho?"),
    DATIV(2, "Dativ", "komu? čemu?"),
    AKUZATIV(3, "Akuzativ", "koho? co?"),
    VOKATIV(4, "Vokativ", "-"),
    LOKAL(5, "Lokál", "(o) kom? (o) čem?"),
    INSTRUMENTAL(6, "Instrumentál", "kým? čím?");

    public final int index;
    public final String name;
    public final String question;

    CzechCase(int index, String name, String question) {
        this.index = index;
        this.name = name;
        this.question = question;
    }

    public static CzechCase fromIndex(int index) {
        for (CzechCase c : values()) {
            if (c.index == index) return c;
        }
        throw new IllegalArgumentException("Invalid case index: " + index + ". Valid range is 0-6.");
    }
}
