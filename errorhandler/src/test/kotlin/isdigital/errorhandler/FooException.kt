package isdigital.errorhandler

/**
 * A checked test exception for using in [ErrorHandler] unit tests
 *
 * @author Stratos Pavlakis
 */
class FooException : Exception {
    var isFatal = false
        private set

    constructor(message: String?) : super(message) {}
    constructor(message: String?, fatal: Boolean) : super(message) {
        isFatal = fatal
    }
}
