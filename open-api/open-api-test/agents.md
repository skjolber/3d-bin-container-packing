# Open-API Test Sub-module

## Purpose
Shared test utilities for the open-api modules. Provides common helpers, fixtures, and Mockito-based mocks reusable across `open-api-server` and `open-api-client` tests.

## Architecture Notes
- Consumed as a `test`-scoped dependency by the sibling open-api modules.
- Currently minimal; grow this module to avoid duplication when adding integration tests across server/client.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | open-api-model, open-api-server |
| compile | mockito-core |

## Build
```bash
mvn test -pl open-api/open-api-test -am
```
