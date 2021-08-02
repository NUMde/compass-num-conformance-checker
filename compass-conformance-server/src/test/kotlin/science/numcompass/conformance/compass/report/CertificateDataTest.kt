package science.numcompass.conformance.compass.report

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Observation
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CertificateDataTest {

    private val fhirContext = FhirContext(FhirVersionEnum.R4)

    private fun validTestBundleWithQuestionnaire() = bundle("/Bundle-conf-test-input-bundle-quest.json")
    private fun validTestBundleNoQuestionnaire() = bundle("/Bundle-conf-test-input-bundle.json")

    private fun bundle(path: String) = javaClass.getResourceAsStream(path)
        .let { fhirContext.newJsonParser().parseResource(it) as Bundle }

    private fun doubleProfileObs() = javaClass.getResourceAsStream("/double-gecco-profile-observation.json")
        .let { fhirContext.newJsonParser().parseResource(it) as Observation }

    private val resultWithQuestionnaire = extractCertificateData(validTestBundleWithQuestionnaire())
    private val resultNoQuestionnaire = extractCertificateData(validTestBundleNoQuestionnaire())

    @Test
    fun testDate_isSetToCurrentDate() {
        assertThat(resultWithQuestionnaire.testDate.toString()).isEqualTo(LocalDate.now().toString())
    }

    @Test
    fun appName_isCorrectlyCaptured() {
        assertThat(resultWithQuestionnaire.appName).isEqualTo("Example COVID-19 app")
    }

    @Test
    fun appVersion_isCorrectlyCaptured() {
        assertThat(resultWithQuestionnaire.appVersion).isEqualTo("1.3.4")
    }

    @Test
    fun appManufacturer_ifPresent_isCorrectlyCaptured() {
        val inputManuBundle = validTestBundleWithQuestionnaire()
        val manuName = "ACME Corp"
        (inputManuBundle.entry[1].resource as Device).manufacturer = manuName

        val resultWithManu = extractCertificateData(inputManuBundle)

        assertThat(resultWithManu.manufacturer).isEqualTo(manuName)
    }

    @Test
    fun ifAppManufacturer_ifNotPresentInInput_itIsNotSet() {
        assertThat(resultWithQuestionnaire.manufacturer).isNull()
    }

    @Test
    fun publishingOrgInformation_isCorrectlyCaptured() {
        val expectedResult =
            """Klinik für Infektiologie - Universitätsklinikum Beispielstadt
                |Spitalgasse 123
                |123456 Beispielstadt
                |Germany
                |E-Mail: uk-beispiel@example.com""".trimMargin("|")

        assertThat(resultWithQuestionnaire.orgData).isEqualTo(expectedResult)
    }

    @Test
    fun whenNoQuestionnaires_allTestedGeccoProfiles_areCorrectlyCaptured() {
        val expectedResult = listOf(
            "Resourcentyp Observation",
            "    2 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr",
            "Resourcentyp Patient",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient"
        )

        assertThat(resultNoQuestionnaire.summary).isEqualTo(expectedResult)
    }

    @Test
    fun whenQuestionnairePresent_allTestedGeccoProfiles_areCorrectlyCaptured() {
        val expectedResult = listOf(
            "Resourcentyp Observation",
            "    2 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr",
            "Resourcentyp Patient",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient",
            "",
            "Die übermittelten Testdaten erfassen alle Profile, die in den 1 zugrundeliegenden",
            "Questionnaire(s) deklariert wurden."
        )

        assertThat(resultWithQuestionnaire.summary).isEqualTo(expectedResult)
    }

    @Test
    fun ifATestResourceDeclaresMultipleGeccoProfiles_allProfilesAreCounted_butResourceIsStillOnlyCounterOnce() {
        val inputDoubleProfBundle = validTestBundleNoQuestionnaire()
        // Replace second observation resource with one that declares two GECCO profiles
        inputDoubleProfBundle.entry.last().resource = doubleProfileObs()

        val resultWithDoubleProf = extractCertificateData(inputDoubleProfBundle)

        val expectedResult = listOf(
            "Resourcentyp Observation",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/body-weight",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/vital-signs-base",
            "Resourcentyp Patient",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient"
        )
        assertThat(resultWithDoubleProf.summary).isEqualTo(expectedResult)
    }

    @Test
    fun ifANonGeccoProfileIsDeclared_itIsNotListed() {
        val inputNonGeccoProfBundle = validTestBundleNoQuestionnaire()
        // Add non-GECCO profile
        inputNonGeccoProfBundle.entry.last().resource.meta.profile.add(
			CanonicalType("http://hl7.org/fhir/StructureDefinition/Observation")
        )

        val resultWithDoubleProf = extractCertificateData(inputNonGeccoProfBundle)

        val expectedResult = listOf(
            "Resourcentyp Observation",
            "    2 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-rt-pcr",
            "Resourcentyp Patient",
            "    1 Beispiel(e) vom Profil",
            "    https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient"
        )

        assertThat(resultWithDoubleProf.summary).isEqualTo(expectedResult)
    }
}
