package com.hmdqr.telegramplugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public class MessageFormatter {
    public static String apply(String template, Map<String, String> values) {
        if (template == null) return "";
        String out = template;
        if (values != null) {
            for (Map.Entry<String, String> e : values.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }

    public static String plain(Component c) {
        if (c == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(c);
    }
}
