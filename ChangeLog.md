# ChangeLog

## 1.0.0

### New

 - Forked from [Workable/java-error-handler](https://github.com/Workable/java-error-handler)
 - Converted to idiomatic Kotlin
 - `run` has been renamed to `runHandling`, and accepts a lambda
 - `Action` has been changed to a typealiased lambda
 - Added compatibility functions that take `Matcher` and `MatcherFactory` to accept lambdas
 - `lazy` delegate function `errorHandler(ErrorHandler?, (ErrorHandler.() -> Unit)?)` added
   - Lazily creates a new `ErrorHandler` which defaults to the passed `ErrorHandler`, otherwise `defaultErrorHandler`
   - Lambda parameter allows adding error handling at initialization point
