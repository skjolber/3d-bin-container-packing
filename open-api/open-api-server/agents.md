# Open-API Server Sub-module

## Purpose
Generated Spring server-side interface stubs for the Pack REST API. Provides `PackApi` (and related interfaces) ready to be implemented by a Spring Boot `@RestController`.

## Key Package
`com.github.skjolber.packing.openapi.server.api` — `PackApi` interface with Spring `@RequestMapping` annotations

## Architecture Notes
- **All source files are generated** from `../3d-api.yaml`. Do not hand-edit.
- Generated in **interface-only** mode — no implementation is provided. Wire a `@RestController` that implements `PackApi`.
- Compatible with **Spring Boot 3** (Spring 7.x, Jakarta namespaces).
- Depends on **open-api-model** for request/response types.

## Implementing the API
```java
@RestController
public class PackController implements PackApi {
    @Override
    public ResponseEntity<PackResponse> pack(PackRequest request) {
        // delegate to a core Packager
    }
}
```

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | open-api-model |
| compile | spring-web, spring-context |
| compile | jakarta.annotation-api, jakarta.validation-api |

## Regenerating
```bash
mvn generate-sources -pl open-api/open-api-server
```
