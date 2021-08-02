package science.numcompass.conformance.fhir.configuration

import ca.uhn.fhir.jpa.dao.data.ITermCodeSystemDao
import ca.uhn.fhir.jpa.term.TermVersionAdapterSvcR4
import ca.uhn.fhir.jpa.term.api.ITermLoaderSvc
import ca.uhn.fhir.jpa.term.api.ITermLoaderSvc.FileDescriptor
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val logger = KotlinLogging.logger { }

@Configuration
@AutoConfigureBefore(PackageConfiguration::class)
class CodeSystemConfiguration(
    private val appProperties: AppProperties,
    private val termLoaderService: ITermLoaderSvc,
    private val applicationContext: ApplicationContext
) {

    /**
     * Code systems will be installed as soon as the application is started and before any packages are installed.
     */
    @Order(Ordered.LOWEST_PRECEDENCE - 50)
    @EventListener
    fun initialize(event: ContextRefreshedEvent) {
        applicationContext.getBean(TermVersionAdapterSvcR4::class.java).start(event)
        val codeSystemDao = applicationContext.getBean(ITermCodeSystemDao::class.java)

        // install LOINC
        appProperties.codeSystems?.loinc?.also { loincFile ->
            if (codeSystemDao.findByCodeSystemUri(ITermLoaderSvc.LOINC_URI) == null) {
                processZipFile(loincFile) {
                    termLoaderService.loadLoinc(getDescriptors(it), null)
                }
            }
        }

        // install SNOMED CT
        appProperties.codeSystems?.snomed?.also { snomedCtFile ->
            if (codeSystemDao.findByCodeSystemUri(ITermLoaderSvc.SCT_URI) == null) {
                processZipFile(snomedCtFile) {
                    termLoaderService.loadSnomedCt(getDescriptors(it), null)
                }
            }
        }
    }

    private fun processZipFile(path: String, handler: (ZipFile) -> Unit) {
        val file = File(path)
        if (file.exists()) {
            val zipFile = ZipFile(file)
            handler(zipFile)
        } else {
            logger.error { "Code system file $path not found!" }
        }
    }

    private fun getDescriptors(zipFile: ZipFile) = zipFile.entries().toList()
        .filter { !it.isDirectory }
        .map { ZipFileDescriptor(zipFile, it) }

    internal class ZipFileDescriptor(private val zipFile: ZipFile, private val entry: ZipEntry) : FileDescriptor {

        override fun getFilename(): String = entry.name

        override fun getInputStream(): InputStream = zipFile.getInputStream(entry)
    }
}
