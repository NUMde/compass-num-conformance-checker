package science.numcompass.conformance.fhir.chain

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.jpa.dao.JpaPersistedResourceValidationSupport
import ca.uhn.fhir.jpa.packages.NpmJpaValidationSupport
import ca.uhn.fhir.jpa.term.TermConceptMappingSvcImpl
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import science.numcompass.conformance.fhir.chain.ValidationChain.IValidator

// These validators extend the default validators provided by HAPI and also implement the interface [IValidator] to
// prevent a cyclic dependency while building the validation chain

open class DefaultProfileValidator(context: FhirContext) : IValidator, DefaultProfileValidationSupport(context)

open class PrePopulatedValidator(context: FhirContext) : IValidator, PrePopulatedValidationSupport(context) {

    fun interface Customizer {
        fun customize(validationSupport: PrePopulatedValidator)
    }
}

open class JpaPersistedResourceValidator(context: FhirContext) : IValidator,
    JpaPersistedResourceValidationSupport(context)

open class SnapshotGeneratingValidator(context: FhirContext) : IValidator, SnapshotGeneratingValidationSupport(context)

open class NpmJpaValidator : IValidator, NpmJpaValidationSupport()

open class CommonCodeSystemsTerminologyValidator(context: FhirContext) : IValidator,
    CommonCodeSystemsTerminologyService(context)

open class TermConceptMapper : IValidator, TermConceptMappingSvcImpl()

open class SubChain : IValidator, ValidationSupportChain()
