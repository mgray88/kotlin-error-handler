package isdigital.errorhandler

/**
 * A runtime test exception for using in [ErrorHandler] unit tests
 *
 * @author Pavlos-Petros Tournaris
 */
class DBErrorException(message: String?) : Exception(message)
