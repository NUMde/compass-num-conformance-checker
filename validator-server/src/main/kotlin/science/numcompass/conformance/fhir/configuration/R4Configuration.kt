package science.numcompass.conformance.fhir.configuration

import ca.uhn.fhir.jpa.api.config.DaoConfig
import ca.uhn.fhir.jpa.binstore.DatabaseBlobBinaryStorageSvcImpl
import ca.uhn.fhir.jpa.binstore.IBinaryStorageSvc
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider
import ca.uhn.fhir.jpa.model.config.PartitionSettings
import ca.uhn.fhir.jpa.model.entity.ModelConfig
import ca.uhn.fhir.jpa.model.entity.NormalizedQuantitySearchLevel
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.orm.jpa.JpaConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import science.numcompass.conformance.fhir.chain.CommonCodeSystemsTerminologyValidator
import science.numcompass.conformance.fhir.chain.DefaultProfileValidator
import science.numcompass.conformance.fhir.chain.InMemoryTerminologyValidator
import science.numcompass.conformance.fhir.chain.JpaPersistedResourceValidator
import science.numcompass.conformance.fhir.chain.NpmJpaValidator
import science.numcompass.conformance.fhir.chain.PrePopulatedValidator
import science.numcompass.conformance.fhir.chain.PrePopulatedValidator.Customizer
import science.numcompass.conformance.fhir.chain.SnapshotGeneratingValidator
import science.numcompass.conformance.fhir.chain.SubChain
import science.numcompass.conformance.fhir.chain.TermConceptMapper
import science.numcompass.conformance.fhir.chain.TerminologyService
import science.numcompass.conformance.fhir.chain.UnknownCodeSystemSupport
import science.numcompass.conformance.fhir.chain.ValidationChain

@Configuration
@ComponentScan(
    "science.numcompass.conformance.fhir.configuration",
    "science.numcompass.conformance.fhir.service"
)
@EnableConfigurationProperties(AppProperties::class)
@AutoConfigureAfter(JpaConfiguration::class)
class R4Configuration(val appProperties: AppProperties) : BaseJavaConfigR4() {

    @Primary
    @Bean
    fun dialectProvider(entityManagerFactory: LocalContainerEntityManagerFactoryBean) =
        object : HibernatePropertiesProvider() {
            val resolvedDialect = StandardDialectResolver().resolveDialect(
                DatabaseMetaDataDialectResolutionInfoAdapter(entityManagerFactory.dataSource.connection.metaData)
            )

            override fun getDialect() = resolvedDialect
        }

    @Bean
    fun daoConfig() = DaoConfig().apply {
        maximumExpansionSize = 100_000
    }

    @Bean
    fun partitionSettings() = PartitionSettings()

    @Bean
    fun modelConfig(daoConfig: DaoConfig): ModelConfig = daoConfig.modelConfig.apply {
        normalizedQuantitySearchLevel = NormalizedQuantitySearchLevel.NORMALIZED_QUANTITY_SEARCH_NOT_SUPPORTED
    }

    @Lazy
    @Bean
    fun binaryStorageSvc(): IBinaryStorageSvc = DatabaseBlobBinaryStorageSvcImpl()

    //region validation chain

    @Bean(JPA_VALIDATION_SUPPORT_CHAIN)
    override fun jpaValidationSupportChain() = ValidationChain(fhirContext())

    @Bean("myDefaultProfileValidationSupport")
    @Order(LOWEST_PRECEDENCE - 1000)
    override fun defaultProfileValidationSupport() = DefaultProfileValidator(fhirContext()).apply {
        fetchCodeSystem("")
        fetchAllStructureDefinitions<IBaseResource>()
    }

    @Bean(JPA_VALIDATION_SUPPORT)
    @Order(LOWEST_PRECEDENCE - 900)
    override fun jpaValidationSupport() = JpaPersistedResourceValidator(fhirContext())

    @Bean
    @Order(LOWEST_PRECEDENCE - 800)
    override fun terminologyService() = TerminologyService(daoConfig())

    @Bean
    @Order(LOWEST_PRECEDENCE - 700)
    fun snapshotGenerator() = SnapshotGeneratingValidator(fhirContext())

    @Bean
    @Order(LOWEST_PRECEDENCE - 600)
    fun inMemoryTerminologyService() = InMemoryTerminologyValidator(fhirContext())

    @Bean
    @Order(LOWEST_PRECEDENCE - 500)
    override fun npmJpaValidationSupport() = NpmJpaValidator()

    @Bean
    @Order(LOWEST_PRECEDENCE - 400)
    fun commonCodeSystemService() = CommonCodeSystemsTerminologyValidator(fhirContext())

    @Bean
    @Order(LOWEST_PRECEDENCE - 300)
    fun prePopulatedValidator(customizers: List<Customizer>) =
        PrePopulatedValidator(fhirContext()).apply {
            customizers.forEach { it.customize(this) }
        }

    @Bean
    @Order(LOWEST_PRECEDENCE - 200)
    override fun termConceptMappingSvc() = TermConceptMapper()

    @Bean
    @Order(LOWEST_PRECEDENCE - 100)
    fun remoteTerminologyValidationChain(): SubChain? =
        if (appProperties.remoteTerminologyServers.isEmpty()) {
            null
        } else {
            SubChain().apply {
                appProperties.remoteTerminologyServers
                    .map { url -> RemoteTerminologyServiceValidationSupport(fhirContext()).apply { setBaseUrl(url) } }
                    .forEach { addValidationSupport(it) }
            }
        }

    @Bean
    @Order(LOWEST_PRECEDENCE)
    @ConditionalOnProperty(name = ["compass.only-warn-on-unknown-code-systems"], matchIfMissing = true)
    override fun unknownCodeSystemWarningValidationSupport() = UnknownCodeSystemSupport(fhirContext())

    //endregion
}
