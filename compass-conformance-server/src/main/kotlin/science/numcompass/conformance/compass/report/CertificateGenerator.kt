package science.numcompass.conformance.compass.report

import ca.uhn.fhir.jpa.dao.data.INpmPackageVersionDao
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import science.numcompass.conformance.compass.report.invoke
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger { }

private const val LINES_ON_FIRST_PAGE = 13
private const val LINES_ON_TEMPLATE_PAGE = 41

@Service
class CertificateGenerator(
    @Value("\${compass.certificate-template}") private val template: Resource,
    buildProperties: BuildProperties?,
    private val packageDao: INpmPackageVersionDao
) {

    private val applicationVersion = buildProperties?.version ?: "SNAPSHOT"
    private var geccoVersion: String = "1.0.4"

    /**
     * Read the installed GECCO package version from the database.
     */
    @Order
    @EventListener(ContextRefreshedEvent::class)
    fun initialize() {
        packageDao.findByPackageId("de.gecco")
            .map { it.versionId }
            .maxByOrNull { it }
            ?.also { geccoVersion = it }
    }

    /**
     * Generates the multi-page PDF certificate based on the report.
     */
    fun generateCertificate(report: CertificateData): ByteArray {
        val pageContents = splitIntoPages(report.summary, LINES_ON_FIRST_PAGE, LINES_ON_TEMPLATE_PAGE)
        val outputStream = ByteArrayOutputStream()

        logger.info { "Creating certificate with ${pageContents.size} page(s)" }

        pdf(template.inputStream) {
            form {
                field("amountResources")("${report.resourceCounter}")
                field("appName")(report.appName)
                field("appPublisher")(report.manufacturer ?: "")
                field("appVersion")(report.appVersion)
                field("certgeccoversion")(geccoVersion)
                field("certversion")(applicationVersion)
                field("checkDate")(report.testDate.toString())
                field("checkedResources")(pageContents[0].joinToString("\n"))
                field("organization")(report.orgData)
                field("pageCurrent")("1")
                field("pageTotal")("$numberOfPages")
            }

            val page2Template = pages[1]
            for (page in 2..pageContents.size) {
                val newPage = page2Template.copy()
                val checkedResources = pageContents[page - 1].joinToString("\n")

                form {
                    field("pageCurrent").copy(newPage, "pageCurrent-$page", "$page")
                    field("pageTotal").copy(newPage, "pageTotal-$page", "$numberOfPages")
                    field("certversion").copy(newPage, "certversion-$page", applicationVersion)
                    field("checkedResources").copy(newPage, "checkedResources-$page", checkedResources)
                }

                addPage(newPage)
            }
            removePage(page2Template)

            form {
                flatten()
            }
        }.use { it.save(outputStream) }

        return outputStream.toByteArray()
    }
}
