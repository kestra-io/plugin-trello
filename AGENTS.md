# Kestra Trello Plugin

## What

description = 'Atlassian Trello plugin for Kestra Exposes 5 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with Atlassian Trello, allowing orchestration of Atlassian Trello-based operations as part of data pipelines and automation workflows.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `trello`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.trello.cards.Comment`
- `io.kestra.plugin.trello.cards.Create`
- `io.kestra.plugin.trello.cards.Move`
- `io.kestra.plugin.trello.cards.Trigger`
- `io.kestra.plugin.trello.cards.Update`

### Project Structure

```
plugin-trello/
├── src/main/java/io/kestra/plugin/trello/cards/
├── src/test/java/io/kestra/plugin/trello/cards/
├── build.gradle
└── README.md
```

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
