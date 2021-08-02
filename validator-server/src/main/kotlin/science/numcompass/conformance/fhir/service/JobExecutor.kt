package science.numcompass.conformance.fhir.service

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import science.numcompass.conformance.fhir.configuration.AppProperties
import java.util.*
import java.util.function.Supplier

@Service
class JobExecutor(
    appProperties: AppProperties,
    @Qualifier("applicationTaskExecutor") private val executor: ThreadPoolTaskExecutor
) {

    private val resultIndex = mutableMapOf<UUID, Any>()

    private val jobCache = Caffeine.newBuilder()
        .expireAfterWrite(appProperties.cacheExpiration)
        .removalListener<String, UUID> { _, value, _ -> resultIndex.remove(value) }
        .build<String, UUID>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getResult(uuid: UUID): T? = resultIndex[uuid] as T?

    /**
     * Executes the job (if it isn't already cached) and returns a UUID with which the job result can be retrieved.
     *
     * @param cacheKey the key used to check the cache (in our case an MD5 hash of the FHIR resource)
     */
    fun execute(cacheKey: String, job: Supplier<Any>): UUID = jobCache.get(cacheKey) {
        val uuid = UUID.randomUUID()

        executor.submit {
            // execute the job and store the result
            resultIndex[uuid] = job.get()
        }

        uuid
    }!!

    fun clearCache() {
        resultIndex.clear()
        jobCache.invalidateAll()
        jobCache.cleanUp()
    }
}
