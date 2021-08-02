package science.numcompass.conformance.fhir.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.time.Duration

const val DEFAULT_CACHE_EXPIRATION = 60L

@ConfigurationProperties("hapi.fhir")
@Configuration
@EnableConfigurationProperties
class AppProperties {

    /** Contains a list of URLs of remote terminology servers used during validation */
    var remoteTerminologyServers = mutableListOf<String>()
    /** Defines the expiration of the validation job cache */
    var cacheExpiration: Duration = Duration.ofMinutes(DEFAULT_CACHE_EXPIRATION)
    var codeSystems: CodeSystems? = null
    /** Defines implementation guides to be installed */
    var implementationGuides: Map<String, ImplementationGuide> = mutableMapOf()

    open class ImplementationGuide {
        /** a URL can be provided if the package is not present in SIMPLIFIER.NET */
        var url: String? = null
        /** name of the package; references SIMPLIFIER.NET */
        var name: String? = null
        /** version of the package; references SIMPLIFIER.NET */
        var version: String? = null
    }

    open class CodeSystems {
        /** path to a zip file containing the LOINC code system */
        var loinc: String? = null
        /** path to a zip file containing the SNOMED CT code system */
        var snomed: String? = null
    }
}
