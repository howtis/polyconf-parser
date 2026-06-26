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

    // --- Token isNumberLiteral edge cases ---

    @Test
    void isNumberLiteralNegative() {
        Token t = new Token("-5", TokenKind.WORD, false, false);
        assertTrue(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralFloat() {
        Token t = new Token("3.14", TokenKind.WORD, false, false);
        assertTrue(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralScientific() {
        Token t = new Token("1e5", TokenKind.WORD, false, false);
        assertTrue(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralScientificNegativeExponent() {
        Token t = new Token("1.5e-3", TokenKind.WORD, false, false);
        assertTrue(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralEmpty() {
        Token t = new Token("", TokenKind.WORD, false, false);
        assertFalse(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralWordFalse() {
        Token t = new Token("hello", TokenKind.WORD, false, false);
        assertFalse(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralQuotedNumber() {
        Token t = new Token("42", TokenKind.QUOTED, false, false);
        assertTrue(t.isNumberLiteral());
    }

    @Test
    void isNumberLiteralOnlySign() {
        Token t = new Token("-", TokenKind.WORD, false, false);
        assertFalse(t.isNumberLiteral());
    }
}
