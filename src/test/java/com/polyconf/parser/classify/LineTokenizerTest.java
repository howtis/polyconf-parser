package com.polyconf.parser.classify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineTokenizerTest {

    @Test
    void simpleKeyValue() {
        List<Token> tokens = LineTokenizer.tokenize("key=value");
        assertEquals(3, tokens.size());
        assertEquals("key", tokens.get(0).text());
        assertEquals(TokenKind.WORD, tokens.get(0).kind());
        assertEquals("=", tokens.get(1).text());
        assertEquals(TokenKind.DELIMITER, tokens.get(1).kind());
        assertFalse(tokens.get(1).spaceBefore());
        assertFalse(tokens.get(1).spaceAfter());
    }

    @Test
    void tomlStyleEquals() {
        List<Token> tokens = LineTokenizer.tokenize("key = value");
        assertEquals(3, tokens.size());
        assertEquals("=", tokens.get(1).text());
        assertTrue(tokens.get(1).spaceBefore());
        assertTrue(tokens.get(1).spaceAfter());
    }

    @Test
    void dottedKey() {
        List<Token> tokens = LineTokenizer.tokenize("server.host=localhost");
        assertEquals(3, tokens.size());
        assertEquals("server.host", tokens.get(0).text());
        assertTrue(tokens.get(0).hasDot());
    }

    @Test
    void quotedStrings() {
        List<Token> tokens = LineTokenizer.tokenize("\"name\": \"app\"");
        assertEquals(3, tokens.size());
        assertEquals("\"name\"", tokens.get(0).text());
        assertEquals(TokenKind.QUOTED, tokens.get(0).kind());
        assertEquals(":", tokens.get(1).text());
        assertEquals("\"app\"", tokens.get(2).text());
    }

    @Test
    void sectionHeader() {
        List<Token> tokens = LineTokenizer.tokenize("[database]");
        assertEquals(3, tokens.size());
        assertEquals("[", tokens.get(0).text());
        assertEquals("database", tokens.get(1).text());
        assertEquals("]", tokens.get(2).text());
    }

    @Test
    void doubleBracketHeader() {
        List<Token> tokens = LineTokenizer.tokenize("[[servers]]");
        assertEquals(3, tokens.size());
        assertEquals("[[", tokens.get(0).text());
        assertEquals("servers", tokens.get(1).text());
        assertEquals("]]", tokens.get(2).text());
    }

    @Test
    void xmlTag() {
        List<Token> tokens = LineTokenizer.tokenize("<config>");
        assertEquals(3, tokens.size());
        assertEquals("<", tokens.get(0).text());
        assertEquals("config", tokens.get(1).text());
        assertEquals(">", tokens.get(2).text());
    }

    @Test
    void emptyLine() {
        List<Token> tokens = LineTokenizer.tokenize("");
        assertTrue(tokens.isEmpty());
    }

    @Test
    void numberLiteral() {
        List<Token> tokens = LineTokenizer.tokenize("42");
        assertEquals(1, tokens.size());
        assertTrue(tokens.get(0).isNumberLiteral());
    }

    @Test
    void jsonLiteral() {
        List<Token> tokens = LineTokenizer.tokenize("null");
        assertEquals(1, tokens.size());
        assertEquals("null", tokens.get(0).text());
    }

    @Test
    void yamlDocSeparator() {
        List<Token> tokens = LineTokenizer.tokenize("---");
        assertEquals(1, tokens.size());
        assertEquals("---", tokens.get(0).text());
    }
}
