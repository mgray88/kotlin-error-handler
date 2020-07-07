package isdigital.errorhandler

/**
 * A runtime test exception for using in [ErrorHandler] unit tests
 *
 * @author Stratos Pavlakis
 */
class BarException : RuntimeException {
    var isOpenBar = true
        private set

    constructor(message: String?) : super(message) {}
    constructor(message: String?, openBar: Boolean) : super(message) {
        isOpenBar = openBar
    }
}
