package com.hiroto99.windowslib.util;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class StringTranslator {
    String current;

    public StringTranslator(String current) {
        this.current = current;
    }

    public StringTranslator(Component current) {
        this.current = current.getString();
    }

    public static StringTranslator literal(String text) {
        Component component = Component.translatable(text);
        return new StringTranslator(component);
    }

    public static StringTranslator translate(String key) {
        Component component = Component.translatable(key);
        return new StringTranslator(component);
    }

    public Component toLiteral() {
        return Component.literal(current);
    }

    public List<Component> toLiteralList() {
        List<Component> literals = new ArrayList<>();
        for (String literal : current.split("\n")) {
            literals.add(Component.literal(literal));
        }
        return literals;
    }
}
