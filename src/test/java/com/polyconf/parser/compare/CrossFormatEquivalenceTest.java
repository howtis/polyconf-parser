package com.polyconf.parser.compare;

import com.polyconf.parser.PolyconfParser;
import com.polyconf.parser.merge.MergePolicy;
import com.polyconf.parser.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrossFormatEquivalenceTest {

    private final PolyconfParser parser = new PolyconfParser();

    // --- Single block: same logical config expressed in different formats ---

    @Test
    void dbConfigTomlAndYamlEqual() {
        Map<String, Object> toml = parser.parse("# @fmt:toml\n[server]\nhost = \"localhost\"\nport = 5432\nenabled = true\n").flattened();
        Map<String, Object> yaml = parser.parse("# @fmt:yaml\nserver:\n  host: localhost\n  port: 5432\n  enabled: true\n").flattened();

        assertEquals(toml.size(), yaml.size());
        toml.forEach((key, value) ->
                assertEquals(String.valueOf(value), String.valueOf(yaml.get(key)), "Mismatch on key: " + key));
    }

    @Test
    void dbConfigTomlAndJsonEqual() {
        Map<String, Object> toml = parser.parse("# @fmt:toml\n[server]\nhost = \"localhost\"\nport = 5432\nenabled = true\n").flattened();
        Map<String, Object> json = parser.parse("# @fmt:json\n{\"server\": {\"host\": \"localhost\", \"port\": 5432, \"enabled\": true}}\n").flattened();

        assertEquals(toml.size(), json.size());
        toml.forEach((key, value) ->
                assertEquals(String.valueOf(value), String.valueOf(json.get(key)), "Mismatch on key: " + key));
    }

    @Test
    void dbConfigTomlAndPropertiesEqual() {
        Map<String, Object> toml = parser.parse("# @fmt:toml\n[server]\nhost = \"localhost\"\nport = 5432\nenabled = true\n").flattened();
        Map<String, Object> props = parser.parse("# @fmt:properties\nserver.host=localhost\nserver.port=5432\nserver.enabled=true\n").flattened();

        assertEquals(toml.size(), props.size());
        // After shared type inference, Properties produces same types as TOML
        toml.forEach((key, value) ->
                assertEquals(value, props.get(key), "Mismatch on key: " + key));
    }

    @Test
    void dbConfigTomlAndIniEqual() {
        Map<String, Object> toml = parser.parse("# @fmt:toml\n[server]\nhost = \"localhost\"\nport = 5432\nenabled = true\n").flattened();
        Map<String, Object> ini = parser.parse("# @fmt:ini\n[server]\nhost=localhost\nport=5432\nenabled=true\n").flattened();

        assertEquals(toml.size(), ini.size());
        // After shared type inference, INI produces same types as TOML
        toml.forEach((key, value) ->
                assertEquals(value, ini.get(key), "Mismatch on key: " + key));
    }

    // --- Multi-block: mixed formats in same input ---

    @Test
    void mixedBlocksYieldSameData() {
        String content = "# @fmt:toml\n[app]\nname = \"polyconf\"\n\n# @fmt:yaml\napp:\n  version: 1.0\n";

        ParseResult result = parser.parse(content, MergePolicy.DEFAULT);

        assertEquals("polyconf", result.flattened().get("app.name"));
        assertEquals(1.0, result.flattened().get("app.version"));
    }

    // --- Flattened consistency across structured formats (type-aware) ---

    @Test
    void allFormatsProduceIdenticalStringValues() {
        String toml = "# @fmt:toml\n[database]\nhost = \"localhost\"\nport = 5432\nmax_pool = 10\n";
        String yaml = "# @fmt:yaml\ndatabase:\n  host: localhost\n  port: 5432\n  max_pool: 10\n";
        String json = "# @fmt:json\n{\"database\": {\"host\": \"localhost\", \"port\": 5432, \"max_pool\": 10}}\n";
        String props = "# @fmt:properties\ndatabase.host=localhost\ndatabase.port=5432\ndatabase.max_pool=10\n";
        String ini = "# @fmt:ini\n[database]\nhost=localhost\nport=5432\nmax_pool=10\n";

        Map<String, Object> tomlMap = parser.parse(toml).flattened();
        Map<String, Object> yamlMap = parser.parse(yaml).flattened();
        Map<String, Object> jsonMap = parser.parse(json).flattened();
        Map<String, Object> propsMap = parser.parse(props).flattened();
        Map<String, Object> iniMap = parser.parse(ini).flattened();

        assertEquals(3, tomlMap.size());
        assertEquals(3, yamlMap.size());
        assertEquals(3, jsonMap.size());
        assertEquals(3, propsMap.size());
        assertEquals(3, iniMap.size());

        // String values match across all formats
        assertEquals(String.valueOf(tomlMap.get("database.host")), String.valueOf(yamlMap.get("database.host")));
        assertEquals(String.valueOf(tomlMap.get("database.host")), String.valueOf(jsonMap.get("database.host")));
        assertEquals(String.valueOf(tomlMap.get("database.host")), String.valueOf(propsMap.get("database.host")));
        assertEquals(String.valueOf(tomlMap.get("database.host")), String.valueOf(iniMap.get("database.host")));

        // Numeric values match across all formats - Properties and INI now infer Long via shared ValueInference
        assertEquals(tomlMap.get("database.port"), propsMap.get("database.port"));
        assertEquals(tomlMap.get("database.port"), iniMap.get("database.port"));
        assertEquals(tomlMap.get("database.port"), jsonMap.get("database.port"));
        assertEquals(String.valueOf(tomlMap.get("database.port")), String.valueOf(yamlMap.get("database.port")));

        assertEquals(tomlMap.get("database.max_pool"), propsMap.get("database.max_pool"));
        assertEquals(tomlMap.get("database.max_pool"), iniMap.get("database.max_pool"));
        assertEquals(tomlMap.get("database.max_pool"), jsonMap.get("database.max_pool"));
        assertEquals(String.valueOf(tomlMap.get("database.max_pool")), String.valueOf(yamlMap.get("database.max_pool")));

        // Verify Properties and INI now produce numeric types, not strings
        assertInstanceOf(Long.class, propsMap.get("database.port"));
        assertInstanceOf(Long.class, iniMap.get("database.port"));
        assertInstanceOf(Long.class, propsMap.get("database.max_pool"));
        assertInstanceOf(Long.class, iniMap.get("database.max_pool"));
    }
}
