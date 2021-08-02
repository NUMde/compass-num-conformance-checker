package science.numcompass.conformance.fhir.configuration

import ca.uhn.fhir.context.FhirContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_JSON
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_XML
import science.numcompass.conformance.fhir.rest.FhirConverter

@Configuration
@ComponentScan("science.numcompass.conformance.fhir.rest")
class RestConfiguration(private val fhirContext: FhirContext) : WebMvcConfigurer {

    /**
     * Add [FhirConverter] for custom XML and JSON.
     */
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val xmlTypes = listOf(APPLICATION_FHIR_XML, APPLICATION_XML)
        converters.add(0, FhirConverter(xmlTypes) { fhirContext.newXmlParser() })
        val jsonTypes = listOf(APPLICATION_FHIR_JSON, APPLICATION_JSON)
        converters.add(0, FhirConverter(jsonTypes) { fhirContext.newJsonParser() })
    }
}
