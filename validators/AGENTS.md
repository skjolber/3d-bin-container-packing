# Validators Module

## Purpose

Validates packing results, manifests, placements, load constraints, and stack stability independently of the packing algorithms.

## Key Packages

- `com.github.skjolber.packing.validator` — Result, manifest, and placement validation
- `com.github.skjolber.packing.validator.load` — Load weight, pressure, count, and identical-box validation
- `com.github.skjolber.packing.validator.stability` — Support-area and center-of-gravity validation
- `com.github.skjolber.packing.validator.*.reasons` — Structured validation failure reasons

## Dependencies

Production code depends only on the `api` module. Keep algorithm-specific integration tests in the algorithm-owning module to avoid circular module dependencies.
