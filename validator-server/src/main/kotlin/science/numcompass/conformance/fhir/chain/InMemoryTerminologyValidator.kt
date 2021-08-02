package science.numcompass.conformance.fhir.chain

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.util.FhirVersionIndependentConcept
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource
import science.numcompass.conformance.fhir.chain.ValidationChain.IValidator

open class InMemoryTerminologyValidator(context: FhirContext) : IValidator,
    InMemoryTerminologyServerValidationSupport(context) {

    override fun validateCodeInValueSet(
        context: ValidationSupportContext,
        options: ConceptValidationOptions?,
        codeSystemUrlAndVersion: String?,
        code: String?,
        display: String?,
        valueSet: IBaseResource
    ): IValidationSupport.CodeValidationResult? {
        if (isExpandedValueSetEmpty(context, valueSet, codeSystemUrlAndVersion, code)) {
            // if the value set is empty, we at least validate the code itself
            return context.rootValidationSupport.validateCode(
                context,
                options,
                codeSystemUrlAndVersion,
                code,
                display,
                ""
            )?.apply { message = "Expanded value set is empty! $message" }
        }
        return super.validateCodeInValueSet(
            context,
            options,
            codeSystemUrlAndVersion,
            code,
            display,
            valueSet
        )
    }

    /**
     * Check whether the expanded value set is empty.
     */
    private fun isExpandedValueSetEmpty(
        context: ValidationSupportContext, valueSet: IBaseResource,
        codeSystemUrlAndVersion: String?,
        code: String?
    ): Boolean {
        val expandedValueSet = expandValueSetToCanonical(context, valueSet, codeSystemUrlAndVersion, code)
        val contains = expandedValueSet?.expansion?.contains ?: true
        val codes = mutableListOf<FhirVersionIndependentConcept>()

        invokeSuper<Unit>("flattenAndConvertCodesR5", contains, codes)

        return codes.isEmpty()
    }

    /**
     * Calls [InMemoryTerminologyServerValidationSupport.expandValueSetToCanonical] to expand the value set.
     */
    private fun expandValueSetToCanonical(
        context: ValidationSupportContext,
        valueSet: IBaseResource,
        codeSystemUrlAndVersion: String?,
        code: String?
    ): org.hl7.fhir.r5.model.ValueSet? {
        return invokeSuper("expandValueSetToCanonical", context, valueSet, codeSystemUrlAndVersion, code)
    }

    /**
     * Helper to call private functions of the superclass.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> invokeSuper(method: String, vararg args: Any?): T? {
        return InMemoryTerminologyServerValidationSupport::class.java.declaredMethods
            .find { it.name == method }
            ?.let {
                it.isAccessible = true
                it.invoke(this, *args)
            } as T?
    }
}
