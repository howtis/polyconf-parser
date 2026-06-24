package com.polyconf.parser;

import com.polyconf.parser.model.BlockDiagnostic;
import com.polyconf.parser.model.DiagnosticLevel;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Quick diagnostic to inspect actual parse output for resource files.
 * Run manually, not part of the test suite.
 */

public class ResourceDiagnostic {
    @Test
    void dump() throws IOException {
        PolyconfParser parser = new PolyconfParser();

        String[][] files = {
                {"json5/config.json5", "JSON5 config"},
                {"json5/data.json5", "JSON5 data"},
                {"json5/settings.json5", "JSON5 settings"},
                {"toml/server.toml", "TOML server"},
                {"toml/complex.toml", "TOML complex"},
                {"yaml/simple.yaml", "YAML simple"},
                {"yaml/server.yaml", "YAML server"},
                {"yaml/complex.yaml", "YAML complex"},
                {"yaml/docker-compose.yaml", "YAML docker compose"},
                {"xml/config.xml", "XML config"},
                {"xml/pom.xml", "XML pom"},
                {"xml/settings.xml", "XML settings"},
                {"xml/web.xml", "XML web"},
                {"properties/database.properties", "Properties database"},
                {"properties/logging.properties", "Properties logging"},
                {"properties/app.properties", "Properties app"},
                {"dotenv/.env", "Dotenv .env"},
                {"dotenv/.env.production", "Dotenv .env.production"},
                {"dotenv/.env.test", "Dotenv .env.test"},
                {"hocon/app.hocon", "HOCON app"},
                {"hocon/akka.hocon", "HOCON akka"},
                {"hocon/config.hocon", "HOCON config"},
                {"kdl/app.kdl", "KDL app"},
                {"kdl/complex.kdl", "KDL complex"},
                {"kdl/config.kdl", "KDL config"},
        };

        Path out = Path.of("build/diagnostic_output.txt");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            for (String[] entry : files) {
                String path = "samples/" + entry[0];
                pw.println("=== " + entry[1] + " (" + path + ") ===");
                try {
                    ParseResult r = parser.parse(readLines(path));
                    pw.println("  HasErrors: " + r.hasErrors());
                    if (!r.diagnostics().isEmpty()) {
                        for (BlockDiagnostic d : r.diagnostics()) {
                            pw.println("  Diag: L" + d.startLine() + "-" + d.endLine()
                                    + " [" + d.level() + "] " + d.message());
                        }
                    }
                    if (!r.blocks().isEmpty()) {
                        pw.println("  DetectedFormat: " + r.blocks().get(0).detectedFormat());
                    }
                    Map<String, Object> f = r.flattened();
                    pw.println("  Flattened keys: " + f.keySet().size());
                    for (Map.Entry<String, Object> e : f.entrySet()) {
                        Object v = e.getValue();
                        String type = v == null ? "NULL" : v.getClass().getSimpleName();
                        pw.println("    " + e.getKey() + " = " + v + " [" + type + "]");
                    }
                } catch (Exception e) {
                    pw.println("  ERROR: " + e.getMessage());
                    e.printStackTrace(pw);
                }
                pw.println();
            }
        }
        System.err.println("Diagnostic written to " + out.toAbsolutePath());
    }

    private static List<String> readLines(String resourcePath) {
        try (var is = ResourceDiagnostic.class.getClassLoader().getResourceAsStream(resourcePath);
             var r = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
            return r.lines().toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
