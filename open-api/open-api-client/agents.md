# Open-API Client Sub-module

## Purpose
Generated Apache HttpComponents Client 5 stubs for calling the Pack REST API. Provides a type-safe Java client that serialises `PackRequest` and deserialises `PackResponse` over HTTP.

## Key Package
`com.github.skjolber.packing.openapi.client.api` — `PackApi` client class

## Architecture Notes
- **All source files are generated** from `../3d-api.yaml`. Do not hand-edit.
- Built on **Apache HttpComponents Client 5** (supports async and connection pooling).
- Depends on **open-api-model** for request/response types.
- Integration tests use **open-api-server** stubs (test scope) to run a local server.

## Usage
```java
ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8080");
PackApi api = new PackApi(client);
PackResponse response = api.pack(request);
```

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | open-api-model |
| compile | httpclient5, jackson-databind |
| test    | open-api-server, truth |

## Regenerating
```bash
mvn generate-sources -pl open-api/open-api-client
```
