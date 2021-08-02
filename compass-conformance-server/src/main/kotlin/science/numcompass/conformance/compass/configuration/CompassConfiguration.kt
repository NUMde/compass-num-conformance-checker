package science.numcompass.conformance.compass.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.FhirValidator
import org.hl7.fhir.r4.model.DomainResource
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import science.numcompass.conformance.compass.engine.CompassConformanceEngine
import science.numcompass.conformance.fhir.chain.PrePopulatedValidator
import science.numcompass.conformance.fhir.configuration.R4Configuration

@AutoConfigureAfter(R4Configuration::class)
@Configuration(proxyBeanMethods = false)
@ComponentScan(
    "science.numcompass.conformance.compass.rest",
    "science.numcompass.conformance.compass.report",
    "science.numcompass.conformance.compass.service"
)
class CompassConfiguration {

    @Bean
    fun compassConformanceEngine(fhirValidator: FhirValidator) = CompassConformanceEngine(fhirValidator)

    /**
     * Customize the [PrePopulatedValidator] by adding the profiles used for checking the input.
     */
    @Bean
    fun numConformanceCustomizer(fhirContext: FhirContext) = PrePopulatedValidator.Customizer { validator ->
        val jsonParser = fhirContext.newJsonParser()
        listOf(
            "/fhir/StructureDefinition-NumConformanceDevice.json",
            "/fhir/StructureDefinition-NumConformanceOrganization.json",
            "/fhir/StructureDefinition-NumConformanceTestBundle.json",
            "/fhir/StructureDefinition-NumConformanceTestComposition.json"
        ).map { javaClass.getResourceAsStream(it) }
            .map { jsonParser.parseResource(it) as DomainResource }
            .forEach { validator.addStructureDefinition(it) }
    }
}
