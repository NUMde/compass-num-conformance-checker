package science.numcompass.conformance.fhir.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.IInstanceValidatorModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FhirValidatorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun fhirValidator(fhirContext: FhirContext, instanceValidator: IInstanceValidatorModule): FhirValidator =
        fhirContext.newValidator().apply {
            registerValidatorModule(instanceValidator)
        }
}
