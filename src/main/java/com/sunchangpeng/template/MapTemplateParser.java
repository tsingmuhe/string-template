package com.sunchangpeng.template;

import java.util.Map;

public class MapTemplateParser extends StringTemplateParser {
    public static class Defaults {
        private static final MapTemplateParser implementation = new MapTemplateParser();
    }

    public static ContextTemplateParser of(final Map map) {
        return template -> Defaults.implementation.parseWithMap(template, map);
    }

    public String parseWithMap(final String template, final Map map) {
        return super.parse(template, macroName -> {
            Object value = map.get(macroName);
            if (value == null) {
                return null;
            }
            return value.toString();
        });
    }
}