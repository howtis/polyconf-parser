package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigList;
import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
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

        ConfigSection root = (ConfigSection) result.children().get("root");
        ConfigSection name = (ConfigSection) root.children().get("name");
        assertEquals("hello", ((ConfigValue) name.children().get("#text")).asString().orElseThrow());
    }

    @Test
    void elementWithAttributes() {
        List<String> lines = List.of("<root><db host=\"localhost\" port=\"5432\"/></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = (ConfigSection) result.children().get("root");
        ConfigSection db = (ConfigSection) root.children().get("db");
        assertEquals("localhost", ((ConfigValue) db.children().get("@host")).asString().orElseThrow());
        assertEquals("5432", ((ConfigValue) db.children().get("@port")).asString().orElseThrow());
    }

    @Test
    void nestedElements() {
        List<String> lines = List.of("<config><database><host>localhost</host></database></config>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection config = (ConfigSection) result.children().get("config");
        ConfigSection database = (ConfigSection) config.children().get("database");
        ConfigSection host = (ConfigSection) database.children().get("host");
        assertEquals("localhost", ((ConfigValue) host.children().get("#text")).asString().orElseThrow());
    }

    @Test
    void repeatedElementsBecomeList() {
        List<String> lines = List.of("<root><item>a</item><item>b</item></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = (ConfigSection) result.children().get("root");
        ConfigList items = (ConfigList) root.children().get("item");
        assertEquals(2, items.items().size());
        ConfigSection first = (ConfigSection) items.items().get(0);
        assertEquals("a", ((ConfigValue) first.children().get("#text")).asString().orElseThrow());
    }

    @Test
    void multilineXml() {
        List<String> lines = List.of(
                "<root>",
                "  <key>value</key>",
                "</root>"
        );
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = (ConfigSection) result.children().get("root");
        assertNotNull(root.children().get("key"));
    }

    @Test
    void elementWithTextAndChild() {
        List<String> lines = List.of("<root><parent>text<child>val</child></parent></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = (ConfigSection) result.children().get("root");
        ConfigSection parent = (ConfigSection) root.children().get("parent");
        assertEquals("text", ((ConfigValue) parent.children().get("#text")).asString().orElseThrow());
        assertNotNull(parent.children().get("child"));
    }

    @Test
    void selfClosingTagsFormEmptySections() {
        List<String> lines = List.of("<root><empty/></root>");
        ConfigSection result = parser.parse(lines).section();

        ConfigSection root = (ConfigSection) result.children().get("root");
        ConfigNode empty = root.children().get("empty");
        assertNotNull(empty);
        assertInstanceOf(ConfigSection.class, empty);
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
