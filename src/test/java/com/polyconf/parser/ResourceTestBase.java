package com.polyconf.parser;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.Format;
import com.polyconf.parser.model.ParseResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTestBase {

    protected final PolyconfParser parser = new PolyconfParser();

    protected List<String> readLines(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(is, "Resource not found: " + resourcePath);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            fail("Failed to read resource: " + resourcePath, e);
        }
        return lines;
    }

    protected ParseResult parseResource(String resourcePath) {
        return parser.parse(readLines(resourcePath));
    }

    protected void assertNoErrors(ParseResult result, String description) {
        List<BlockDiagnostic> errors = result.diagnostics().stream()
                .filter(d -> d.level() == DiagnosticLevel.ERROR)
                .toList();
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Errors in " + description + ":");
            for (BlockDiagnostic d : errors) {
                sb.append("\n  L").append(d.startLine()).append("-L").append(d.endLine())
                        .append(": ").append(d.message());
            }
            fail(sb.toString());
        }
    }

    protected void assertHasBlock(ParseResult result, String description) {
        assertFalse(result.blocks().isEmpty(),
                "Expected at least one block for " + description);
    }

    protected void assertFormat(ParseResult result, Format expected, String description) {
        assertHasBlock(result, description);
        assertEquals(expected, result.blocks().get(0).detectedFormat(),
                "Wrong format detected for " + description);
    }
}
