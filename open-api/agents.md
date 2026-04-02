# Open-API Parent Module

## Purpose
Parent POM aggregating all REST API modules. Defines shared dependency management and build configuration for the OpenAPI-generated code. The actual Pack API is a single POST `/pack` endpoint.

## Sub-modules
| Directory | Artifact | Role |
|-----------|----------|------|
| `open-api-model` | open-api-model | Generated Jackson data model |
| `open-api-server` | open-api-server | Generated Spring server interfaces |
| `open-api-client` | open-api-client | Generated Apache HttpClient 5 client |
| `open-api-test` | open-api-test | Shared test utilities for the API |

## API Specification
The canonical source of truth is `open-api/3d-api.yaml` (OpenAPI 3.0). All model, server, and client code is **generated** — edit the spec, then regenerate; do not hand-edit generated sources.

## Regenerating Code
```bash
mvn generate-sources -pl open-api/open-api-model,open-api/open-api-server,open-api/open-api-client
```

## Build
```bash
mvn install -pl open-api -am
```
