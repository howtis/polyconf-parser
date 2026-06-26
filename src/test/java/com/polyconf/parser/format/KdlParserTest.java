package com.polyconf.parser.format;

import com.polyconf.parser.model.ConfigNode;
import com.polyconf.parser.model.ConfigSection;
import com.polyconf.parser.model.ConfigValue;
import com.polyconf.parser.model.ParserResult;
import com.polyconf.parser.parse.LenientParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KdlParserTest {

    private final LenientParser parser = new KdlFormat.Parser();

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

        ConfigValue host = (ConfigValue) serverChildren.get("host");
        assertNotNull(host);
        assertEquals("localhost", host.get());

        ConfigValue port = (ConfigValue) serverChildren.get("port");
        assertNotNull(port);
        assertEquals(8080L, port.get());
    }

    @Test
    void nodeWithProperties() {
        List<String> lines = List.of(
                "author name=\"Alex\" email=alex@example.com active=#true"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection author = result.section().childSection("author").orElseThrow();
        assertEquals("Alex", author.childValue("name").orElseThrow().get());
        assertEquals("alex@example.com", author.childValue("email").orElseThrow().get());
        assertEquals(true, author.childValue("active").orElseThrow().get());
    }

    @Test
    void nodeWithArguments() {
        List<String> lines = List.of(
                "bookmarks 12 15 188"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection bookmarks = result.section().childSection("bookmarks").orElseThrow();
        assertEquals(12L, bookmarks.childValue("0").orElseThrow().get());
        assertEquals(15L, bookmarks.childValue("1").orElseThrow().get());
        assertEquals(188L, bookmarks.childValue("2").orElseThrow().get());
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

        ConfigSection contents = result.section().childSection("contents").orElseThrow();

        ConfigSection section = contents.childSection("section").orElseThrow();

        assertEquals("This is first", section.childValue("paragraph").orElseThrow().get());
        assertEquals("This is second", section.childValue("paragraph_1").orElseThrow().get());
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

        ConfigSection server = result.section().childSection("server").orElseThrow();
        assertEquals("localhost", server.childValue("host").orElseThrow().get());
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

        ConfigSection server = result.section().childSection("server").orElseThrow();
        assertEquals("localhost", server.childValue("host").orElseThrow().get());
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

        ConfigSection server = result.section().childSection("server").orElseThrow();
        assertEquals("localhost", server.childValue("host").orElseThrow().get());
    }

    @Test
    void booleanAndNullValues() {
        List<String> lines = List.of(
                "settings debug=#true verbose=#false none=#null"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection settings = result.section().childSection("settings").orElseThrow();
        assertEquals(true, settings.childValue("debug").orElseThrow().get());
        assertEquals(false, settings.childValue("verbose").orElseThrow().get());
        assertNull(settings.childValue("none").orElseThrow().get());
        assertTrue(settings.childValue("none").orElseThrow().isNull());
    }

    @Test
    void quotedStrings() {
        List<String> lines = List.of(
                "node \"hello world\" \"multi\\nline\""
        );

        ParserResult result = parser.parse(lines);

        ConfigSection node = result.section().childSection("node").orElseThrow();
        assertEquals("hello world", node.childValue("0").orElseThrow().get());
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

        ConfigValue title = result.section().childValue("title").orElseThrow();
        assertEquals("Hello-World", title.get());
    }

    @Test
    void numberValues() {
        List<String> lines = List.of(
                "stats count=42 pi=3.14 hex=0xdeadbeef bin=0b1010 oct=0o755"
        );

        ParserResult result = parser.parse(lines);

        ConfigSection stats = result.section().childSection("stats").orElseThrow();
        assertEquals(42L, stats.childValue("count").orElseThrow().get());
        assertEquals(3.14, stats.childValue("pi").orElseThrow().get());
        assertEquals(0xdeadbeefL, stats.childValue("hex").orElseThrow().get());
        assertEquals(0b1010L, stats.childValue("bin").orElseThrow().get());
        assertEquals(0755L, stats.childValue("oct").orElseThrow().get());
    }
}
