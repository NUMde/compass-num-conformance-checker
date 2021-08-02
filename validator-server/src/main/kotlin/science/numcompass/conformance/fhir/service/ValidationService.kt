package science.numcompass.conformance.fhir.service

import ca.uhn.fhir.validation.FhirValidator
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.stereotype.Service
import java.util.*

@Service
class ValidationService(
    private val fhirValidator: FhirValidator,
    private val jobExecutor: JobExecutor
) {

    fun validate(resource: String) = fhirValidator.validateWithResult(resource).toOperationOutcome() as OperationOutcome

    fun getAsyncResult(id: UUID): OperationOutcome? = jobExecutor.getResult(id)

    /**
     * Executes the validation in a background job and return the job ID
     *
     * @param resource a FHIR resource to validate
     * @param resourceCacheKey the key used for the cache
     *
     * @return the job ID
     */
    fun validateAsync(resource: String, resourceCacheKey: String) =
        jobExecutor.execute(resourceCacheKey) { validate(resource) }

}
