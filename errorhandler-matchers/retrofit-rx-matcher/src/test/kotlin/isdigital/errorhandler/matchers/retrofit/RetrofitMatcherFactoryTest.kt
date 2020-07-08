package isdigital.errorhandler.matchers.retrofit

import isdigital.errorhandler.Action
import isdigital.errorhandler.ErrorHandler
import isdigital.errorhandler.ErrorHandler.Companion.createIsolated
import isdigital.errorhandler.matchers.retrofit.RetrofitMatcherFactory.create
import isdigital.errorhandler.matchers.retrofit.RetrofitMatcherFactory.createRange
import junit.framework.TestCase
import org.junit.Test
import org.mockito.Mockito
import retrofit2.adapter.rxjava.HttpException

class RetrofitMatcherFactoryTest : TestCase() {
    internal interface ActionDelegate {
        fun action1()
    }

    private var actionDelegateMock: ActionDelegate? =
        null

    @Throws(Exception::class)
    override fun setUp() {
        actionDelegateMock =
            Mockito.mock(
                ActionDelegate::class.java
            )
    }

    @Test
    fun test_catching_exact_http_code() {
        createIsolated()
            .bind(400, create())
            .bindClass(
                Range::class,
                createRange()
            )
            .on(400) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .on(Range.of(400, 500)) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .handle(HttpException(RetrofitHelper.generateErrorResponseWith(400)))
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(2)
        )!!.action1()
    }

    @Test
    fun test_not_catching_exact_http_code() {
        createIsolated()
            .bind(400, create())
            .bindClass(
                Range::class,
                createRange()
            )
            .on(400) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .on(Range.of(450, 450)) { _, _ ->
                actionDelegateMock!!.action1()
            }
            .handle(HttpException(RetrofitHelper.generateErrorResponseWith(401)))
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(0)
        )!!.action1()
    }

    @Test
    fun test_catching_with_class() {
        createIsolated()
            .bindClass(Int::class, create())
            .on(500) { _, _ ->
                    actionDelegateMock!!.action1()
            }
            .handle(HttpException(RetrofitHelper.generateErrorResponseWith(401)))
        Mockito.verify(
            actionDelegateMock,
            Mockito.times(0)
        )!!.action1()
    }
}
