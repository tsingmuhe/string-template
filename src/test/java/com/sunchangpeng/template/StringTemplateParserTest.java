package com.sunchangpeng.template;

import org.junit.Test;

public class StringTemplateParserTest {
    @Test
    public void test() {
        StringTemplateParser stp = new StringTemplateParser();
        stp.parse("xxx${small}xxx", String::toUpperCase);
        System.out.println(stp.parse("xxx${small}xxx", String::toUpperCase));
    }
}