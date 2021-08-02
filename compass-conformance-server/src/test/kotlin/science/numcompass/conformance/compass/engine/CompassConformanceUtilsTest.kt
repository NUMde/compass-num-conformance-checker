package science.numcompass.conformance.compass.engine

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.junit.jupiter.api.Test

internal class CompassConformanceUtilsTest {

    private val fhirContext = FhirContext(FhirVersionEnum.R4)

    private fun validTestBundleWithQuestionnaire() = bundle("/Bundle-conf-test-input-bundle-quest.json")
    private fun geccoBundle() = bundle("/Gecco-bundle.json")

    private fun bundle(path: String) = javaClass
        .getResourceAsStream(path)
        .let { fhirContext.newJsonParser().parseResource(it) as Bundle }

    @Test
    fun whenOperationOutcomes_containsAFatal_itIsNotSuccessful() {
        val opsOut = OperationOutcome()
        opsOut.addIssue()
            .apply {
                severity = OperationOutcome.IssueSeverity.FATAL
                code = OperationOutcome.IssueType.BUSINESSRULE
            }

        assertThat(opsOut.isSuccessful()).isFalse()
    }

    @Test
    fun whenOperationOutcomes_containsAnError_itIsNotSuccessful() {
        val opsOut = OperationOutcome()
        opsOut.addIssue()
            .apply {
                severity = OperationOutcome.IssueSeverity.ERROR
                code = OperationOutcome.IssueType.BUSINESSRULE
            }

        assertThat(opsOut.isSuccessful()).isFalse()
    }

    @Test
    fun whenOperationOutcomes_containsOnlyWarningsAndInformation_itIsSuccessful() {
        val opsOut = OperationOutcome()
        opsOut.addIssue()
            .apply {
                severity = OperationOutcome.IssueSeverity.WARNING
                code = OperationOutcome.IssueType.BUSINESSRULE
            }
        opsOut.addIssue()
            .apply {
                severity = OperationOutcome.IssueSeverity.INFORMATION
                code = OperationOutcome.IssueType.BUSINESSRULE
            }

        assertThat(opsOut.isSuccessful()).isTrue()
    }

    @Test
    fun whenOperationOutcomes_doesNotContainANyIssues_itIsSuccessful() {
        val opsOut = OperationOutcome()

        assertThat(opsOut.isSuccessful()).isTrue()
    }

    @Test
    fun whenSectionWithLoincCodeExistsInBundle_allResourcesAreReturned() {
        val testBundle = validTestBundleWithQuestionnaire()

        val resources = getSectionResourceByLoincCode(testBundle, TEST_RESOURCE_SECTION_LOINC_CODE)

        assertThat(resources.size).isEqualTo(3)
    }

    @Test
    fun whenSectionWithLoincCodeDoesNotInBundle_anEmptyListIsReturned() {
        val testBundle = validTestBundleWithQuestionnaire()

        val allergyConsultNoteLoincCode = "95761-3"
        val resources = getSectionResourceByLoincCode(testBundle, allergyConsultNoteLoincCode)

        assertThat(resources).isEmpty()
    }

    @Test
    fun whenBundleIsNotAFhirDocument_anEmptyListIsReturned() {
        val testBundle = geccoBundle()

        val resources = getSectionResourceByLoincCode(testBundle, TEST_RESOURCE_SECTION_LOINC_CODE)

        assertThat(resources).isEmpty()
    }
}
