package isdigital.errorhandler

/**
 * A checked test exception for using in [ErrorHandler] unit tests
 *
 * @author Stratos Pavlakis
 */
class QuxException(val errorStatus: Int) : Exception()
