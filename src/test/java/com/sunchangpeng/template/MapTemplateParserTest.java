package com.sunchangpeng.template;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapTemplateParserTest {
    @Test
    public void test() {
        String template = "Hello ${foo}. Today is ${dayName}.";
        Map<String, String> map = new HashMap<>();
        map.put("foo", "Jodd");
        map.put("dayName", "Sunday");

        String result = MapTemplateParser.of(map).parse(template);
        System.out.println(result);
    }
}