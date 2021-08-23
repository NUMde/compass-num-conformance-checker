package science.numcompass.conformance.fhir.chain

import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.context.support.ValueSetExpansionOptions
import ca.uhn.fhir.jpa.api.config.DaoConfig
import ca.uhn.fhir.jpa.term.IValueSetConceptAccumulator
import ca.uhn.fhir.jpa.term.TermReadSvcR4
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.ValueSet
import science.numcompass.conformance.fhir.chain.ValidationChain.IValidator

/**
 * This server extends [TermReadSvcR4] to prevent missing code system errors.
 */
open class TerminologyService(daoConfig: DaoConfig) : IValidator, TermReadSvcR4() {

    /**
     * Default options to prevent errors caused by missing code systems
     * and expanding value sets with more than 1000 codes.
     */
    private val defaultExpansionOptions = ValueSetExpansionOptions().apply {
        count = daoConfig.maximumExpansionSize
        isFailOnMissingCodeSystem = false
    }

    override fun expandValueSet(
        context: ValidationSupportContext?,
        options: ValueSetExpansionOptions?,
        valueSet: IBaseResource
    ): IValidationSupport.ValueSetExpansionOutcome? {
        return super.expandValueSet(context, options ?: defaultExpansionOptions, valueSet)
    }

    override fun expandValueSet(
        options: ValueSetExpansionOptions?,
        valueSet: ValueSet?,
        accumulator: IValueSetConceptAccumulator?
    ) {
        super.expandValueSet(options ?: defaultExpansionOptions, valueSet, accumulator)
    }

    /**
     * This override catches post-coordinates (composite) SNOMED codes that cannot be validated and
     * returns a warning for them (to avoiding spurious errors being returned from code validation).
     */
    override fun validateCode(
        theValidationSupportContext: ValidationSupportContext?,
        theOptions: ConceptValidationOptions?,
        theCodeSystem: String?,
        theCode: String?,
        theDisplay: String?,
        theValueSetUrl: String?
    ): IValidationSupport.CodeValidationResult? {

        if (isPostCoordinateSnomed(theCodeSystem, theCode))
            return IValidationSupport.CodeValidationResult()
                .setSeverity(IValidationSupport.IssueSeverity.WARNING)
                .setCodeSystemName(theCodeSystem)
                .setCode(theCode)
                .setMessage("Post-coordinated (composite) SNOMED codes are not supported - did not validate $theCode")

        return super<TermReadSvcR4>.validateCode(
            theValidationSupportContext,
            theOptions,
            theCodeSystem,
            theCode,
            theDisplay,
            theValueSetUrl
        )
    }

    /**
     * Check if a system-code pair represents a post-coordinated (composite) SNOMED code.
     */
    private fun isPostCoordinateSnomed(theCodeSystem: String?, theCode: String?): Boolean {
        if (theCode == null || theCodeSystem?.startsWith("http://snomed.info/sct") != true) return false
        val isSimpleSnomedCodeRegExp = Regex("\\d+")
        return !isSimpleSnomedCodeRegExp.matches(theCode)
    }

}
