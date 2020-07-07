package isdigital.errorhandler

import isdigital.errorhandler.ErrorHandler.Companion.create
import isdigital.errorhandler.ErrorHandler.Companion.createIsolated
import isdigital.errorhandler.ErrorHandler.Companion.defaultErrorHandler
import isdigital.errorhandler.ErrorHandlerTest.DBError
import isdigital.errorhandler.FooException
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
            .bind(
                "closed:bar",
                object : MatcherFactory<String> {
                    override fun build(errorCode: String): Matcher {
                        return object : Matcher {
                            override fun matches(throwable: Throwable): Boolean {
                                return if (throwable is BarException) {
                                    !throwable.isOpenBar
                                } else {
                                    false
                                }
                            }
                        }
                    }
                }
            )
            .bindClass(
                Int::class,
                object : MatcherFactory<Int> {
                    override fun build(errorCode: Int): Matcher {
                        return object : Matcher {
                            override fun matches(throwable: Throwable): Boolean {
                                return if (throwable is QuxException) {
                                    throwable.errorStatus == errorCode
                                } else {
                                    false
                                }
                            }
                        }
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.defaultAction1()
                    }
                }
            )
            .on(
                500,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.defaultAction2()
                    }
                }
            )
            .on(
                "closed:bar",
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.defaultAction3()
                    }
                }
            )
            .otherwise(object : Action {
                override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                    actionDelegateMock!!.defaultOtherwise()
                }
            })
            .always(object : Action {
                override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                    actionDelegateMock!!.defaultAlways()
                }
            })
    }

    override fun tearDown() {
        defaultErrorHandler()
            .clear()
    }

    @Test
    fun testActionsExecutionOrder() {
        val errorHandler: ErrorHandler = create()
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                    }
                }
            )
            .on(
                object : Matcher {
                    override fun matches(throwable: Throwable): Boolean {
                        return try {
                            FooException::class.java.cast(throwable).isFatal
                        } catch (ignore: ClassCastException) {
                            false
                        }
                    }
                },
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action2()
                    }
                }
            )
            .on(
                "closed:bar",
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action3()
                    }
                }
            )
            .on(
                400,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action4()
                    }
                }
            )
            .on(
                500,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action5()
                    }
                }
            )
            .otherwise(object : Action {
                override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                    actionDelegateMock!!.otherwise1()
                }
            })
            .always(object : Action {
                override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                    actionDelegateMock!!.always1()
                }
            })
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
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                    }
                }
            )
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
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, handler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                        handler.skipDefaults()
                    }
                }
            )
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
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, handler: ErrorHandler) {
                        actionDelegateMock!!.action2()
                        handler.skipFollowing()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action3()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action4()
                    }
                }
            )
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
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, handler: ErrorHandler) {
                        actionDelegateMock!!.action2()
                        handler.skipAlways()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action3()
                    }
                }
            )
            .on(
                FooException::class,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action4()
                    }
                }
            )
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
            .bindClass(
                DBError::class,
                object : MatcherFactory<DBError> {
                    override fun build(errorCode: DBError): Matcher {
                        return object : Matcher {
                            override fun matches(throwable: Throwable): Boolean {
                                if (throwable is DBErrorException) {
                                    return DBError.from(throwable) == errorCode
                                }
                                return false
                            }
                        }
                    }
                }
            )
            .on(
                DBError.READ_ONLY,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                        errorHandler.skipAlways()
                    }
                }
            )
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
            .bindClass(
                DBError::class,
                object : MatcherFactory<DBError> {
                    override fun build(errorCode: DBError): Matcher {
                        return object : Matcher {
                            override fun matches(throwable: Throwable): Boolean {
                                return throwable is DBErrorException && DBError.from(
                                    throwable
                                ) == errorCode
                            }
                        }
                    }
                }
            )
            .on(
                DBError.READ_ONLY,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                        errorHandler.skipAlways()
                    }
                }
            )
            .run(object : BlockExecutor {
                override fun invoke() {
                    throw DBErrorException("read-only")
                }
            })
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
            .bindClass(
                DBError::class,
                object : MatcherFactory<DBError> {
                    override fun build(errorCode: DBError): Matcher {
                        return object : Matcher {
                            override fun matches(throwable: Throwable): Boolean {
                                return throwable is DBErrorException && DBError.from(
                                    throwable
                                ) == errorCode
                            }
                        }
                    }
                }
            )
            .on(
                DBError.READ_ONLY,
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                        errorHandler.skipAlways()
                    }
                }
            )
            .run(object : BlockExecutor {
                override fun invoke() {
                    throw DBErrorException("read")
                }
            })
        testVerifier.verifyNoMoreInteractions()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
    }

    @Test
    fun testErrorHandlerIfSkipDefaults() {
        val testVerifier = Mockito.inOrder(actionDelegateMock)
        create()
            .skipDefaults()
            .on(
                "closed:bar",
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action1()
                    }
                }
            )
            .run(object : BlockExecutor {
                override fun invoke() {
                    throw BarException("", false)
                }
            })
        testVerifier.verify(
            actionDelegateMock
        )!!.action1()
        Mockito.verifyNoMoreInteractions(actionDelegateMock)
        create()
            .on(
                "closed:bar",
                object : Action {
                    override fun execute(throwable: Throwable, errorHandler: ErrorHandler) {
                        actionDelegateMock!!.action2()
                    }
                }
            )
            .run(object : BlockExecutor {
                override fun invoke() {
                    throw BarException("", false)
                }
            })
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
