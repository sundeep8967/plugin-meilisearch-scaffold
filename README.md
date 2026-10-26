<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png" alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

# Kestra Meilisearch Plugin

## Why

- Data engineering and AI pipelines frequently need to ingest, index, and query data in [Meilisearch](https://www.meilisearch.com/), the open-source, lightning-fast search engine.
- Without a native plugin, workflows must wrap the Meilisearch REST API with custom HTTP tasks or Python scripts, requiring boilerplate for authentication, batching, and error handling.
- This plugin brings native Meilisearch integration into Kestra workflows: create indexes, batch-index documents from inline definitions or internal storage (`Data<T>`), execute typo-tolerant searches with `FetchType` (`FETCH`, `FETCH_ONE`, `STORE`), and delete documents or indexes.

## What

Provides tasks under `io.kestra.plugin.meilisearch`:

- `CreateIndex` — creates a new search index with an optional primary key and synchronous task waiting.
- `AddDocuments` — batch-indexes documents into Meilisearch from an inline list or Kestra internal storage files.
- `Search` — executes fast, typo-tolerant full-text and faceted search with sorting and field projection.
- `DeleteDocuments` — deletes documents by a list of primary key IDs or by a filter expression.
- `DropIndex` — permanently deletes an index and all associated documents.

## Example Flow

```yaml
id: meilisearch_indexing_and_search
namespace: company.search

tasks:
  - id: create_index
    type: io.kestra.plugin.meilisearch.CreateIndex
    host: http://localhost:7700
    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
    index: movies
    primaryKey: id

  - id: add_documents
    type: io.kestra.plugin.meilisearch.AddDocuments
    host: http://localhost:7700
    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
    index: movies
    documents:
      - id: 1
        title: "Interstellar"
        genre: "Sci-Fi"
      - id: 2
        title: "Inception"
        genre: "Sci-Fi"
      - id: 3
        title: "The Dark Knight"
        genre: "Action"

  - id: search_movies
    type: io.kestra.plugin.meilisearch.Search
    host: http://localhost:7700
    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
    index: movies
    q: "interstelar"
    filter: "genre = 'Sci-Fi'"
    limit: 5
    fetchType: FETCH

  - id: log_matches
    type: io.kestra.plugin.core.log.Log
    message: "Found {{ outputs.search_movies.size }} movies"
```

## Running the tests

Tests use [Testcontainers](https://testcontainers.com/) against the official `getmeili/meilisearch:v1.11.0` image:

```bash
./gradlew test
```

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)

## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)
