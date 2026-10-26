# TeaQL Spring MVC view adapter

This optional artifact lets a Spring MVC controller render the same `ViewPreparationService` result used by a framework-free CLI. The host creates a trusted `UserContext`, completes the business transaction, and calls `SpringMvcViewAdapter.render(context, result, options)`. For an already prepared view, `renderPrepared` rechecks current authorization without reissuing document credentials or Blob URLs.

The adapter encodes the prepared copy to JSON bytes before returning `ResponseEntity<byte[]>`, sets `Content-Type: application/json` and the view's current private cache policy, and never passes a raw TeaQL entity or `PreparedView` to Spring serialization. Governed document credentials are included under `documentCredentials` beside `data`. Preparation or encoding failures propagate a redacted `ViewPreparationException`; its `businessCommitted()` flag tells the caller when a committed command must be read again rather than replayed.

Spring Web is a provided dependency. The core and runtime modules have no Spring dependency; an application opts in to this adapter and supplies its own controller and principal-to-`UserContext` mapping. The module has no automatic response advice or transaction lifecycle hook.
