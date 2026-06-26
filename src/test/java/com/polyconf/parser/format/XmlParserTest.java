package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlParserTest {

    private final LenientParser parser = new XmlFormat.Parser();

    @Test
    void simpleElement() {
        List<String> lines = List.of("<root><name>hello</name></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        ConfigSection name = root.childSection("name").orElseThrow();
        assertEquals("hello", name.childValue("#text").orElseThrow().asString().orElseThrow());
    }

    @Test
    void elementWithAttributes() {
        List<String> lines = List.of("<root><db host=\"localhost\" port=\"5432\"/></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        ConfigSection db = root.childSection("db").orElseThrow();
        assertEquals("localhost", db.childValue("@host").orElseThrow().asString().orElseThrow());
        assertEquals("5432", db.childValue("@port").orElseThrow().asString().orElseThrow());
    }

    @Test
    void nestedElements() {
        List<String> lines = List.of("<config><database><host>localhost</host></database></config>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection config = result.childSection("config").orElseThrow();
        ConfigSection database = config.childSection("database").orElseThrow();
        ConfigSection host = database.childSection("host").orElseThrow();
        assertEquals("localhost", host.childValue("#text").orElseThrow().asString().orElseThrow());
    }

    @Test
    void repeatedElementsBecomeList() {
        List<String> lines = List.of("<root><item>a</item><item>b</item></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        ConfigList items = root.childList("item").orElseThrow();
        assertEquals(2, items.items().size());
        ConfigSection first = (ConfigSection) items.items().get(0);
        assertEquals("a", first.childValue("#text").orElseThrow().asString().orElseThrow());
    }

    @Test
    void multilineXml() {
        List<String> lines = List.of(
                "<root>",
                "  <key>value</key>",
                "</root>"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        assertTrue(root.children().containsKey("key"));
    }

    @Test
    void elementWithTextAndChild() {
        List<String> lines = List.of("<root><parent>text<child>val</child></parent></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        ConfigSection parent = root.childSection("parent").orElseThrow();
        assertEquals("text", parent.childValue("#text").orElseThrow().asString().orElseThrow());
        assertTrue(parent.children().containsKey("child"));
    }

    @Test
    void selfClosingTagsFormEmptySections() {
        List<String> lines = List.of("<root><empty/></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = result.childSection("root").orElseThrow();
        assertNotNull(root.childSection("empty").orElse(null));
    }

    @Test
    void processingInstructionIgnored() {
        // XML declaration and PI should be ignored
        List<String> lines = List.of(
                "<?xml version=\"1.0\"?>",
                "<root><key>value</key></root>"
        );
        ConfigSection result = parser.parse(lines).section();
        assertNotNull(result.children().get("root"));
    }

    @Test
    void emptyInput() {
        ConfigSection result = parser.parse(List.of("   ")).section();
        assertTrue(result.children().isEmpty());
    }

    @Test
    void nullInput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

}
