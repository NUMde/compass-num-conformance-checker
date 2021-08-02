package science.numcompass.conformance.fhir.rest

import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport
import org.hl7.fhir.r4.model.CodeSystem
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.r4.model.ValueSet
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
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
import science.numcompass.conformance.fhir.service.ValidationService
import java.util.*

const val APPLICATION_FHIR_JSON_VALUE = "application/fhir+json"
val APPLICATION_FHIR_JSON = MediaType("application", "fhir+json")
const val APPLICATION_FHIR_XML_VALUE = "application/fhir+xml"
val APPLICATION_FHIR_XML = MediaType("application", "fhir+xml")

@RestController
@RequestMapping(
    path = ["/\${fhir.rest.base-path:fhir}"],
    produces = [
        APPLICATION_JSON_VALUE,
        APPLICATION_FHIR_JSON_VALUE,
        APPLICATION_XML_VALUE,
        APPLICATION_FHIR_XML_VALUE
    ]
)
class ValidationRestResource(
    private val validationService: ValidationService,
    private val validationSupport: CachingValidationSupport
) {

    /**
     * List the canonical URLs of all FHIR artifacts available for validation (StructureDefinition, CodeSystem, and
     * ValueSystem resources).
     */
    @GetMapping("/\$list-conformance-resources", produces = [TEXT_PLAIN_VALUE])
    fun listConformanceResources() = (validationSupport.fetchAllConformanceResources() ?: emptyList())
        .mapNotNull {
            when (it) {
                is ValueSet -> it.url
                is CodeSystem -> it.url
                is StructureDefinition -> it.url
                else -> null
            }
        }.sorted()
        .joinToString("\n")


    /**
     * FHIR-validate a single resource.
     */
    @PostMapping(
        path = ["/\$validate-single"],
        consumes = [
            APPLICATION_JSON_VALUE,
            APPLICATION_FHIR_JSON_VALUE,
            APPLICATION_XML_VALUE,
            APPLICATION_FHIR_XML_VALUE
        ]
    )
    fun validateSingle(@RequestBody resource: String, @RequestAttribute md5: String): OperationOutcome =
        validationService.validate(resource)

    /**
     * FHIR-validate a single resource asynchronously by setting the prefer header to "respond-async" - the return
     * value is then an ID that can be used to retrieve the results under /$validate-single/{id}.
     */
    @PostMapping(
        path = ["/\$validate-single"],
        headers = ["prefer=respond-async"],
        consumes = [
            APPLICATION_JSON_VALUE,
            APPLICATION_FHIR_JSON_VALUE,
            APPLICATION_XML_VALUE,
            APPLICATION_FHIR_XML_VALUE
        ],
        produces = [APPLICATION_JSON_VALUE, APPLICATION_FHIR_JSON_VALUE]
    )
    fun validateSingleAsync(@RequestBody resource: String, @RequestAttribute md5: String) =
        validationService.validateAsync(resource, md5)

    /**
     * Retrieve the result of a FHIR validation triggered via the $validate-single endpoint.
     */
    @GetMapping("/\$validate-single/{id}")
    fun validationResult(@PathVariable id: String): ResponseEntity<OperationOutcome> =
        validationService.getAsyncResult(UUID.fromString(id))?.let { ok(it) } ?: notFound().build()

}
