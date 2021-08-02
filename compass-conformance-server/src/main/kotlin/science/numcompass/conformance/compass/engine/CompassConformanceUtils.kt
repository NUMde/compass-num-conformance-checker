package science.numcompass.conformance.compass.engine

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.FATAL
import org.hl7.fhir.r4.model.Resource

/*
* Various constant used in multiple places.
*/
internal const val CONFORMANCE_TEST_BUNDLE_PROFILE =
    "https://num-compass.science/fhir/StructureDefinition/NumConformanceTestBundle"
internal const val GECCO_BASE_URL = "https://www.netzwerk-universitaetsmedizin.de/fhir/"
internal const val TARGET_GECCO_PROFILE_EXTENSION_URL =
    "https://num-compass.science/fhir/StructureDefinition/GeccoTargetProfile"
internal const val PUBLISHING_ORG_SECTION_LOINC_CODE = "91025-7"
internal const val APP_SECTION_LOINC_CODE = "92040-5"
internal const val QUESTIONNAIRE_SECTION_LOINC_CODE = "74468-0"
internal const val TEST_RESOURCE_SECTION_LOINC_CODE = "68839-0"
internal const val LOINC_URL = "http://loinc.org"

/**
 * Check an OperationOutcome resources for errors
 *
 * @return true if OperationOutcome contains neither errors nor fatals
 */
internal fun OperationOutcome.isSuccessful(): Boolean =
    this.issue?.none { it.severity == FATAL || it.severity == ERROR } ?: true

/**
 * Extract all resources in the FHIR document Bundle which are listed in the Composition
 * section that is marked with a particular LOINC code.
 *
 * @param inputBundle FHIR document Bundle
 * @param loincCode LOINC code used to designate the relevant section of the Composition resources
 * @return List containing all resources occurring as entries in the Composition section
 */
internal fun getSectionResourceByLoincCode(inputBundle: Bundle, loincCode: String): List<Resource> { //NOSONAR
    // Composition must always be first resource in a FHIR document bundle
    val composition = inputBundle.entry?.first()?.resource as? Composition
        ?: return emptyList()
    val resourceUrlsInSection = composition.section
        ?.filter { sec -> sec.code.coding.any { it.system == LOINC_URL && it.code == loincCode } }
        ?.flatMap { it.entry }
        ?.map { it.reference }
        ?: return emptyList()
    return inputBundle.entry
        ?.filter { it.fullUrl in resourceUrlsInSection }
        ?.mapNotNull { it.resource } ?: emptyList()
}
