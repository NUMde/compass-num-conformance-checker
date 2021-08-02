package science.numcompass.conformance.compass.service

import mu.KotlinLogging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.stereotype.Service
import science.numcompass.conformance.compass.engine.CompassConformanceEngine
import science.numcompass.conformance.compass.engine.isSuccessful
import science.numcompass.conformance.compass.report.CertificateGenerator
import science.numcompass.conformance.compass.report.extractCertificateData
import science.numcompass.conformance.fhir.service.JobExecutor
import java.util.*

private val logger = KotlinLogging.logger { }

@Service
class GeccoConformanceService(
    private val compassConformanceEngine: CompassConformanceEngine,
    private val certificateGenerator: CertificateGenerator,
    private val jobExecutor: JobExecutor
) {

    fun checkConformance(bundle: Bundle): OperationOutcome {
        return compassConformanceEngine.checkConformance(bundle)
    }

    /**
     * Executes the validation in a background job and return the job ID
     *
     * @param bundle a GECCO [Bundle]
     * @param resourceCacheKey the key used for the cache
     *
     * @return the job ID
     */
    fun checkConformanceAsync(bundle: Bundle, resourceCacheKey: String) = jobExecutor.execute(resourceCacheKey) {
        logger.info { "Starting GECCO conformance job" }

        val operationOutcome = checkConformance(bundle)

        if (!operationOutcome.isSuccessful()) {
            operationOutcome
        } else {
            generatePdfResponse(bundle)
        }
    }

    fun getResult(id: UUID): Any? = jobExecutor.getResult(id)

    fun generatePdfResponse(bundle: Bundle): ByteArray {
        val certificateData = extractCertificateData(bundle)
        return certificateGenerator.generateCertificate(certificateData)
    }
}
