# Open-API Model Sub-module

## Purpose
Generated Jackson data model for the Pack REST API. Contains all request/response POJOs produced by the OpenAPI Generator Maven plugin from `../3d-api.yaml`.

## Key Package
`com.github.skjolber.packing.openapi.model` — `PackRequest`, `PackResponse`, and related value types

## Architecture Notes
- **All source files are generated.** Do not edit them by hand; modify `3d-api.yaml` and re-run `mvn generate-sources`.
- Uses Jackson annotations for JSON serialisation and Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) for bean validation.
- Swagger/OpenAPI 3 annotations (`@Schema`) are retained in generated code for documentation.

## Dependencies
| Scope   | Artifact |
|---------|----------|
| compile | jackson-databind, jackson-annotations |
| compile | jakarta.validation-api |
| compile | swagger-annotations |

## Regenerating
```bash
mvn generate-sources -pl open-api/open-api-model
```
