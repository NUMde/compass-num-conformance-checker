package science.numcompass.conformance.fhir.rest

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

private val logger = KotlinLogging.logger { }

/**
 * Provides exception handlers for all REST controllers.
 */
@ControllerAdvice
class ExceptionAdvices {

    /**
     * Handles invalid FHIR requests and responds with HTTP 400.
     */
    @ExceptionHandler(
        DataFormatException::class,
        HttpMessageNotReadableException::class,
        InvalidRequestException::class
    )
    fun handleInvalidInput(exception: Exception): ResponseEntity<String?> {
        logger.error(exception) { exception.message }
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(exception.message)
    }

    /**
     * General exception handler responding with HTTP 500 and the message of the exception.
     */
    @ExceptionHandler
    fun handleException(exception: Exception): ResponseEntity<String?> {
        logger.error(exception) { exception.message }
        return ResponseEntity.internalServerError().contentType(MediaType.TEXT_PLAIN).body(exception.message)
    }
}
