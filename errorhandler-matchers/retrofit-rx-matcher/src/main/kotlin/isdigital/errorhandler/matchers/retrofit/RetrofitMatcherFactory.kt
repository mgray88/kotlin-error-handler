package isdigital.errorhandler.matchers.retrofit

import isdigital.errorhandler.Matcher
import isdigital.errorhandler.MatcherFactory
import retrofit2.adapter.rxjava.HttpException

object RetrofitMatcherFactory {
    /**
     * Creates a [MatcherFactory] that checks HTTP statuses
     *
     * @return new MatcherFactory for Retrofit Rx HttpException that works with Integer
     */
    @JvmStatic
    fun create(): MatcherFactory<Int> {
        return object : MatcherFactory<Int> {
            override fun build(errorCode: Int): Matcher {
                return object : Matcher {
                    override fun matches(throwable: Throwable): Boolean {
                        return throwable is HttpException &&
                            throwable.code() == errorCode
                    }
                }
            }
        }
    }

    /**
     * Creates a [MatcherFactory] that checks if HTTP status is in given [Range]
     *
     * @return new MatcherFactory for Retrofit Rx HttpException that works with Range
     */
    @JvmStatic
    fun createRange(): MatcherFactory<Range> {
        return object : MatcherFactory<Range> {
            override fun build(errorCode: Range): Matcher {
                return object : Matcher {
                    override fun matches(throwable: Throwable): Boolean {
                        return throwable is HttpException &&
                            errorCode.contains(throwable.code())
                    }
                }
            }
        }
    }
}
