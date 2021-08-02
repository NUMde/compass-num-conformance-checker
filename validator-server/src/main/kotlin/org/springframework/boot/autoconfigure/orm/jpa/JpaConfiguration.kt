package org.springframework.boot.autoconfigure.orm.jpa

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.jpa.config.HapiFhirHibernateJpaDialect
import ca.uhn.fhir.jpa.config.HapiFhirLocalContainerEntityManagerFactoryBean
import org.hibernate.boot.model.naming.ImplicitNamingStrategy
import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.SchemaManagementProvider
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.jta.JtaTransactionManager
import javax.sql.DataSource

/**
 * This class extension is needed in order to provide Hibernate with the necessary [HapiFhirHibernateJpaDialect].
 */
@Configuration
@EnableConfigurationProperties(JpaProperties::class)
@AutoConfigureAfter(DataSourceAutoConfiguration::class)
@EnableTransactionManagement
internal class JpaConfiguration( //NOSONAR
    private val fhirContext: FhirContext,
    dataSource: DataSource,
    jpaProperties: JpaProperties?,
    beanFactory: ConfigurableListableBeanFactory?,
    jtaTransactionManager: ObjectProvider<JtaTransactionManager>?,
    hibernateProperties: HibernateProperties?,
    metadataProviders: ObjectProvider<MutableCollection<DataSourcePoolMetadataProvider>>?,
    providers: ObjectProvider<SchemaManagementProvider>?,
    physicalNamingStrategy: ObjectProvider<PhysicalNamingStrategy>?,
    implicitNamingStrategy: ObjectProvider<ImplicitNamingStrategy>?,
    hibernatePropertiesCustomizers: ObjectProvider<HibernatePropertiesCustomizer>?
) : HibernateJpaConfiguration(
    dataSource,
    jpaProperties,
    beanFactory,
    jtaTransactionManager,
    hibernateProperties,
    metadataProviders,
    providers,
    physicalNamingStrategy,
    implicitNamingStrategy,
    hibernatePropertiesCustomizers
) {

    override fun entityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        HapiFhirLocalContainerEntityManagerFactoryBean().apply {
            jpaDialect = HapiFhirHibernateJpaDialect(fhirContext.localizer)
            persistenceUnitName = "HAPI_PU"
            persistenceProvider = HibernatePersistenceProvider()
            dataSource = super.getDataSource()
            setJpaPropertyMap(vendorProperties)
            setPackagesToScan("ca.uhn.fhir.jpa.model.entity", "ca.uhn.fhir.jpa.entity")
        }

    @Primary
    @Bean("hapiTransactionManager")
    override fun transactionManager(customizers: ObjectProvider<TransactionManagerCustomizers>?)
        : PlatformTransactionManager = super.transactionManager(customizers)
}
