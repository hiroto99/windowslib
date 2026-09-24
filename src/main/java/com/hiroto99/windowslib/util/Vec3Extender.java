package com.hiroto99.windowslib.util;

import net.minecraft.world.phys.Vec3;

public final class Vec3Extender {
    public static Vec3 sum(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.x + vec2.x, vec1.y + vec2.y, vec1.z + vec2.z);
    }

    public static Vec3 difference(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.x - vec2.x, vec1.y - vec2.y, vec1.z - vec2.z);
    }
}
