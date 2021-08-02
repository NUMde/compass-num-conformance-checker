package science.numcompass.conformance.compass.report

import ca.uhn.fhir.jpa.dao.data.ITermCodeSystemDao
import mu.KotlinLogging
import org.hl7.fhir.r4.model.CodeSystem
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import science.numcompass.conformance.fhir.chain.ValidationChain
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

private val logger = KotlinLogging.logger { }

private const val LINES_ON_FIRST_PAGE = 36
private const val LINES_ON_TEMPLATE_PAGE = 41

@Service
class CodeSystemReportGenerator(
    private val validationChain: ValidationChain,
    private val codeSystemDao: ITermCodeSystemDao,
    buildProperties: BuildProperties?,
    @Value("\${compass.codesystem-report-template}") private val template: Resource
) {

    lateinit var codeSystems: List<String>

    private val applicationVersion = buildProperties?.version ?: "SNAPSHOT"

    /** A weak reference allows the GC to collect the report, we can recreate it if necessary */
    private var report: WeakReference<ByteArray>? = null

    /**
     * Collect code system information after the application has started.
     */
    @Order
    @EventListener(ContextRefreshedEvent::class)
    fun initialize() {
        codeSystems = (getCodeSystemUrlsFromRootValidator() + getCodeSystemUrlsFromDatabase())
            .filter { !(it.startsWith("http://hl7.org/fhir/") || it.startsWith("http://terminology.hl7.org/CodeSystem/")) }
            .distinct()
            .sorted()
    }

    /**
     * Provides the report; recreates it if necessary.
     */
    fun getReport(): ByteArray {
        if (report?.get() == null) {
            report = WeakReference(generateReport())
        }
        return report?.get() ?: throw ReportError("Code system report is null")
    }

    private fun generateReport(): ByteArray {
        val pageContents = splitIntoPages(codeSystems, LINES_ON_FIRST_PAGE, LINES_ON_TEMPLATE_PAGE)
        val outputStream = ByteArrayOutputStream()

        logger.info { "Creating code system report with ${pageContents.size} page(s)" }

        pdf(template.inputStream) {
            form {
                field("certversion")(applicationVersion)
                field("pageCurrent")("1")
                field("pageTotal")("${pageContents.size}")
                field("codesystems")(pageContents[0].joinToString("\n"))
            }

            val page2Template = pages[1]
            for (page in 2..pageContents.size) {
                val newPage = page2Template.copy()

                form {
                    field("certversion").copy(newPage, "certversion-$page", applicationVersion)
                    field("pageCurrent").copy(newPage, "pageCurrent-$page", "$page")
                    field("pageTotal").copy(newPage, "pageTotal-$page", "${pageContents.size}")
                    field("codesystems").copy(newPage, "codesystems-$page", pageContents[page - 1].joinToString("\n"))
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

    private fun getCodeSystemUrlsFromRootValidator() = (validationChain.fetchAllConformanceResources() ?: emptyList())
        .filterIsInstance<CodeSystem>()
        .map { it.url }

    private fun getCodeSystemUrlsFromDatabase() = codeSystemDao.findAll()
        .map { it.codeSystemUri }

}
