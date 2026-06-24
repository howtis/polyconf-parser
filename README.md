# polyconf-parser

**⚠️ This project is under active development and not yet stable. APIs may change without notice.**

A Java library that parses **polyglot configuration files** — files containing multiple configuration formats mixed together in a single document.

### Supported Formats

TOML · YAML · JSON · JSON5 · Properties · INI · Dotenv · XML · HOCON

### How It Works

`polyconf-parser` segments input content into blocks, auto-detects each block's format, and parses them individually. Results are merged into a unified configuration model.

Two core constraints:
- **One format per line** — each line belongs to exactly one format; a line cannot contain tokens from multiple formats.
- **Contiguous blocks** — once a format begins, all successive lines belong to that format until the next format block starts. Blocks do not interleave.

- **Auto-detection** — each block's format is inferred from its content
- **Hints** — explicit `# @format TOML` markers to override detection
- **Lenient parsing** — extracts as much valid config as possible, with diagnostics for unrecognized sections
- **Merge policies** — configurable strategies for resolving overlapping keys across blocks

### Requirements

- Java 17+
- Gradle (wrapper included)

### Building

```bash
./gradlew build
```

### Status

This project is a **work in progress**. Core parsing and format detection are implemented, but the public API, merge semantics, and test coverage are still evolving. Breaking changes are expected.
