package isdigital.errorhandler

import isdigital.errorhandler.ErrorHandler.Companion.create
import isdigital.errorhandler.ErrorHandler.Companion.createIsolated
import isdigital.errorhandler.ErrorHandler.Companion.defaultErrorHandler
import junit.framework.TestCase
import org.junit.Test
import org.mockito.Mockito

/**
 * [ErrorHandler] unit tests
 *
 * @author Stratos Pavlakis
 */
class ErrorHandlerTest : TestCase() {
    internal interface ActionDelegate {
        fun action1()
        fun action2()
        fun action3()
        fun action4()
        fun action5()
        fun otherwise1()
        fun always1()
        fun defaultAction1()
        fun defaultAction2()
        fun defaultAction3()
        fun defaultOtherwise()
        fun defaultAlways()
    }

    private var actionDelegateMock: ActionDelegate? = null
    override fun setUp() {
        actionDelegateMock = Mockito.mock(
            ActionDelegate::class.java
        )
        defaultErrorHandler()
            .bind("closed:bar") {
                { throwable ->
                    if (throwable is BarException) {
                        !throwable.isOpenBar
                    } else {
                        false
                    }
                }
            }
            .bindClass(Int::class) { errorCode ->
                { throwable ->
                    if (throwable is QuxException) {
                        throwable.errorStatus == errorCode
                    } else {
                        false
                    }
                }
            }
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.defaultAction1()
            }
            .on(500) { _, _ ->
                actionDelegateMock!!.defaultAction2()
            }
            .on("closed:bar") { _, _ ->
                actionDelegateMock!!.defaultAction3()
            }
            .otherwise { _, _ ->
                actionDelegateMock!!.defaultOtherwise()
            }
            .always { _, _ ->
                actionDelegateMock!!.defaultAlways()
            }
    }

    override fun tearDown() {
        defaultErrorHandler()
            .clear()
    }

    @Test
    fun testActionsExecutionOrder() {
        val errorHandler: ErrorHandler = create()
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .on(
                { throwable ->
                    try {
                        FooException::class.java.cast(throwable).isFatal
                    } catch (ignore: ClassCastException) {
                        false
                    }
                },
                { _, _ ->
                    actionDelegateMock!!.action2()
                }
            )
            .on("closed:bar") { _, _ ->
                actionDelegateMock!!.action3()
            }
            .on(400) { _, _ ->
                actionDelegateMock!!.action4()
            }
            .on(500) { _, _ ->
                actionDelegateMock!!.action5()
            }
            .otherwise { _, _ ->
                actionDelegateMock!!.otherwise1()
            }
            .always { _, _ ->
                actionDelegateMock!!.always1()
            }
        val testVerifier1 = Mockito.inOrder(actionDelegateMock)
        errorHandler.handle(FooException("test1"))
        testVerifier1.verify(
            actionDelegateMock
        )!!.action1()
        testVerifier1.verify(
            actionDelegateMock
        )!!.always1()
        testVerifier1.verify(
            actionDelegateMock
        )!!.defaultAction1()
        testVerifier1.verify(
            actionDelegateMock
        )!!.defaultAlways()
        testVerifier1.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
        Mockito.reset(actionDelegateMock)
        val testVerifier2 = Mockito.inOrder(actionDelegateMock)
        errorHandler.handle(BarException("What a shame", false))
        testVerifier2.verify(
            actionDelegateMock
        )!!.action3()
        testVerifier2.verify(
            actionDelegateMock
        )!!.always1()
        testVerifier2.verify(
            actionDelegateMock
        )!!.defaultAction3()
        testVerifier2.verify(
            actionDelegateMock
        )!!.defaultAlways()
        testVerifier2.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
        Mockito.reset(actionDelegateMock)
        val testVerifier3 = Mockito.inOrder(actionDelegateMock)
        errorHandler.handle(QuxException(500))
        testVerifier3.verify(
            actionDelegateMock
        )!!.action5()
        testVerifier3.verify(
            actionDelegateMock
        )!!.always1()
        testVerifier3.verify(
            actionDelegateMock
        )!!.defaultAction2()
        testVerifier3.verify(
            actionDelegateMock
        )!!.defaultAlways()
        testVerifier3.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testSkipDefaults() {
        create()
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .handle(FooException("foo error"))
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(1)
        )!!.action1()
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(1)
        )!!.defaultAction1()
        Mockito.reset(actionDelegateMock)
        create()
            .on(FooException::class) { _, handler ->
                actionDelegateMock!!.action1()
                handler.skipDefaults()
            }
            .handle(FooException("foo error"))
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(1)
        )!!.action1()
        Mockito.verify(
            actionDelegateMock,
            Mockito.never()
        )!!.defaultAction1()
    }

    @Test
    fun testSkipFollowing() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        create()
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .on(FooException::class) { _, handler ->
                actionDelegateMock!!.action2()
                handler.skipFollowing()
            }
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action3()
            }
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action4()
            }
            .handle(FooException("foo error"))
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        testVerifier.verify(
            actionDelegateMock
        )!!.action2()
        testVerifier.verify(
            actionDelegateMock
        )!!.defaultAlways()
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testSkipAlways() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        create()
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .on(FooException::class) { _, handler ->
                actionDelegateMock!!.action2()
                handler.skipAlways()
            }
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action3()
            }
            .on(FooException::class) { _, _ ->
                actionDelegateMock!!.action4()
            }
            .handle(FooException("foo error"))
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        testVerifier.verify(
            actionDelegateMock
        )!!.action2()
        testVerifier.verify(
            actionDelegateMock
        )!!.action3()
        testVerifier.verify(
            actionDelegateMock
        )!!.action4()
        testVerifier.verify(
            actionDelegateMock
        )!!.defaultAction1()
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testEnumClassHandling() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        create()
            .bindClass(DBError::class) { errorCode ->
                { throwable ->
                    throwable is DBErrorException &&
                        DBError.from(throwable) == errorCode
                }
            }
            .on(DBError.READ_ONLY) { _, errorHandler ->
                actionDelegateMock!!.action1()
                errorHandler.skipAlways()
            }
            .handle(DBErrorException("read-only"))
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testErrorHandlerBlockExecutorExceptionHandling() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        createIsolated()
            .bindClass(DBError::class) { errorCode ->
                { throwable ->
                    throwable is DBErrorException &&
                        DBError.from(throwable) == errorCode
                }
            }
            .on(DBError.READ_ONLY) { _, errorHandler ->
                actionDelegateMock!!.action1()
                errorHandler.skipAlways()
            }
            .runHandling {
                throw DBErrorException("read-only")
            }
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testErrorHandlerBlockExecutorIgnoresNotMatchedException() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        createIsolated()
            .bindClass(DBError::class) { errorCode ->
                { throwable ->
                    throwable is DBErrorException &&
                        DBError.from(throwable) == errorCode
                }
            }
            .on(DBError.READ_ONLY) { _, errorHandler ->
                actionDelegateMock!!.action1()
                errorHandler.skipAlways()
            }
            .runHandling {
                throw DBErrorException("read")
            }
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testErrorHandlerIfSkipDefaults() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        create()
            .skipDefaults()
            .on("closed:bar") { _, _ ->
                actionDelegateMock!!.action1()
            }
            .runHandling {
                throw BarException("", false)
            }
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
        create()
            .on("closed:bar") { _, _ ->
                actionDelegateMock!!.action2()
            }
            .runHandling {
                throw BarException("", false)
            }
        testVerifier.verify(
            actionDelegateMock
        )!!.action2()
        testVerifier.verify(
            actionDelegateMock
        )!!.defaultAction3()
    }

    private enum class DBError {
        READ_ONLY, DEADLOCK, FATAL;

        companion object {
            fun from(error: DBErrorException): DBError {
                return when (error.message) {
                    "read-only" -> READ_ONLY
                    "deadlock" -> DEADLOCK
                    else -> FATAL
                }
            }
        }
    }
}
