package science.numcompass.conformance.compass.report

import java.time.LocalDate

/**
 * Class for capturing the information that goes into the certificate PDF after a successful check.
 *
 * @constructor
 * @param orgData Information about the organization publishing the app
 * @param appName Name of the app being checked
 * @param appVersion Version of the app being checked
 * @param testDate Date of the conformance check
 * @param resourceCounter Number of validated resources
 * @param questionnaireCounter Number of validated questionnaires
 * @param summary Summary of the validation of the test data itself
 * @param manufacturer Name of the company that programmed the app (default: null)
 */
data class CertificateData(
    val orgData: String,
    val appName: String,
    val appVersion: String,
    val testDate: LocalDate,
    val resourceCounter: Int,
    val questionnaireCounter: Int,
    val summary: MutableList<String>,
    val manufacturer: String? = null
)
