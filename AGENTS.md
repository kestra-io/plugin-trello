# Kestra Trello Plugin

## What

- Provides plugin components under `io.kestra.plugin.trello`.
- Includes classes such as `Comment`, `Create`, `Trigger`, `Update`.

## Why

- This plugin integrates Kestra with Trello Cards.
- It provides tasks and triggers for managing Trello cards - create, update, move, comment, and monitor card changes.

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

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
