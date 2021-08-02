package science.numcompass.conformance.fhir.rest

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotIn
import assertk.assertions.isTrue
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.validation.ResultSeverityEnum.ERROR
import ca.uhn.fhir.validation.ResultSeverityEnum.FATAL
import org.hamcrest.CoreMatchers.containsString
import org.hl7.fhir.r4.model.OperationOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.JpaConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import science.numcompass.conformance.fhir.configuration.R4Configuration
import science.numcompass.conformance.fhir.rest.ValidationIT.TestConfig
import science.numcompass.conformance.fhir.service.JobExecutor
import java.util.*
import java.util.concurrent.Future

@DirtiesContext
@ActiveProfiles("h2", "test")
@WebMvcTest(ValidationRestResource::class)
@SpringJUnitWebConfig(classes = [TestConfig::class], initializers = [ConfigDataApplicationContextInitializer::class])
class ValidationIT {

    @Configuration
    @Import(DataSourceAutoConfiguration::class, JpaConfiguration::class, R4Configuration::class)
    class TestConfig {
        @Bean
        fun applicationTaskExecutor() = object : ThreadPoolTaskExecutor() {

            init {
                corePoolSize = 4
            }

            override fun submit(task: Runnable): Future<*> {
                task.run()
                return super.submit(task)
            }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var fhirContext: FhirContext

    @Autowired
    lateinit var jobExecutor: JobExecutor

    @BeforeEach
    fun setup() {
        jobExecutor.clearCache()
    }

    //region validation chain tests

    @Autowired
    lateinit var validationSupportChain: IValidationSupport

    @Test
    fun testUnknownCodeSystemIsSupported() {
        val context = ValidationSupportContext(validationSupportChain)

        val supported = validationSupportChain.isCodeSystemSupported(context, "http://loincXYZ.org")

        assertThat(supported).isTrue()
    }

    //endregion

    //region REST tests

    @Test
    fun testListConformanceResources() {
        mockMvc.perform(get("/fhir/\$list-conformance-resources"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("http://hl7.org/fhir/StructureDefinition/code")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["<html>", "{}"])
    fun validateInvalidResource(resource: String) {
        val response = postToEndpoint("/fhir/\$validate-single", resource)
            .andExpect(status().isOk)
            .andReturn().response
        val operationOutcome = response.toOperationOutcome()
        assertThat(operationOutcome.issue).hasSize(1)
        assertThat(operationOutcome.issue[0].severity).isEqualTo(OperationOutcome.IssueSeverity.FATAL)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", ""])
    fun validateSingleEmptyResource(resource: String) {
        postToEndpoint("/fhir/\$validate-single", resource)
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Coverage-example.json",
            "/Coverage-example.xml",
            "/Example-pulsoximetrische-sauerstoffsaettigung.json",
            "/Example-pulsoximetrische-sauerstoffsaettigung.xml",
            "/ChargeItem-example.json",
            "/ChargeItem-example.xml"
        ]
    )
    fun validateSingleResource(path: String) {
        val resource = loadClassPathResource(path)

        val response = postToEndpoint("/fhir/\$validate-single", resource)
            .andExpect(status().isOk)
            .andReturn().response

        checkOperationOutcome(response.toOperationOutcome())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Coverage-example.json",
            "/Coverage-example.xml",
            "/Example-pulsoximetrische-sauerstoffsaettigung.json",
            "/Example-pulsoximetrische-sauerstoffsaettigung.xml",
            "/ChargeItem-example.json",
            "/ChargeItem-example.xml"
        ]
    )
    fun validateSingeResourceWithPlainContentType(path: String) {
        val resource = loadClassPathResource(path)

        val response = postToEndpoint("/fhir/\$validate-single", resource, true)
            .andExpect(status().isOk)
            .andReturn().response

        checkOperationOutcome(response.toOperationOutcome())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Coverage-example.json",
            "/Coverage-example.xml",
            "/Example-pulsoximetrische-sauerstoffsaettigung.json",
            "/Example-pulsoximetrische-sauerstoffsaettigung.xml",
            "/ChargeItem-example.json",
            "/ChargeItem-example.xml"
        ]
    )
    fun validateSingleResourceAsync(path: String) {
        val resource = loadClassPathResource(path)

        val response = postToEndpoint("/fhir/\$validate-single", resource, async = true)
                .andExpect(status().isOk)
                .andReturn().response

        assertThat(response.status).isEqualTo(HttpStatus.OK.value())

        val jobId = UUID.fromString(response.contentAsString.replace("\"", ""))

        val result = mockMvc.perform(
            get("/fhir/\$validate-single/{id}", jobId)
                .accept(mediaType(resource, false))
        ).andExpect(status().isOk)
            .andReturn().response

        checkOperationOutcome(result.toOperationOutcome())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/Coverage-example.json",
            "/Coverage-example.xml",
            "/Example-pulsoximetrische-sauerstoffsaettigung.json",
            "/Example-pulsoximetrische-sauerstoffsaettigung.xml",
            "/ChargeItem-example.json",
            "/ChargeItem-example.xml"
        ]
    )
    fun validateResourceWithPlainTypeAsync(path: String) {
        val resource = loadClassPathResource(path)

        val response = postToEndpoint(
            "/fhir/\$validate-single",
            resource,
            usePlainType = true,
            async = true
        ).andExpect(status().isOk)
            .andReturn().response

        assertThat(response.status).isEqualTo(HttpStatus.OK.value())

        val jobId = UUID.fromString(response.contentAsString.replace("\"", ""))

        val result = mockMvc.perform(
            get("/fhir/\$validate-single/{id}", jobId)
                .accept(mediaType(resource, true))
        ).andExpect(status().isOk)
            .andReturn().response

        checkOperationOutcome(result.toOperationOutcome())
    }

    private fun MockHttpServletResponse.toOperationOutcome(): OperationOutcome {
        val resourceString = contentAsString
        val parser = if (mediaType(resourceString) == APPLICATION_FHIR_XML) {
            fhirContext.newXmlParser()
        } else {
            fhirContext.newJsonParser()
        }
        return parser.parseResource(OperationOutcome::class.java, resourceString)
    }

    private fun postToEndpoint(
        url: String,
        resource: String,
        usePlainType: Boolean = false,
        async: Boolean = false
    ): ResultActions {
        val mediaType = mediaType(resource, usePlainType)
        val post = post(url)
            .contentType(mediaType)
            .content(resource)
            .accept(if (async) APPLICATION_JSON else mediaType)
            .apply { if (async) header("prefer", "respond-async") }

        return mockMvc.perform(post)
    }

    private fun checkOperationOutcome(operationOutcome: OperationOutcome) {
        operationOutcome.issue.forEach {
            assertThat(it.severityElement.value.toCode()).isNotIn(ERROR.code, FATAL.code)
        }
    }

    private fun mediaType(resource: String, usePlainType: Boolean = false) =
        if (resource.startsWith("<")) {
            if (usePlainType) MediaType.APPLICATION_XML else APPLICATION_FHIR_XML
        } else {
            if (usePlainType) APPLICATION_JSON else APPLICATION_FHIR_JSON
        }

    //endregion

    private fun loadClassPathResource(path: String) = javaClass.getResource(path)!!.readText()
}
