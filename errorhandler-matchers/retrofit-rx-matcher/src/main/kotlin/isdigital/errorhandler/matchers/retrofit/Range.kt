package isdigital.errorhandler.matchers.retrofit

/**
 * Range class for HTTP status codes
 */
class Range private constructor(val lowerBound: Int, val upperBound: Int) {

    /**
     * Checks if the passed httpStatusCode is contained in given range
     *
     * @param httpStatusCode the status code to check
     * @return true if contains, otherwise false
     */
    operator fun contains(httpStatusCode: Int): Boolean {
        return httpStatusCode >= lowerBound && httpStatusCode <= upperBound
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val range =
            o as Range
        return if (lowerBound != range.lowerBound) false else upperBound == range.upperBound
    }

    override fun hashCode(): Int {
        var result = lowerBound
        result = 31 * result + upperBound
        return result
    }

    companion object {
        /**
         * Creates a Range object with lower and upper bound
         * @param lowerBound lower limit of Range
         * @param upperBound upper limit of Range
         *
         * @return a Range instance
         */
        @JvmStatic
        fun of(lowerBound: Int, upperBound: Int): Range {
            return Range(lowerBound, upperBound)
        }
    }
}
