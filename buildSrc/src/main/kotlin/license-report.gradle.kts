import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    id("com.github.kkdad.dependency-license-report")
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryMarkdownReportRenderer())
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

tasks.register<Copy>("copyLicenseReport") {
    group = "reporting"
    from(licenseReport.outputDir + "/licenses.md")
    into("${project.rootDir}")
    rename { "dependencies.md" }
    dependsOn(tasks.generateLicenseReport)
}
