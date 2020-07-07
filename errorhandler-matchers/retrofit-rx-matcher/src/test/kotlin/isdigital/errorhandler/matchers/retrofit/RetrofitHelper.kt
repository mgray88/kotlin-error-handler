package isdigital.errorhandler.matchers.retrofit

import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

object RetrofitHelper {
    fun generateMockResponseWith(networkCode: Int): Response {
        return Response.Builder()
            .code(networkCode)
            .message("OK")
            .body(convertStringResponseBody("MOCK"))
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .build()
    }

    fun generateSuccessResponseWith(networkCode: Int): retrofit2.Response<*> {
        return retrofit2.Response.success<Any?>(
            null,
            generateMockResponseWith(networkCode)
        )
    }

    fun generateErrorResponseWith(networkCode: Int): retrofit2.Response<*> {
        return retrofit2.Response.error<Any>(
            networkCode,
            generateMockResponseWith(networkCode).body()
        )
    }

    fun convertStringResponseBody(value: String?): ResponseBody {
        return ResponseBody.create(MediaType.parse("text/plain"), value)
    }
}
