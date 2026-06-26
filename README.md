# polyconf-parser

**⚠️ This project is under active development and not yet stable. APIs may change without notice.**

A Java library that parses **polyglot configuration files** — files containing multiple configuration formats mixed together in a single document.

### Supported Formats

TOML · YAML · JSON · JSON5 · KDL · Properties · INI · Dotenv · XML · HOCON

### How It Works

`polyconf-parser` splits input into segments, auto-detects each segment's format via a two-pass classifier, then parses with the appropriate lenient parser.

#### Core Principles

- **Best-effort over perfection** — returns partial results with diagnostics rather than failing
- **Lenient parsing** — never throws on invalid input (except null); unrecognized content becomes diagnostics
- **Diagnostic transparency** — every block result includes format, confidence, and any issues found
- **Confidence model** — hinted blocks = 1.0, classified = 1.0, trial-and-error = 0.5, fallback = 0.0

#### Constraints

- **One format per segment** — each contiguous block belongs to a single format; format boundaries are detected automatically
- **Contiguous blocks** — formats do not interleave within a segment

### Requirements

- Java 17+

### Building & Testing

```bash
./gradlew build     # compile, run tests, generate JaCoCo coverage report
./gradlew test      # run tests only
```

### Status

The public API is stabilizing but breaking changes are still possible.
