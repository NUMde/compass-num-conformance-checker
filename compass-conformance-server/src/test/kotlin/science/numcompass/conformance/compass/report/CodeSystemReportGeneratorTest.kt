package science.numcompass.conformance.compass.report

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import ca.uhn.fhir.jpa.dao.data.ITermCodeSystemDao
import ca.uhn.fhir.jpa.entity.TermCodeSystem
import org.hl7.fhir.r4.model.CodeSystem
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.core.io.ClassPathResource
import science.numcompass.conformance.fhir.chain.ValidationChain
import java.io.ByteArrayInputStream

internal class CodeSystemReportGeneratorTest {

    private val template = ClassPathResource("/report/codesystems.pdf", javaClass)

    private val validationChainMock = mock<ValidationChain> {
        val codeSystemMock = mock<CodeSystem> {
            on { url } doReturn "b"
        }
        on { fetchAllConformanceResources() } doReturn listOf(codeSystemMock)
    }

    private val daoMock = mock<ITermCodeSystemDao> {
        val codeSystemMock = mock<TermCodeSystem> {
            on { codeSystemUri } doReturn "a"
        }
        on { findAll() } doReturn listOf(codeSystemMock)
    }

    @Test
    fun testGetCodeSystems() {
        val service = CodeSystemReportGenerator(validationChainMock, daoMock, null, template)
        service.initialize()

        assertThat(service.codeSystems).isNotEmpty()
        assertThat(service.codeSystems).containsExactly("a", "b")
    }

    @Test
    fun testUninitializedGetThrowsException() {
        val service = CodeSystemReportGenerator(validationChainMock, daoMock, null, template)

        assertThat { service.codeSystems }.isFailure()
            .isInstanceOf(UninitializedPropertyAccessException::class)
    }

    @Test
    fun testGenerateReport() {
        val service = CodeSystemReportGenerator(mock { }, mock { }, null, template)
        service.codeSystems = (1..100).map { "$it" }

        val rawPdf = service.getReport()

        assertThat(rawPdf).isNotEmpty()

        val pdf = pdf(ByteArrayInputStream(rawPdf))
        assertThat(pdf.pages.count).isEqualTo(3)
    }
}
