package com.celltostars.data;

/** Body patterns, painted with the secondary colour (textures/entity/cell_pattern_*.png). */
public enum Pattern {
    NONE("none"),
    SPOTS("spots"),
    STRIPES("stripes"),
    PATCHES("patches");

    private final String id;

    Pattern(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "celltostars.pattern." + id;
    }

    public Pattern next() {
        Pattern[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static Pattern byId(String id) {
        for (Pattern p : values()) {
            if (p.id.equals(id)) return p;
        }
        return NONE;
    }
}
