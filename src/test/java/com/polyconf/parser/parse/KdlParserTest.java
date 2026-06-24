package com.polyconf.parser.parse;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KdlParserTest {

    private final KdlParser parser = new KdlParser();

    @Test
    void simpleNodeWithChildren() {
        List<String> lines = List.of(
                "server {",
                "  host \"localhost\"",
                "  port 8080",
                "}"
        );

        ParserResult result = parser.parse(lines);

        Map<String, ConfigNode> root = result.section().children();
        assertEquals(1, root.size());
        assertTrue(root.containsKey("server"));

        ConfigSection server = (ConfigSection) root.get("server");
        Map<String, ConfigNode> serverChildren = server.children();
        assertEquals(2, serverChildren.size());

        ConfigSection host = (ConfigSection) serverChildren.get("host");
        assertNotNull(host);
        ConfigValue hostVal = (ConfigValue) host.children().get("0");
        assertEquals("localhost", hostVal.get());

        ConfigSection port = (ConfigSection) serverChildren.get("port");
        assertNotNull(port);
        ConfigValue portVal = (ConfigValue) port.children().get("0");
        assertEquals(8080L, portVal.get());
    }

    @Test
    void nodeWithProperties() {
        List<String> lines = List.of(
                "author name=\"Alex\" email=alex@example.com active=#true"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection author = (ConfigSection) result.section().children().get("author");
        assertNotNull(author);
        assertEquals("Alex", ((ConfigValue) author.children().get("name")).get());
        assertEquals("alex@example.com", ((ConfigValue) author.children().get("email")).get());
        assertEquals(true, ((ConfigValue) author.children().get("active")).get());
    }

    @Test
    void nodeWithArguments() {
        List<String> lines = List.of(
                "bookmarks 12 15 188"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection bookmarks = (ConfigSection) result.section().children().get("bookmarks");
        assertNotNull(bookmarks);
        assertEquals(12L, ((ConfigValue) bookmarks.children().get("0")).get());
        assertEquals(15L, ((ConfigValue) bookmarks.children().get("1")).get());
        assertEquals(188L, ((ConfigValue) bookmarks.children().get("2")).get());
    }

    @Test
    void nestedNodes() {
        List<String> lines = List.of(
                "contents {",
                "  section \"First section\" {",
                "    paragraph \"This is first\"",
                "    paragraph \"This is second\"",
                "  }",
                "}"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection contents = (ConfigSection) result.section().children().get("contents");
        assertNotNull(contents);

        ConfigSection section = (ConfigSection) contents.children().get("section");
        assertNotNull(section);

        ConfigSection para1 = (ConfigSection) section.children().get("paragraph");
        assertNotNull(para1);
        assertEquals("This is first", ((ConfigValue) para1.children().get("0")).get());

        ConfigSection para2 = (ConfigSection) section.children().get("paragraph_1");
        assertNotNull(para2);
        assertEquals("This is second", ((ConfigValue) para2.children().get("0")).get());
    }

    @Test
    void lineCommentsAreIgnored() {
        List<String> lines = List.of(
                "// This is a comment",
                "server {",
                "  // Another comment",
                "  host \"localhost\"",
                "}"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection server = (ConfigSection) result.section().children().get("server");
        assertNotNull(server);
        ConfigSection host = (ConfigSection) server.children().get("host");
        assertNotNull(host);
        assertEquals("localhost", ((ConfigValue) host.children().get("0")).get());
    }

    @Test
    void blockCommentsAreIgnored() {
        List<String> lines = List.of(
                "/* block comment */",
                "server {",
                "  host \"localhost\"",
                "}"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection server = (ConfigSection) result.section().children().get("server");
        assertNotNull(server);
        ConfigSection host = (ConfigSection) server.children().get("host");
        assertNotNull(host);
        assertEquals("localhost", ((ConfigValue) host.children().get("0")).get());
    }

    @Test
    void slashdashCommentsAreIgnored() {
        List<String> lines = List.of(
                "/-commented node",
                "server {",
                "  host \"localhost\"",
                "}"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection server = (ConfigSection) result.section().children().get("server");
        assertNotNull(server);
        assertEquals("localhost",
                ((ConfigValue) ((ConfigSection) server.children().get("host")).children().get("0")).get());
    }

    @Test
    void booleanAndNullValues() {
        List<String> lines = List.of(
                "settings debug=#true verbose=#false none=#null"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection settings = (ConfigSection) result.section().children().get("settings");
        assertNotNull(settings);
        assertEquals(true, ((ConfigValue) settings.children().get("debug")).get());
        assertEquals(false, ((ConfigValue) settings.children().get("verbose")).get());
        assertNull(((ConfigValue) settings.children().get("none")).get());
        assertTrue(((ConfigValue) settings.children().get("none")).isNull());
    }

    @Test
    void quotedStrings() {
        List<String> lines = List.of(
                "node \"hello world\" \"multi\\nline\""
        );

        ParserResult result = parser.parse(lines);

        ConfigSection node = (ConfigSection) result.section().children().get("node");
        assertNotNull(node);
        assertEquals("hello world", ((ConfigValue) node.children().get("0")).get());
    }

    @Test
    void emptyInput() {
        ParserResult result = parser.parse(List.of());
        assertTrue(result.section().children().isEmpty());
    }

    @Test
    void blankLines() {
        ParserResult result = parser.parse(List.of("", "  ", ""));
        assertTrue(result.section().children().isEmpty());
    }

    @Test
    void nullLinesThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void semicolonSeparatedNodes() {
        List<String> lines = List.of(
                "node1; node2; node3"
        );

        ParserResult result = parser.parse(lines);

        Map<String, ConfigNode> root = result.section().children();
        assertEquals(3, root.size());
        assertTrue(root.containsKey("node1"));
        assertTrue(root.containsKey("node2"));
        assertTrue(root.containsKey("node3"));
    }

    @Test
    void unquotedStringValues() {
        List<String> lines = List.of(
                "title Hello-World"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection title = (ConfigSection) result.section().children().get("title");
        assertNotNull(title);
        assertEquals("Hello-World", ((ConfigValue) title.children().get("0")).get());
    }

    @Test
    void numberValues() {
        List<String> lines = List.of(
                "stats count=42 pi=3.14 hex=0xdeadbeef bin=0b1010 oct=0o755"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection stats = (ConfigSection) result.section().children().get("stats");
        assertNotNull(stats);
        assertEquals(42L, ((ConfigValue) stats.children().get("count")).get());
        assertEquals(3.14, ((ConfigValue) stats.children().get("pi")).get());
        assertEquals(0xdeadbeefL, ((ConfigValue) stats.children().get("hex")).get());
        assertEquals(0b1010L, ((ConfigValue) stats.children().get("bin")).get());
        assertEquals(0755L, ((ConfigValue) stats.children().get("oct")).get());
    }
}
