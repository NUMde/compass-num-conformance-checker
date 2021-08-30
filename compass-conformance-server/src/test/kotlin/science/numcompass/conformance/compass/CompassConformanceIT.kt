package science.numcompass.conformance.compass

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.fhir.context.FhirContext
import mu.KotlinLogging
import org.hamcrest.CoreMatchers
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.FATAL
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import science.numcompass.conformance.compass.CompassConformanceIT.TestConfig
import science.numcompass.conformance.compass.chain.LoincSnomedCtWarningSupport
import science.numcompass.conformance.compass.configuration.CompassConfiguration
import science.numcompass.conformance.compass.engine.CompassConformanceEngine
import science.numcompass.conformance.compass.engine.TARGET_GECCO_PROFILE_EXTENSION_URL
import science.numcompass.conformance.compass.engine.isSuccessful
import science.numcompass.conformance.compass.rest.GeccoConformanceRestResource
import science.numcompass.conformance.compass.service.GeccoConformanceService
import science.numcompass.conformance.fhir.configuration.R4Configuration
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_JSON
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_XML
import science.numcompass.conformance.fhir.service.JobExecutor
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

fun loadClassPathResource(path: String) = CompassConformanceIT::class.java.getResource(path)!!.readText()

private val logger = KotlinLogging.logger { }

@DirtiesContext
@ActiveProfiles("h2", "test", "compass")
@WebMvcTest(GeccoConformanceRestResource::class)
@SpringJUnitWebConfig(classes = [TestConfig::class], initializers = [ConfigDataApplicationContextInitializer::class])
class CompassConformanceIT {

    @Configuration
    @ComponentScan("org.springframework.boot.autoconfigure.orm.jpa")
    @Import(DataSourceAutoConfiguration::class, R4Configuration::class, CompassConfiguration::class)
    class TestConfig {

        @Primary
        @Bean
        fun applicationTaskExecutor() = object : ThreadPoolTaskExecutor() {

            init {
                corePoolSize = 4
            }

            override fun submit(task: Runnable): Future<*> {
                task.run()
                return CompletableFuture.completedFuture(Unit)
            }
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE - 650)
        fun loincSnomedCtWarningSupport(context: FhirContext) = LoincSnomedCtWarningSupport(context)
    }

    @Autowired
    lateinit var fhirContext: FhirContext

    @Autowired
    lateinit var jobExecutor: JobExecutor

    @BeforeEach
    fun setup() {
        jobExecutor.clearCache()
    }

    //region CompassConformanceEngine tests

    @Autowired
    lateinit var conformanceEngine: CompassConformanceEngine

    private fun validTestBundleNoQuestionnaires() = loadClassPathBundle("/Bundle-conf-test-input-bundle.json")
    private fun validTestBundleWithQuestionnaire() = loadClassPathBundle("/Bundle-conf-test-input-bundle-quest.json")
    private fun doubleProfileObs() = loadClassPathResource("/double-gecco-profile-observation.json")
        .let { fhirContext.newJsonParser().parseResource(it) as Observation }

    @Test
    fun whenInputBundleNoQuestionnaireIsValid_noValidationErrorOccurs() {
        val result = conformanceEngine.checkConformance(validTestBundleNoQuestionnaires())

        result.logOperationErrors()
        assertThat(result.isSuccessful()).isTrue()
    }

    @Test
    fun whenInputBundleWithQuestionnaireIsValid_noValidationErrorOccurs() {
        val result = conformanceEngine.checkConformance(validTestBundleWithQuestionnaire())

        result.logOperationErrors()
        assertThat(result.isSuccessful()).isTrue()
    }

    @Test
    fun whenInputBundleIsValid_butContainsNoQuestionnaireResources_noValidationErrorOccurs() {
        val result = conformanceEngine.checkConformance(validTestBundleNoQuestionnaires())

        result.logOperationErrors()
        assertThat(result.isSuccessful()).isTrue()
    }

    @Test
    fun whenInputIsNotAFhirDocumentBundle_aValidationErrorOccurs() {
        val nonDocumentBundle = validTestBundleWithQuestionnaire()
        nonDocumentBundle.type = Bundle.BundleType.BATCH

        val result = conformanceEngine.checkConformance(nonDocumentBundle)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTheCompositionResourceIsInvalid_aValidationErrorOccurs() {
        val testBundleInvalidDevice = validTestBundleWithQuestionnaire()
        // Change Composition resource so it does not conform to put profile
        (testBundleInvalidDevice.entry.first().resource as Composition).type.codingFirstRep.code = "11485-0"

        val result = conformanceEngine.checkConformance(testBundleInvalidDevice)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenDeviceResourceForAppIsMissingFromBundle_aValidationErrorOccurs() {
        val testBundleNoDevice = validTestBundleWithQuestionnaire()
        // Remove Device resource with app information
        testBundleNoDevice.entry.removeAt(1)

        val result = conformanceEngine.checkConformance(testBundleNoDevice)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenDeviceResourceForAppIsMissingFromComposition_aValidationErrorOccurs() {
        val testBundleNoDeviceInComp = validTestBundleWithQuestionnaire()
        (testBundleNoDeviceInComp.entry.first().resource as Composition).section.removeAt(0)

        val result = conformanceEngine.checkConformance(testBundleNoDeviceInComp)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenDeviceResourceForAppIsMissingFromBundleAndComposition_aValidationErrorOccurs() {
        val testBundleNoDeviceAnywhere = validTestBundleWithQuestionnaire()
        testBundleNoDeviceAnywhere.entry.removeAt(1)
        (testBundleNoDeviceAnywhere.entry.first().resource as Composition).section.removeAt(0)

        val result = conformanceEngine.checkConformance(testBundleNoDeviceAnywhere)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenDeviceResourceForAppIsInvalid_aValidationErrorOccurs() {
        val testBundleInvalidDevice = validTestBundleWithQuestionnaire()
        // Change Device resource so it does not conform to put profile
        (testBundleInvalidDevice.entry[1].resource as Device).deviceName = null

        val result = conformanceEngine.checkConformance(testBundleInvalidDevice)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenMultipleDeviceResourcesForAppAreIncludes_aValidationErrorOccurs() {
        val testBundleMultipleDevices = validTestBundleWithQuestionnaire()
        // Duplicate and insert Device resource with app information
        val deviceResourceEntry = testBundleMultipleDevices.entry[1]
        val newUuid = "urn:uuid:${UUID.randomUUID()}"
        deviceResourceEntry.fullUrl = newUuid
        testBundleMultipleDevices.entry.add(2, deviceResourceEntry)
        // Add extra Device to the app section of the Composition
        (testBundleMultipleDevices.entry.first().resource as Composition).section.first().entry.add(Reference(newUuid))

        val result = conformanceEngine.checkConformance(testBundleMultipleDevices)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenOrganizationResourceForPublishingOrgIsMissingFromBundle_aValidationErrorOccurs() {
        val testBundleNoOrg = validTestBundleWithQuestionnaire()
        // Remove Organization resource with app publisher information
        testBundleNoOrg.entry.removeAt(2)

        val result = conformanceEngine.checkConformance(testBundleNoOrg)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenOrganizationResourceForPublishingOrgIsMissingFromComposition_aValidationErrorOccurs() {
        val testBundleNoOrgInComp = validTestBundleWithQuestionnaire()
        (testBundleNoOrgInComp.entry.first().resource as Composition).section.removeAt(1)

        val result = conformanceEngine.checkConformance(testBundleNoOrgInComp)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenOrganizationResourceForPublishingOrgIsMissingFromBundleAndComposition_aValidationErrorOccurs() {
        val testBundleNoOrgAnywhere = validTestBundleWithQuestionnaire()
        (testBundleNoOrgAnywhere.entry.first().resource as Composition).section.removeAt(1)
        testBundleNoOrgAnywhere.entry.removeAt(2)

        val result = conformanceEngine.checkConformance(testBundleNoOrgAnywhere)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenOrganizationResourceForPublishingOrgIsInvalid_aValidationErrorOccurs() {
        val testBundleInvalidOrg = validTestBundleWithQuestionnaire()
        // Change Organization resource so it does not conform to our profile
        (testBundleInvalidOrg.entry[2].resource as Organization).name = null

        val result = conformanceEngine.checkConformance(testBundleInvalidOrg)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenMultipleOrganizationResourcesForPublishingOrgAreIncluded_aValidationErrorOccurs() {
        val testBundleMultipleOrgs = validTestBundleWithQuestionnaire()
        // Duplicate and insert Organization resource with app information
        val orgResourceEntry = testBundleMultipleOrgs.entry[2]
        val newUuid = "urn:uuid:${UUID.randomUUID()}"
        orgResourceEntry.fullUrl = newUuid
        testBundleMultipleOrgs.entry.add(2, orgResourceEntry)
        // Add extra Organization to the app section of the Composition
        (testBundleMultipleOrgs.entry.first().resource as Composition).section[1].entry.add(Reference(newUuid))

        val result = conformanceEngine.checkConformance(testBundleMultipleOrgs)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTestDataResourcesAreMissingFromBundle_aValidationErrorOccurs() {
        val testBundleNoTestData = validTestBundleWithQuestionnaire()
        // Remove the test data resources (last three entries)
        testBundleNoTestData.entry.removeLast()
        testBundleNoTestData.entry.removeLast()
        testBundleNoTestData.entry.removeLast()

        val result = conformanceEngine.checkConformance(testBundleNoTestData)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTestDataResourcesAreMissingFromComposition_aValidationErrorOccurs() {
        val testBundleNoTestDataInComp = validTestBundleWithQuestionnaire()
        (testBundleNoTestDataInComp.entry.first().resource as Composition).section.removeLast()

        val result = conformanceEngine.checkConformance(testBundleNoTestDataInComp)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTestDataResourcesCompositionSectionListsNoResources_aValidationErrorOccurs() {
        val testBundleNoTestDataAnywhere = validTestBundleWithQuestionnaire()
        // Leave Composition section for test data but do not link any resources
        (testBundleNoTestDataAnywhere.entry.first().resource as Composition).section.last().entry = null
        // Remove the test data resources (last three entries)
        testBundleNoTestDataAnywhere.entry.removeLast()
        testBundleNoTestDataAnywhere.entry.removeLast()
        testBundleNoTestDataAnywhere.entry.removeLast()

        val result = conformanceEngine.checkConformance(testBundleNoTestDataAnywhere)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTestDataResourcesAreMissingFromBundleAndComposition_aValidationErrorOccurs() {
        val testBundleNoTestDataAnywhere = validTestBundleWithQuestionnaire()
        (testBundleNoTestDataAnywhere.entry.first().resource as Composition).section.removeLast()
        // Remove the test data resources (last three entries)
        testBundleNoTestDataAnywhere.entry.removeLast()
        testBundleNoTestDataAnywhere.entry.removeLast()
        testBundleNoTestDataAnywhere.entry.removeLast()

        val result = conformanceEngine.checkConformance(testBundleNoTestDataAnywhere)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenATestDataResourceIsInvalid_aValidationErrorOccurs() {
        val testBundleInvalidTestResource = validTestBundleWithQuestionnaire()
        // Change a test Resource resource so it does not conform to its declared GECCO profile
        val observation = testBundleInvalidTestResource.entry.last().resource as Observation
        observation.valueCodeableConcept.coding[0].apply {
            system = "http://fhir.de/CodeSystem/dimdi/atc"
            code = "260407003"
            display = "Weak (qualifier value)"
        }

        val result = conformanceEngine.checkConformance(testBundleInvalidTestResource)

        assertThat(result.isSuccessful()).isFalse()
    }

    @Test
    fun whenTestDataResourceIsReferencesInComposition_butDoesNotDeclareAGeccoProfile_aValidationErrorOccurs() {
        val testBundleInvalidTestResource = validTestBundleWithQuestionnaire()
        // Remove the GECCO profile from the last test resource
        (testBundleInvalidTestResource.entry.last().resource as Observation).meta = null

        val result = conformanceEngine.checkConformance(testBundleInvalidTestResource)

        val expectedErrorString = "In the test data section of the Composition resource, the resource with index " +
            "2 does not declare a GECCO profile"
        assertThat(result.isSuccessful()).isFalse()
        assertThat(result.errorDiagnosticsContainsString(expectedErrorString)).isTrue()
    }

    @Test
    fun whenAGeccoProfileIsNotUsedInQuestionnaire_butIsDeclaredInTestData_noValidationErrorOccurs() {
        val testBundleAdditionalGeccoProfile = validTestBundleWithQuestionnaire()
        // Replace second observation resource with one that declares GECCO profiles not used in Questionnaire
        testBundleAdditionalGeccoProfile.entry.last().resource = doubleProfileObs()

        val result = conformanceEngine.checkConformance(testBundleAdditionalGeccoProfile)

        result.logOperationErrors()
        assertThat(result.isSuccessful()).isTrue()
    }

    @Test
    fun whenAGeccoProfileIsUsedInQuestionnaireTopLevelItem_butDoesNotOccurInTestData_aValidationErrorOccurs() {
        val testBundleUnusedGeccoProfile = validTestBundleWithQuestionnaire()
        // Add further GECCO target profile extension to top-level item
        val targetProfileExt = Extension().apply { url = TARGET_GECCO_PROFILE_EXTENSION_URL }.setValue(
            CanonicalType("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-pressure")
        )
        (testBundleUnusedGeccoProfile.entry[3].resource as Questionnaire).item.first().extension.add(targetProfileExt)

        val result = conformanceEngine.checkConformance(testBundleUnusedGeccoProfile)

        val expectedErrorStr = "GECCO profile declared in Questionnaire was not declared in any of the top-level " +
            "resource: https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-pressure"
        assertThat(result.isSuccessful()).isFalse()
        assertThat(result.errorDiagnosticsContainsString(expectedErrorStr)).isTrue()
    }

    @Test
    fun whenAGeccoProfileIsUsedInQuestionnaireNestedItem_butDoesNotOccurInTestData_aValidationErrorOccurs() {
        val testBundleUnusedGeccoProfile = validTestBundleWithQuestionnaire()
        // Add further GECCO target profile extension to nested item
        val targetProfileExt = Extension().apply { url = TARGET_GECCO_PROFILE_EXTENSION_URL }.setValue(
            CanonicalType("https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-pressure")
        )
        (testBundleUnusedGeccoProfile.entry[3].resource as Questionnaire).item[1].item.first().extension.add(
            targetProfileExt
        )

        val result = conformanceEngine.checkConformance(testBundleUnusedGeccoProfile)

        val expectedErrorStr = "GECCO profile declared in Questionnaire was not declared in any of the top-level " +
            "resource: https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/blood-pressure"
        assertThat(result.isSuccessful()).isFalse()
        assertThat(result.errorDiagnosticsContainsString(expectedErrorStr)).isTrue()
    }

    //endregion

    //region GeccoConformanceService tests

    @Autowired
    lateinit var conformanceService: GeccoConformanceService

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Bundle-conf-test-input-bundle.json",
            "/Bundle-conf-test-input-bundle-quest.json"
        ]
    )
    fun validBundle_causesNoErrors(path: String) {
        val bundle = loadClassPathBundle(path)

        val validationResult = conformanceService.checkConformance(bundle)

        assertThat(validationResult.isSuccessful()).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Bundle-conf-test-input-bundle.json",
            "/Bundle-conf-test-input-bundle-quest.json"
        ]
    )
    fun validBundle_async_givesPdfResult(path: String) {
        val bundle = loadClassPathBundle(path)

        val id = conformanceService.checkConformanceAsync(bundle, UUID.randomUUID().toString())

        assertThat(id).isNotNull()
        conformanceService.getResult(id).also {
            assertThat(it).isNotNull()
            assertThat(it is ByteArray).isTrue()
        }
    }

    //endregion

    //region REST tests

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun testMetadata() {
        mockMvc.perform(get("/fhir/metadata").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().string(CoreMatchers.containsString("CapabilityStatement")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["<html>", "{}"])
    fun validateInvalidResource(resource: String) {
        postToEndpoint("/fhir/\$check-gecco-conformance", resource)
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", ""])
    fun completelyInvalidInput_isRejectedAsBadRequests(resource: String) {
        postToEndpoint("/fhir/\$check-gecco-conformance", resource)
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Bundle-conf-test-input-bundle.json",
            "/Bundle-conf-test-input-bundle-quest.json"
        ]
    )
    fun validInputBundle_returnsStatus200_andReturnsPdf(path: String) {
        val resource = loadClassPathResource(path)

        postToEndpoint("/fhir/\$check-gecco-conformance", resource)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/pdf"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Bundle-conf-test-input-bundle.json",
            "/Bundle-conf-test-input-bundle-quest.json"
        ]
    )
    fun validInputBundle_async(path: String) {
        val resource = loadClassPathResource(path)

        val response = postToEndpoint("/fhir/\$check-gecco-conformance", resource) {
            header("prefer", "respond-async")
        }.andExpect(status().isOk)
            .andReturn().response

        val jobId = UUID.fromString(response.contentAsString.replace("\"", ""))

        mockMvc.perform(get("/fhir/\$check-gecco-conformance/{id}", jobId).accept(APPLICATION_PDF_VALUE))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", APPLICATION_PDF_VALUE))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Invalid-bundle-conf-test-input-bundle.json",
            "/Invalid-bundle-conf-test-input-bundle-quest.json"
        ]
    )
    fun invalidInputBundle_returnsStatus200_andReturnsOperationOutcome(path: String) {
        val resource = loadClassPathResource(path)

        postToEndpoint("/fhir/\$check-gecco-conformance", resource)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/fhir+json"))
            .andExpect(content().string(CoreMatchers.containsString("OperationOutcome")))
    }

    @Test
    fun testGetCodeSystemReport() {
        mockMvc.perform(get("/fhir/codesystems").accept(APPLICATION_PDF_VALUE))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", APPLICATION_PDF_VALUE))
    }

    private fun postToEndpoint(
        url: String,
        resource: String,
        usePlainType: Boolean = false,
        customizer: MockHttpServletRequestBuilder.() -> Unit = {}
    ): ResultActions {
        val mediaType = mediaType(resource, usePlainType)
        val post = MockMvcRequestBuilders.post(url)
            .contentType(mediaType)
            .content(resource)
            .accept(mediaType)
            .apply(customizer)

        return mockMvc.perform(post)
    }

    private fun mediaType(resource: String, usePlainType: Boolean = false) =
        if (resource.startsWith("<")) {
            if (usePlainType) MediaType.APPLICATION_XML else APPLICATION_FHIR_XML
        } else {
            if (usePlainType) MediaType.APPLICATION_JSON else APPLICATION_FHIR_JSON
        }
    //endregion

    //region helper functions

    /**
     * Helper extension method for checking that a string with a certain diagnostic string is present
     */
    private fun OperationOutcome.errorDiagnosticsContainsString(str: String) =
        operationErrors().any { it.diagnostics.contains(str) }

    private fun loadClassPathBundle(path: String) =
        fhirContext.newJsonParser().parseResource(Bundle::class.java, loadClassPathResource(path))

    private fun OperationOutcome.logOperationErrors() {
        operationErrors().forEach {
            logger.error { "${it.code}, diagnostics: ${it.diagnostics}" }
            logger.error { "Details: ${it.details.text}" }
        }
    }

    private fun OperationOutcome.operationErrors() = issue.filter { it.severity == ERROR || it.severity == FATAL }

    //endregion
}
