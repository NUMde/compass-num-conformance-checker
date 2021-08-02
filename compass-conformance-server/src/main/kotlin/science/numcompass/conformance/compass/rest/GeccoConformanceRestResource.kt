package science.numcompass.conformance.compass.rest

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import science.numcompass.conformance.compass.engine.isSuccessful
import science.numcompass.conformance.compass.report.CodeSystemReportGenerator
import science.numcompass.conformance.compass.service.GeccoConformanceService
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_JSON_VALUE
import science.numcompass.conformance.fhir.rest.APPLICATION_FHIR_XML_VALUE
import java.util.*

@RestController
@RequestMapping(
    path = ["/\${fhir.rest.base-path:fhir}"],
    produces = [
        APPLICATION_JSON_VALUE,
        APPLICATION_FHIR_JSON_VALUE,
        APPLICATION_XML_VALUE,
        APPLICATION_FHIR_XML_VALUE,
        APPLICATION_PDF_VALUE
    ]
)
class GeccoConformanceRestResource(
    fhirContext: FhirContext,
    private val conformanceService: GeccoConformanceService,
    private val codeSystemReportGenerator: CodeSystemReportGenerator
) {

    private val capabilityStatement =
        javaClass.getResourceAsStream("/fhir/CapabilityStatement-conformanceServerCapabilities.json")
            .use { fhirContext.newJsonParser().parseResource(it) as CapabilityStatement }

    /**
     * FHIR capability interaction returning the CapabilityStatement resource for the server.
     */
    @GetMapping("/metadata")
    fun metadata() = capabilityStatement

    /**
     * Endpoint for retrieving the code system report PDF.
     */
    @GetMapping(path = ["/codesystems"], produces = [APPLICATION_PDF_VALUE])
    fun codeSystems() = createPdfResponse(codeSystemReportGenerator.getReport(), "codesystems")

    /**
     * Check a complete GECCO conformance bundle. The return value is a PDF stream in case of success or an
     * [OperationOutcome] in case of errors.
     */
    @PostMapping(
        path = ["/\$check-gecco-conformance"],
        consumes = [
            APPLICATION_JSON_VALUE,
            APPLICATION_FHIR_JSON_VALUE,
            APPLICATION_XML_VALUE,
            APPLICATION_FHIR_XML_VALUE
        ]
    )
    fun checkGeccoConformance(@RequestBody bundle: Bundle, @RequestAttribute md5: String): ResponseEntity<*> =
        conformanceService.checkConformance(bundle).let {
            if (it.isSuccessful()) {
                createPdfResponse(conformanceService.generatePdfResponse(bundle), "certificate")
            } else {
                ok(it)
            }
        }

    /**
     * Check a complete GECCO conformance bundle asynchronously by setting the prefer header to
     * "respond-async" - the return value is an ID that can be used to retrieve the results under
     * /$vcheck-gecco-conformance/{id}.
     */
    @PostMapping(
        path = ["/\$check-gecco-conformance"],
        headers = ["prefer=respond-async"],
        consumes = [
            APPLICATION_JSON_VALUE,
            APPLICATION_FHIR_JSON_VALUE,
            APPLICATION_XML_VALUE,
            APPLICATION_FHIR_XML_VALUE
        ],
        produces = [APPLICATION_JSON_VALUE, APPLICATION_FHIR_JSON_VALUE]
    )
    fun checkGeccoConformanceAsync(
        @RequestBody bundle: Bundle,
        @RequestAttribute md5: String
    ) = conformanceService.checkConformanceAsync(bundle, md5)

    /**
     * Retrieve the result of a FHIR validation triggered via the $check-gecco-conformance endpoint.
     */
    @GetMapping("/\$check-gecco-conformance/{id}")
    fun conformanceResult(@PathVariable id: String): ResponseEntity<*> =
        when (val result = conformanceService.getResult(UUID.fromString(id))) {
            is OperationOutcome -> ok(result)
            is ByteArray -> createPdfResponse(result, "certificate")
            null -> notFound().build<Any>()
            else -> throw IllegalArgumentException("Unsupported result ${result.javaClass}")
        }

    private fun createPdfResponse(rawData: ByteArray, fileName: String) = ok()
        .header("Content-Disposition", "attachment; filename=$fileName.pdf")
        .contentType(APPLICATION_PDF)
        .body(rawData)
}
