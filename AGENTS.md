# Kestra Trello Plugin

## What

- Provides plugin components under `io.kestra.plugin.trello`.
- Includes classes such as `Comment`, `Create`, `Trigger`, `Update`.

## Why

- What user problem does this solve? Teams need to integration plugin for Atlassian Trello to manage boards, lists, cards, and workflow automation from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Atlassian Trello steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Atlassian Trello.

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
