package science.numcompass.conformance.compass.chain

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource
import science.numcompass.conformance.fhir.chain.ValidationChain.IValidator

open class LoincSnomedCtWarningSupport(context: FhirContext) : IValidator, BaseValidationSupport(context) {

    override fun isCodeSystemSupported(context: ValidationSupportContext?, system: String?) =
        isLoincOrSnomedCt(system)

    override fun isValueSetSupported(context: ValidationSupportContext?, valueSetUrl: String?) = true

    override fun validateCode(
        context: ValidationSupportContext?,
        options: ConceptValidationOptions?,
        codeSystem: String?,
        code: String,
        display: String?,
        valueSetUrl: String?
    ) = dummyResult(codeSystem, code)

    override fun validateCodeInValueSet(
        context: ValidationSupportContext?,
        options: ConceptValidationOptions?,
        codeSystem: String?,
        code: String,
        display: String?,
        valueSet: IBaseResource
    ) = dummyResult(codeSystem, code)

    private fun dummyResult(codeSystem: String?, code: String?): IValidationSupport.CodeValidationResult? =
        if (isLoincOrSnomedCt(codeSystem)) IValidationSupport.CodeValidationResult()
            .setSeverity(IValidationSupport.IssueSeverity.WARNING)
            .setCodeSystemName(codeSystem)
            .setCode(code)
        else null

    private fun isLoincOrSnomedCt(url: String?) = url?.let {
        it.startsWith("http://loinc.org") || it.startsWith("http://snomed.info/sct")
    } ?: false
}
