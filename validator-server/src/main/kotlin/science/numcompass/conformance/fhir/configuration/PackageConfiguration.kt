package science.numcompass.conformance.fhir.configuration

import ca.uhn.fhir.jpa.dao.data.INpmPackageVersionDao
import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec
import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

private val logger = KotlinLogging.logger { }
private val excludedPackages = listOf("hl7.fhir.r2.core", "hl7.fhir.r3.core", "hl7.fhir.r4.core", "hl7.fhir.r5.core")

@Configuration
class PackageConfiguration(
    private val packageInstaller: IPackageInstallerSvc,
    private val packageDao: INpmPackageVersionDao,
    private val appProperties: AppProperties
) {

    /**
     * Starts installing packages as soon as the application is started.
     */
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @EventListener(ContextRefreshedEvent::class)
    fun initialize() {
        appProperties.implementationGuides.values.forEach { guide ->
            if (packageDao.findByPackageIdAndVersion(guide.name, guide.version).isEmpty) {
                logger.info { "Installing package ${guide.name}:${guide.version}" }
                val packageInstallationSpec = PackageInstallationSpec()
                    .setPackageUrl(guide.url)
                    .setName(guide.name)
                    .setVersion(guide.version)
                    .setInstallMode(PackageInstallationSpec.InstallModeEnum.STORE_AND_INSTALL)
                    .setFetchDependencies(true)
                    .apply { dependencyExcludes = excludedPackages }
                packageInstaller.install(packageInstallationSpec)
            }
        }
    }
}
