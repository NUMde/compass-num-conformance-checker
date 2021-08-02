package science.numcompass.conformance.compass.report

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Resource
import science.numcompass.conformance.compass.engine.APP_SECTION_LOINC_CODE
import science.numcompass.conformance.compass.engine.GECCO_BASE_URL
import science.numcompass.conformance.compass.engine.PUBLISHING_ORG_SECTION_LOINC_CODE
import science.numcompass.conformance.compass.engine.QUESTIONNAIRE_SECTION_LOINC_CODE
import science.numcompass.conformance.compass.engine.TEST_RESOURCE_SECTION_LOINC_CODE
import science.numcompass.conformance.compass.engine.getSectionResourceByLoincCode
import java.time.LocalDate
import kotlin.math.ceil

class ReportError(msg: String) : Error(msg)
typealias Page = List<String>

/**
 * Generate a summary of the information submitted, meant for the
 * subsequent generation of the certificate.
 *
 * @param inputBundle Complete input Bundle for a conformance check
 * @return the certificate data
 */
fun extractCertificateData(inputBundle: Bundle): CertificateData {
    val appDevice = getAppDevice(inputBundle)
    val geccoResources = getSectionResourceByLoincCode(inputBundle, TEST_RESOURCE_SECTION_LOINC_CODE)
        .filter { res -> res.meta?.profile?.any { it.value.startsWith(GECCO_BASE_URL) } == true }
    val questionnaires = getSectionResourceByLoincCode(inputBundle, QUESTIONNAIRE_SECTION_LOINC_CODE)

    return CertificateData(
        summarizeOrgData(getPublishingOrg(inputBundle)),
        appDevice.deviceName?.first()?.name ?: "",
        appDevice.version?.first()?.value ?: "",
        LocalDate.now(),
        geccoResources.size,
        questionnaires.size,
        (summarizeGeccoResources(geccoResources) + summarizeQuestionnaires(questionnaires)).toMutableList(),
        appDevice.manufacturer
    )
}

/**
 * Create a text summary of the GECCO resources themselves, organized by resource type and GECCO profile.
 */
private fun summarizeGeccoResources(geccoResources: List<Resource>): MutableList<String> {
    // Count the no. of resources per resource type and profile
    val resourceProfileMap = geccoResources.groupBy { it.resourceType }
        .mapValues { entry ->
            entry.value
                .mapNotNull { it.meta?.profile }
                .flatten()
                .filter { it.value.startsWith(GECCO_BASE_URL) }
                .groupBy { it.value }
                .mapValues { it.value.size }
                .toSortedMap()
        }
        .toSortedMap()
    // Generate string summary of the collected data
    val lines = arrayListOf<String>()
    resourceProfileMap.forEach { (resourceType, profiles) ->
        lines.add("Resourcentyp $resourceType")
        profiles.forEach { (url, count) ->
            lines.add("    $count Beispiel(e) vom Profil")
            lines.add("    $url")
        }
    }
    return lines
}

/**
 * Create a short text summary of the questionnaires.
 */
private fun summarizeQuestionnaires(questionnaires: List<Resource>): List<String> =
    if (questionnaires.isEmpty()) {
        emptyList()
    } else {
        listOf(
            "",
            "Die übermittelten Testdaten erfassen alle Profile, die in den ${questionnaires.size} zugrundeliegenden",
            "Questionnaire(s) deklariert wurden."
        )
    }

/**
 * Get the resource representing the organization that publishes the app.
 */
private fun getPublishingOrg(inputBundle: Bundle): Organization =
    getSectionResourceByLoincCode(inputBundle, PUBLISHING_ORG_SECTION_LOINC_CODE).firstOrNull() as? Organization
        ?: throw ReportError("Bundle does not contain app publisher information information")

/**
 * Create text summary of the data for the organization publishing the app.
 */
private fun summarizeOrgData(org: Organization): String {
    val address = org.address?.first()
    val eMail = org.telecom
        ?.first { it.system == ContactPoint.ContactPointSystem.EMAIL }
        ?.value ?: ""
    return with(StringBuilder()) {
        append("${org.name}\n")
        address?.line?.forEach { append("${it}\n") }
        append("${address?.postalCode} ${address?.city}\n")
        append("${address?.country}\n")
        append("E-Mail: $eMail")
    }.toString()
}

/**
 * Get the resource representing the app itself.
 */
private fun getAppDevice(inputBundle: Bundle): Device =
    getSectionResourceByLoincCode(inputBundle, APP_SECTION_LOINC_CODE).firstOrNull() as? Device
        ?: throw ReportError("Bundle does not contain app information")

/**
 * Splits the given lines into a list of lists.
 */
fun splitIntoPages(lines: List<String>, linesOnPage1: Int, linesOnRepeatablePage: Int): List<Page> {
    if (lines.isEmpty()) {
        return emptyList()
    }

    val numberOfLines = lines.size
    val numberOfPagesToGenerate = ceil(
        (numberOfLines - linesOnPage1) / linesOnRepeatablePage.toFloat()
    ).toInt()
    val numberOfPages = 1 + numberOfPagesToGenerate
    return (1..numberOfPages).map { page ->
        val startLine = if (page == 1) 0 else linesOnPage1 + (linesOnRepeatablePage * (page - 2))
        val linesOnPage = if (page == 1) linesOnPage1 else linesOnRepeatablePage
        val endLine = (startLine + linesOnPage).coerceAtMost(lines.size)
        lines.subList(startLine, endLine)
    }
}
