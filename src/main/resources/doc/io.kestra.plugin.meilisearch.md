# Meilisearch Plugin

Use the Meilisearch plugin to manage indexes, add and update documents, run full-text and vector search, and delete documents in [Meilisearch](https://www.meilisearch.com/), an open-source, ultra-fast search engine, or [Meilisearch Cloud](https://cloud.meilisearch.com/).

## Connection

All tasks inherit common connection properties:
- `host`: The host URL of the Meilisearch server or Cloud instance, e.g. `http://localhost:7700` (required).
- `apiKey`: The API key or master key for authentication (optional, secret).
- `clientTimeout`: Request timeout in seconds (optional).

## Tasks

- `CreateIndex`: Creates a new index with an optional custom primary key (`primaryKey`). Supports waiting for the asynchronous indexing task to complete (`waitForTask`).
- `AddDocuments`: Inserts or updates documents in batch. Documents can be provided as an inline list or read from a Kestra internal storage URI (`Data<T>`).
- `Search`: Performs fast, typo-tolerant full-text and faceted search (`q`, `filter`, `limit`, `offset`, `sort`). Outputs results directly in execution outputs (`FETCH`), as a single record (`FETCH_ONE`), or to Kestra internal storage (`STORE`).
- `DeleteDocuments`: Deletes documents by primary key IDs (`ids`) or by a filter expression (`filter`). Exactly one of the two must be set.
- `DropIndex`: Permanently deletes an index and all associated documents.
