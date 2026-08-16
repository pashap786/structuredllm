# StructuredLLM

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://oracle.com/java)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**StructuredLLM** is a zero-dependency Java 17+ library designed to reliably extract, parse, and validate JSON outputs from Large Language Models (LLMs) into strongly-typed Java `record` instances. Created by Pasha Utt.

Large Language Models frequently introduce conversational fluff, markdown backticks (```json), or missing properties in their responses. **StructuredLLM** isolates clean JSON payloads, enforces runtime structural integrity on Java records, and provides a self-healing retry loop that feeds validation error messages back into subsequent LLM prompts automatically.

---

## Key Features

* **Zero External Dependencies:** Built with pure Java 17 primitives—no transitive libraries, reflection framework bloat, or runtime weight.
* **Markdown & Fluff Extraction:** Isolates valid JSON structures from markdown code fences and conversational preambles/postambles.
* **Java 17+ Record Validation:** Verifies non-null requirements across target record properties via component accessors.
* **Self-Healing Retry Loops:** Features a functional `RetryStrategy` engine that captures failed validation rules and appends system feedback into the LLM prompt for automatic correction attempts.
* **Framework Agnostic:** Compatible with any Java HTTP client or LLM SDK (OpenAI, Anthropic, Ollama, LangChain4j, etc.).

---

## Installation

### Maven

Add JitPack to your `pom.xml` repositories:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>