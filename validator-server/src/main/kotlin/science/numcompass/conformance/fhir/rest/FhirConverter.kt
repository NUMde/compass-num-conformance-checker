package science.numcompass.conformance.fhir.rest

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter

/**
 * This converter uses the [IParser] to parse FHIR messages.
 */
class FhirConverter(mediaTypes: List<MediaType>, private val parserProvider: () -> IParser) :
    AbstractHttpMessageConverter<Any>() {

    init {
        supportedMediaTypes = mediaTypes
    }

    override fun supports(clazz: Class<*>) =
        String::class.java == clazz || IBaseResource::class.java.isAssignableFrom(clazz)

    override fun readInternal(clazz: Class<out Any>, inputMessage: HttpInputMessage): Any {
        val rawResource = inputMessage.body.bufferedReader().readText()
        if (rawResource.isBlank()) {
            throw DataFormatException("Resource is empty")
        }
        return if (String::class.java == clazz) rawResource else parserProvider().parseResource(rawResource)
    }

    override fun writeInternal(resource: Any, outputMessage: HttpOutputMessage) {
        outputMessage.body.bufferedWriter().use {
            if (resource is String) {
                it.write(resource)
            } else if (resource is IBaseResource) {
                parserProvider().encodeResourceToWriter(resource, it)
            }
        }
    }
}
