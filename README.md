# ErrorHandler
 [![Download](https://api.bintray.com/packages/is-digital/kotlin-error-handler/kotlin-error-handler/images/download.svg)](https://bintray.com/is-digital/kotlin-error-handler/kotlin-error-handler/_latestVersion)

> Error handling library for Android and Kotlin

Encapsulate error handling logic into objects that adhere to configurable defaults. Then pass them around as parameters or inject them via DI. 

## Download
Download the [latest JAR](https://bintray.com/is-digital/kotlin-error-handler/kotlin-error-handler/_latestVersion) or grab via Maven:
```xml
<dependency>
  <groupId>isdigital.errorhandler</groupId>
  <artifactId>kotlin-error-handler</artifactId>
  <version>1.0.1</version>
  <type>pom</type>
</dependency>
```

or Gradle:

```groovy
implementation 'isdigital.errorhandler:kotlin-error-handler:1.0.1'
```


## Usage

Let's say we're building a messaging Android app that uses both the network and a local database.

We need to:

### Setup a default ErrorHandler once

 - Configure the default ErrorHandler
 - Alias errors to codes that are easier to use like Integer, String and Enum values
 - Map errors to actions to take when those errors occur (exceptions thrown)

```kotlin
// somewhere inside MessagingApp.kt

ErrorHandler
  .defaultErrorHandler()

  // Bind certain exceptions to "offline"
  .bind("offline") { errorCode -> 
    { throwable ->
      return throwable is UnknownHostException || throwable is ConnectException
    }
  }

  // Bind HTTP 404 status to 404
  .bind(404) { errorCode -> 
    { throwable ->
      return throwable is HttpException && throwable.code() == 404
    }
  }

  // Bind HTTP 500 status to 500
  .bind(500) { errorCode -> 
    { throwable ->
      return throwable is HttpException && throwable.code() == 500
    }
  }

  // Bind all DB errors to a custom enumeration
  .bindClass(DBError::class) { errorCode -> 
    { throwable ->
      return DBError.from(throwable) == errorCode
    }
  }

  // Handle HTTP 500 errors
  .on(500) { throwable, errorHandler ->
    displayAlert("Kaboom!")
  }

  // Handle HTTP 404 errors
  .on(404) { throwable, errorHandler ->
    displayAlert("Not found!")
  }

  // Handle "offline" errors
  .on("offline") { throwable, errorHandler ->
    displayAlert("Network dead!")
  }

  // Handle unknown errors
  .otherwise { throwable, errorHandler ->
    displayAlert("Oooops?!")
  }

  // Always log to a crash/error reporting service
  .always { throwable, errorHandler ->
    Logger.log(throwable)
  }
```

### Use ErrorHandler inside catch blocks

```kotlin
// ErrorHandler instances created using ErrorHandler.create(), delegate to the default ErrorHandler
// So it's actually a "handle the error using only defaults"
// i.e. somewhere inside MessageListActivity.kt
try {
  fetchNewMessages()
} catch (ex: Exception) {
  ErrorHandler.create().handle(ex)
}
```

### Run blocks of code using ErrorHandler.runHandling

```kotlin
errorHandler.runHandling { fetchNewMessages() }
```

### Override defaults when needed

```kotlin
// Configure a new ErrorHandler instance that delegates to the default one, for a specific method call
// i.e. somewhere inside MessageListActivity.kt
try {
  fetchNewMessages()
} catch (ex: Exception) {
  ErrorHandler
    .create()
    .on(StaleDataException::class) { throwable, errorHandler ->
        reloadList()
        errorHandler.skipDefaults()
    }
    .on(404) { throwable, errorHandler ->
        // We handle 404 specifically on this screen by overriding the default action
        displayAlert("Could not load new messages")
        errorHandler.skipDefaults()
    }
    .on(DBError.READ_ONLY) { throwable, errorHandler ->
        // We could not open our database to write the new messages
        ScheduledJob.saveMessages(someMessages).execute()
        // We also don't want to log this error because ...
        errorHandler.skipAlways()
    }
    .handle(ex)
}
```

### Extension functions provide type safety and ease of initialization

```kotlin
// Initialize a local `ErrorHandler` lazily and add actions inline to keep logic in one location
// Inherits defaults from `defaultErrorHandler` or the provided optional parent `ErrorHandler`
private val errorHandler by errorHandler(optionalParent) {
  on(404) { throwable, errorHandler ->
    displayAlert("Not found!")
  }
  on("offline") { throwable, errorHandler ->
    displayAlert("Network dead!")
  }


  // Add typed actions on specific exceptions with an extension function
  on<StaleDataException> { exception, errorHandler ->
    // exception is of type StaleDataException
  } 
}
```

### Things to know

ErrorHandler is __thread-safe__.


## API

### Initialize

* `defaultErrorHandler()` Get the default ErrorHandler.

* `create()` Create a new ErrorHandler that is linked to the default one.

* `create(ErrorHandler)` Create a new ErrorHandler that uses the passed ErrorHandler as the default one.

* `createIsolated()` Create a new empty ErrorHandler that is not linked to the default one.

### Configure

* `on(Matcher, Action)` Register an _Action_ to be executed if _Matcher_ matches the error.

* `on(KClass<out Throwable>, Action)` Register an _Action_ to be executed if error is an instance of `Throwable`.

* `on(T, Action)` Register an _Action_ to be executed if error is bound to T, through `bind()` or `bindClass()`.

* `otherwise(Action)` Register an _Action_ to be executed only if no other _Action_ gets executed.

* `always(Action)` Register an _Action_ to be executed always and after all other actions. Works like a `finally` clause.

* `skipFollowing()`  Skip the execution of any subsequent _Actions_ except those registered via `always()`.

* `skipAlways()` Skip all _Actions_ registered via `always()`.

* `skipDefaults()` Skip any default actions. Meaning any actions registered on the `defaultErrorHandler` instance.

* `bind(T, MatcherFactory<T>)` Bind instances of _T_ to match errors through a matcher provided by _MatcherFactory_.

* `bindClass(KClass<T>, MatcherFactory<T>)` Bind class _T_ to match errors through a matcher provided by _MatcherFactory_.

* `clear()` Clear all registered _Actions_.

### Convenience Extensions

* `errorHandler(ErrorHandler?, (ErrorHandler.() -> Unit)?)` Lazy initializer keeps error handling logic in one place.

* `on<T : Exception>((T, ErrorHandler) -> Unit)` Register a typed _Action_ to be executed if error is an instance of `T`.

### Execute

* `handle(Throwable)` Handle the given error.


## About

When designing for errors, we usually need to:

1. have a **default** handler for every **expected** error 
   // i.e. network, subscription errors
2. handle **specific** errors **as appropriate** based on where and when they occur 
   // i.e. network error while uploading a file, invalid login
3. have a **catch-all** handler for **unknown** errors 
   // i.e. system libraries runtime errors we don't anticipate
4. keep our code **DRY**

## License
```
The MIT License

Copyright (c) 2013-2016 Workable SA
Copyright (c) 2020 Michael Gray

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
