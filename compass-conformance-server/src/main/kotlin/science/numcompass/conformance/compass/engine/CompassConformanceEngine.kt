package science.numcompass.conformance.compass.engine

import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ValidationOptions
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent

private val logger = KotlinLogging.logger { }

/**
 * Class for orchestrating the full NUM-COMPASS conformance check.
 *
 * @constructor
 * @param fhirValidator HAPI FHIR validator instance to use for validation against FHIR profiles
 */
class CompassConformanceEngine(private val fhirValidator: FhirValidator) {

    /**
     * Check a COMPASS conformance input Bundle for conformance.
     *
     * Validation includes both checking the data against the GECCO profiles and checking that all
     * the needed meta-data required (e.g. information about the producing app) are submitted.
     *
     * Unexpected exceptions during validation are caught and treated as fatal validation issues.
     *
     * @param inputBundle FHIR Bundle containing everything need for a COMPASS conformance check
     * @return OperationOutcome resource listing any errors, warnings, and informational items
     */
    fun checkConformance(inputBundle: Bundle): OperationOutcome {
        val validationOptions = ValidationOptions().addProfile(CONFORMANCE_TEST_BUNDLE_PROFILE)
        var opsOut: OperationOutcome
        try {
            // Validation of entire Bundle against relevant FHIR profiles
            opsOut = fhirValidator.validateWithResult(inputBundle, validationOptions)
                .toOperationOutcome() as OperationOutcome
            // Validation against further business rules
            checkGeccoProfileDeclarations(inputBundle).forEach { opsOut.addIssue(it) }
            checkGeccoProfilesFromQuestionnaires(inputBundle).forEach { opsOut.addIssue(it) }
        } catch (exception: Exception) { //NOSONAR
            logger.error { "--- ERROR ---\n${exception.message}\n---STACKTRACE---\n${exception.stackTrace}" }
            opsOut = OperationOutcome()
            opsOut.addIssue().apply {
                severity = OperationOutcome.IssueSeverity.FATAL
                code = OperationOutcome.IssueType.EXCEPTION
                diagnostics = "Conformance check encountered an unexpected error!: ${exception.message}"
            }
        }

        return opsOut
    }

    /**
     * Check whether all GECCO Profiles declared in the questionnaires (if present)
     * are covered by at least one input resource.
     */
    private fun checkGeccoProfilesFromQuestionnaires(inputBundle: Bundle): List<OperationOutcomeIssueComponent> {
        val questionnaireResources = getSectionResourceByLoincCode(inputBundle, QUESTIONNAIRE_SECTION_LOINC_CODE)
        if (questionnaireResources.isEmpty()) return emptyList()

        // Collect all profiles declared in Questionnaires
        val geccoProfileUrlsInQuestionnaire = questionnaireResources
            .asSequence()
            .mapNotNull { it as? Questionnaire }
            .map { getTargetGeccoProfileExtensionUrls(it.item) }
            .flatten()
            .toSet()
        // Collect all profiles declared on top level resources
        val geccoProfileUrlsInTopLevelResource =
            getSectionResourceByLoincCode(inputBundle, TEST_RESOURCE_SECTION_LOINC_CODE)
                .asSequence()
                .mapNotNull { it.meta?.profile }
                .flatMap { profs -> profs.map { it.value } }
                .filter { it?.startsWith(GECCO_BASE_URL) == true }
                .toSet()
        val missingProfileUrls = geccoProfileUrlsInQuestionnaire.minus(geccoProfileUrlsInTopLevelResource)
        return missingProfileUrls.map {
            OperationOutcomeIssueComponent().apply {
                severity = OperationOutcome.IssueSeverity.ERROR
                code = OperationOutcome.IssueType.BUSINESSRULE
                diagnostics = "GECCO profile declared in Questionnaire was not declared in any of the top-level" +
                    " resource: $it"
            }
        }
    }

    /**
     * Recursively retrieve all the declared target GECCO profiles.
     */
    private fun getTargetGeccoProfileExtensionUrls(
        questionnaireItem: List<QuestionnaireItemComponent>?,
        currentUrlSet: Set<String> = emptySet()
    ): Set<String> {
        if (questionnaireItem == null) return currentUrlSet
        val targetGeccoProfilesOnThisLevel = questionnaireItem
            .asSequence()
            .mapNotNull { it.extension }
            .flatten()
            .filter { it.url?.startsWith(TARGET_GECCO_PROFILE_EXTENSION_URL) == true }
            .mapNotNull { (it.value as CanonicalType).value }
            .toSet()
        val newUrlList = currentUrlSet + targetGeccoProfilesOnThisLevel
        // Add all new URLs from sub-items
        return questionnaireItem.fold(newUrlList) { urls, qItem ->
            getTargetGeccoProfileExtensionUrls(qItem.item, urls)
        }
    }

    /**
     * Check whether all top-level resources declare a GECCO profile and return an OperationOutcome issue
     * for each of those that do not.
     */
    private fun checkGeccoProfileDeclarations(inputBundle: Bundle): List<OperationOutcomeIssueComponent> {
        val topLevelResource = getSectionResourceByLoincCode(inputBundle, TEST_RESOURCE_SECTION_LOINC_CODE)
        if (topLevelResource.isEmpty()) {
            return listOf(
                OperationOutcomeIssueComponent().apply {
                    severity = OperationOutcome.IssueSeverity.ERROR
                    code = OperationOutcome.IssueType.BUSINESSRULE
                    diagnostics = "Test data set is empty"
                }
            )
        }
        return topLevelResource.mapIndexed { index, res ->
            if (res.meta?.profile?.none { it.value?.startsWith(GECCO_BASE_URL) == true } == true) {
                OperationOutcomeIssueComponent().apply {
                    severity = OperationOutcome.IssueSeverity.ERROR
                    code = OperationOutcome.IssueType.BUSINESSRULE
                    diagnostics = "In the test data section of the Composition resource, the resource with index" +
                        " $index does not declare a GECCO profile"
                }
            } else {
                null
            }
        }.filterNotNull()
    }
}
