package science.numcompass.conformance.compass.report

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.boot.info.BuildProperties
import org.springframework.core.io.ClassPathResource
import science.numcompass.conformance.compass.loadClassPathResource
import java.io.ByteArrayInputStream
import java.time.LocalDate

internal class CertificateGeneratorTest {

    private val template = ClassPathResource("/report/certificate.pdf")
    private val buildProperties = mock<BuildProperties> {
        on { version } doReturn "1.0.0"
    }
    private val service = CertificateGenerator(template, buildProperties, mock { })

    @Test
    fun generateCertificate() {
        val json = loadClassPathResource("/Bundle-conf-test-input-bundle.json")
        val bundle = FhirContext.forR4().newJsonParser().parseResource(json) as Bundle
        val certificateData = extractCertificateData(bundle)

        val rawPdf = service.generateCertificate(certificateData)

        assertThat(rawPdf).isNotEmpty()
        assertThat(String(rawPdf)).contains("(1.0.0)")

        val pdf = pdf(ByteArrayInputStream(rawPdf))
        assertThat(pdf.pages.count).isEqualTo(1)
    }

    @Test
    fun generateMultiPageCertificate() {
        val summary = (1..100).map { "$it" }.toMutableList()
        val report = CertificateData("org", "app", "version", LocalDate.now(), 100, 100, summary)

        val rawPdf = service.generateCertificate(report)

        assertThat(rawPdf).isNotEmpty()
        assertThat(String(rawPdf)).contains("(1.0.0)")

        val pdf = pdf(ByteArrayInputStream(rawPdf))
        assertThat(pdf.pages.count).isEqualTo(4)
    }
}
