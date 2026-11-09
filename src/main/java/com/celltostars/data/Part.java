package com.celltostars.data;

/** Parts you can buy for your cell in the editor. Some can be added several times (up to maxCount). */
public enum Part {
    FLAGELLUM("flagellum", 15, 5),
    SPIKE("spike", 10, 3),
    MEMBRANE("membrane", 15, 1);

    private final String id;
    private final int cost;
    private final int maxCount;

    Part(String id, int cost, int maxCount) {
        this.id = id;
        this.cost = cost;
        this.maxCount = maxCount;
    }

    public String id() {
        return id;
    }

    public int cost() {
        return cost;
    }

    public int maxCount() {
        return maxCount;
    }

    public String nameKey() {
        return "celltostars.part." + id;
    }

    public String descKey() {
        return "celltostars.part." + id + ".desc";
    }

    public static Part byId(String id) {
        for (Part p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
