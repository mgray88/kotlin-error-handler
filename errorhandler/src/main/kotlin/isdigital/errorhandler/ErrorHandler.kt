/*
 * The MIT License
 *
 * Copyright (c) 2013-2016 Workable SA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package isdigital.errorhandler

import java.util.HashMap
import kotlin.reflect.KClass

/**
 * An ErrorHandler is responsible for handling an error by executing one or more actions,
 * instances of [Action], that are found to match the error.
 *
 * @author Stratos Pavlakis - pavlakis@workable.com
 * @author Pavlos-Petros Tournaris - tournaris@workable.com
 * @author Vasilis Charalampakis - basilis@workable.com
 */
class ErrorHandler private constructor() {
    private val errorCodeMap = mutableMapOf<ErrorCodeIdentifier<*>, MatcherFactory<*>>()
    private val actions = mutableListOf<ActionEntry>()
    private val otherwiseActions= mutableListOf<Action>()
    private val alwaysActions = mutableListOf<Action>()
    private var localContext: ThreadLocal<Context> = object : ThreadLocal<Context>() {
        override fun initialValue(): Context {
            return Context()
        }
    }
    private var parentErrorHandler: ErrorHandler? = null

    /**
     * Create a new ErrorHandler with the given one as parent.
     *
     * @param parentErrorHandler the parent @{link ErrorHandler}
     */
    private constructor(parentErrorHandler: ErrorHandler?) : this() {
        this.parentErrorHandler = parentErrorHandler
    }

    /**
     * Register `action` to be executed by [.handle],
     * if the thrown error matches the `matcher`.
     *
     * @param matcher a matcher to match the thrown error
     * @param action  the associated action
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun on(
        matcher: Matcher,
        action: Action
    ): ErrorHandler {
        actions.add(ActionEntry.from(matcher, action))
        return this
    }

    /**
     * Register `action` to be executed by [.handle],
     * if the thrown error is an instance of `exceptionClass`.
     *
     * @param exceptionClass the class of the error
     * @param action         the associated action
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun on(
        exceptionClass: KClass<out Throwable>,
        action: Action
    ): ErrorHandler {
        actions.add(ActionEntry.from(ExceptionMatcher(exceptionClass), action))
        return this
    }

    /**
     * Register `action` to be executed by [.handle],
     * if the thrown error is bound (associated) to `errorCode`.
     *
     *
     * See [.bindClass] and [.bind]
     * on how to associate arbitrary error codes with actual Throwables via [Matcher].
     *
     * @param <T> the error code type
     * @param errorCode the error code
     * @param action    the associated action
     * @return the current `ErrorHandler` instance - to use in command chains
    </T> */
    fun <T : Any> on(
        errorCode: T,
        action: Action
    ): ErrorHandler {
        val matcherFactory: MatcherFactory<T> =
            getMatcherFactoryForErrorCode(errorCode)
                ?: throw UnknownErrorCodeException(errorCode)
        actions.add(ActionEntry.Companion.from(matcherFactory.build(errorCode), action))
        return this
    }

    /**
     * Register `action` to be executed in case no other *conditional*
     * action gets executed.
     *
     * @param action the action
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun otherwise(action: Action): ErrorHandler {
        otherwiseActions.add(action)
        return this
    }

    /**
     * Register `action` to be executed on all errors.
     *
     * @param action the action
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun always(action: Action): ErrorHandler {
        alwaysActions.add(action)
        return this
    }

    /**
     * Skip all following actions registered via an `on` method
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun skipFollowing(): ErrorHandler {
        localContext.get().skipFollowing = true
        return this
    }

    /**
     * Skip all actions registered via [.always]
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun skipAlways(): ErrorHandler {
        localContext.get().skipAlways = true
        return this
    }

    /**
     * Skip the default matching actions if any
     * @return the current `ErrorHandler` instance - to use in command chains
     */
    fun skipDefaults(): ErrorHandler {
        localContext.get().skipDefaults = true
        return this
    }

    private fun handle(
        error: Throwable,
        context: ThreadLocal<Context>
    ) {
        localContext = context
        val ctx = localContext.get()
        for (actionEntry in actions) {
            if (ctx.skipFollowing) break
            if (actionEntry.matcher.matches(error)) {
                actionEntry.action(error, this)
                ctx.handled = true
            }
        }
        if (!ctx.handled && otherwiseActions.isNotEmpty()) {
            for (action in otherwiseActions) {
                action(error, this)
                ctx.handled = true
            }
        }
        if (!ctx.skipAlways) {
            for (action in alwaysActions) {
                action(error, this)
                ctx.handled = true
            }
        }
        if (!ctx.skipDefaults) {
            parentErrorHandler?.handle(error, localContext)
        }
    }

    /**
     * Run a custom code block and assign current ErrorHandler instance
     * to handle a possible exception throw in 'catch'.
     *
     * @param blockExecutor functional interface containing Exception prone code
     */
    fun run(blockExecutor: BlockExecutor) {
        try {
            blockExecutor.invoke()
        } catch (exception: Exception) {
            handle(exception, localContext)
        }
    }

    /**
     * Handle `error` by executing all matching actions.
     *
     * @param error the error as a [Throwable]
     */
    fun handle(error: Throwable) {
        this.handle(error, localContext)
    }

    /**
     * Bind an `errorCode` to a `Matcher`, using a `MatcherFactory`.
     *
     *
     *
     * For example, when we need to catch a network timeout it's better to just write "timeout"
     * instead of a train-wreck expression. So we need to bind this "timeout" error code to an actual
     * condition that will check the actual error when it occurs to see if its a network timeout or not.
     *
     *
     * <pre>
     * `ErrorHandler
     * .defaultErrorHandler()
     * .bind("timeout", errorCode -> throwable -> {
     * return (throwable instanceof SocketTimeoutException) && throwable.getMessage().contains("Read timed out");
     * });
     *
     * // ...
     *
     * ErrorHandler
     * .build()
     * .on("timeout", (throwable, handler) -> {
     * showOfflineScreen();
     * })
    ` *
    </pre> *
     *
     *
     * @param <T> the error code type
     * @param errorCode the errorCode value, can use a primitive for clarity and let it be autoboxed
     * @param matcherFactory a factory that given an error code, provides a matcher to match the error against it
     * @return the current `ErrorHandler` instance - to use in command chains
    </T> */
    fun <T : Any> bind(
        errorCode: T,
        matcherFactory: MatcherFactory<T>
    ): ErrorHandler {
        errorCodeMap[ErrorCodeIdentifier(errorCode)] = matcherFactory
        return this
    }

    /**
     * Bind an `errorCode` `Class` to a `Matcher`, using a `MatcherFactory`.
     *
     *
     *
     * For example, when we prefer using plain integers to refer to HTTP errors instead of
     * checking the HTTPException status code every time.
     *
     *
     * <pre>
     * `ErrorHandler
     * .defaultErrorHandler()
     * .bindClass(Integer.class, errorCode -> throwable -> {
     * return throwable instanceof HTTPException && ((HTTPException)throwable).getStatusCode() == errorCode;
     * });
     *
     * // ...
     *
     * ErrorHandler
     * .build()
     * .on(404, (throwable, handler) -> {
     * showResourceNotFoundError();
     * })
     * .on(500, (throwable, handler) -> {
     * showServerError();
     * })
    ` *
    </pre> *
     *
     * @param <T> the error code type
     * @param errorCodeClass the errorCode class
     * @param matcherFactory a factory that given an error code, provides a matcher to match the error against it
     * @return the current `ErrorHandler` instance - to use in command chains
    </T> */
    fun <T : Any> bindClass(
        errorCodeClass: KClass<T>,
        matcherFactory: MatcherFactory<T>
    ): ErrorHandler {
        errorCodeMap[ErrorCodeIdentifier(errorCodeClass)] = matcherFactory
        return this
    }

    protected fun <T : Any> getMatcherFactoryForErrorCode(errorCode: T): MatcherFactory<T>? {
        var matcherFactory: MatcherFactory<T>?
        matcherFactory = errorCodeMap[ErrorCodeIdentifier(errorCode)] as? MatcherFactory<T>
        if (matcherFactory != null) {
            return matcherFactory
        }

        matcherFactory = errorCodeMap[ErrorCodeIdentifier(errorCode::class)] as? MatcherFactory<T>
        if (matcherFactory != null) {
            return matcherFactory
        }
        return if (parentErrorHandler != null) {
            parentErrorHandler?.getMatcherFactoryForErrorCode(errorCode)
        } else null
    }

    /**
     * Clear ErrorHandler instance from all its registered Actions and Matchers.
     */
    fun clear() {
        actions.clear()
        errorCodeMap.clear()
        otherwiseActions.clear()
        alwaysActions.clear()
        localContext.get().clear()
    }

    private class Context {
        private val keys = HashMap<String, Any>()
        var handled = false
        var skipDefaults = false
        var skipFollowing = false
        var skipAlways = false
        operator fun get(key: Any?): Any? {
            return keys[key]
        }

        fun put(key: String, value: Any): Any? {
            return keys.put(key, value)
        }

        fun remove(key: Any?): Any? {
            return keys.remove(key)
        }

        fun clear() {
            keys.clear()
            skipDefaults = false
            skipFollowing = false
            skipAlways = false
        }
    }

    /**
     * Used to identify an error code either by its "literal" value
     * or by its Class.
     *
     *
     * When using custom objects as error codes,
     * make sure you implement [Object.equals] to allow ErrorHandler
     * perform equality comparisons between instances.
     */
    private class ErrorCodeIdentifier<out T : Any> {
        private val errorCode: T?
        private val errorCodeClass: KClass<out T>?

        internal constructor(errorCode: T) {
            this.errorCode = errorCode
            this.errorCodeClass = null
        }

        internal constructor(errorCodeClass: KClass<out T>) {
            this.errorCode = null
            this.errorCodeClass = errorCodeClass
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val that = other as ErrorCodeIdentifier<*>

            if (if (errorCode != null) errorCode != that.errorCode else that.errorCode != null) return false
            return if (errorCodeClass != null) errorCodeClass == that.errorCodeClass else that.errorCodeClass == null
        }

        override fun hashCode(): Int {
            var result = errorCode?.hashCode() ?: 0
            result = 31 * result + (errorCodeClass?.hashCode() ?: 0)
            return result
        }
    }

    companion object {
        private var defaultInstance: ErrorHandler? = null

        /**
         * Create a new @{link ErrorHandler}, isolated from the default one.
         *
         *
         * In other words, designed to handle all errors by itself without delegating
         * to the default error handler.
         *
         * @return returns a new `ErrorHandler` instance
         */
        @JvmStatic
        fun createIsolated(): ErrorHandler {
            return ErrorHandler()
        }

        /**
         * Create a new @{link ErrorHandler}, that delegates to the default one.
         *
         *
         * Any default actions, are always executed after the ones registered on this one.
         *
         * @return returns a new `ErrorHandler` instance
         */
        @JvmStatic
        fun create(): ErrorHandler {
            return ErrorHandler(defaultErrorHandler())
        }

        /**
         * Get the default @{link ErrorHandler}, a singleton object
         * to which all other instances by default delegate to.
         *
         * @return the default @{link ErrorHandler} instance
         */
        @JvmStatic
        @Synchronized
        fun defaultErrorHandler(): ErrorHandler {
            if (defaultInstance == null) {
                defaultInstance =
                    ErrorHandler()
            }
            return defaultInstance!!
        }
    }
}
