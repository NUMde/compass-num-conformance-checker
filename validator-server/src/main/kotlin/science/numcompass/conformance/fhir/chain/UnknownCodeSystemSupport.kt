package science.numcompass.conformance.fhir.chain

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult
import ca.uhn.fhir.context.support.IValidationSupport.IssueSeverity.WARNING
import ca.uhn.fhir.context.support.ValidationSupportContext
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource
import science.numcompass.conformance.fhir.chain.ValidationChain.IValidator

/**
 * Adds support for unknown code systems, validating codes against an unknown code system produce a
 * [CodeValidationResult] with a severity set to [WARNING].
 */
open class UnknownCodeSystemSupport(context: FhirContext) : IValidator,
    UnknownCodeSystemWarningValidationSupport(context) {

    override fun isCodeSystemSupported(context: ValidationSupportContext?, system: String?) =
        !isCodeSystemKnown(context, system)

    override fun isValueSetSupported(context: ValidationSupportContext?, url: String?) = true

    override fun validateCode(
        context: ValidationSupportContext?,
        options: ConceptValidationOptions?,
        codeSystem: String?,
        code: String?,
        display: String?,
        valueSetUrl: String?
    ) = validate(context, codeSystem, code)

    override fun validateCodeInValueSet(
        context: ValidationSupportContext?,
        options: ConceptValidationOptions?,
        codeSystem: String?,
        code: String?,
        display: String?,
        valueSet: IBaseResource
    ) = validate(context, codeSystem, code)

    /**
     * @return null if the code system is known, otherwise a [CodeValidationResult]
     */
    private fun validate(
        context: ValidationSupportContext?,
        codeSystem: String?,
        code: String?
    ): CodeValidationResult? {
        if (isCodeSystemKnown(context, codeSystem)) {
            return null
        }
        return CodeValidationResult()
            .setSeverity(WARNING)
            .setCodeSystemName(codeSystem)
            .setCode(code)
            .setMessage("Unknown code system: $codeSystem")
    }

    private fun isCodeSystemKnown(context: ValidationSupportContext?, codeSystem: String?) =
        context?.rootValidationSupport?.fetchCodeSystem(codeSystem) != null
}
