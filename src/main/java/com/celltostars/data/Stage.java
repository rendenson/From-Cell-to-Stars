package com.celltostars.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Stage implements StringRepresentable {
    NONE("none"),
    CELL("cell"),
    CREATURE("creature");

    public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);

    private final String name;

    Stage(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
