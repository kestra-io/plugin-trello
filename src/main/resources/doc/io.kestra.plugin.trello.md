# How to use the Trello plugin

Manage cards on Trello boards from Kestra flows.

## Authentication

Set `apiKey` to your Trello API key and `apiToken` to your Trello API token. Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`cards.Create` creates a card — set `name` and `listId` (both required). Optionally set `desc`, `pos`, and `due`. The output includes the new `cardId`.

`cards.Update` updates a card by `cardId` — set any of `name`, `desc`, `closed` (archive/reopen), `due`, or `pos`.

`cards.Move` moves a card to a new `listId` by `cardId`.

`cards.Comment` adds a comment to a card — set `cardId` and `text`. The output includes the new `commentId`.

`cards.Trigger` polls one or more Trello lists on a schedule (default 5 minutes) and starts one execution per batch of new or changed cards. Set `lists` to a list of list IDs, or `boardId` to watch an entire board.
