package science.numcompass.conformance.fhir.chain

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain
import mu.KotlinLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.aop.TargetClassAware
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger { }

/**
 * This class represents a validation chain based on the [IValidator]. This enabled building the validation chain using
 * Spring functionality (bean ordering) without introducing a cyclic dependency.
 */
open class ValidationChain(fhirContext: FhirContext) : JpaValidationSupportChain(fhirContext) {

    interface IValidator

    @Autowired
    lateinit var validators: List<IValidator>

    @PostConstruct
    override fun postConstruct() {
        logger.info { "Building validation support chain" }
        validators.map { it as IValidationSupport }.forEach {
            logger.info { "     chain link ${getActualClass(it)}) added" }
            addValidationSupport(it)
        }
    }

    override fun fetchCodeSystem(system: String?): IBaseResource? =
        if (system == null) null else super.fetchCodeSystem(system)

    override fun fetchValueSet(url: String?): IBaseResource? =
        if (url == null) null else super.fetchValueSet(url)

    /**
     * Retrieves the class of a validator respecting the possibility that the object might be a Spring proxy.
     */
    private fun getActualClass(it: Any) = if (it is TargetClassAware) it.targetClass else it.javaClass
}
