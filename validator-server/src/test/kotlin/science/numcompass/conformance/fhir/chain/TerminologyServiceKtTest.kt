package science.numcompass.conformance.fhir.chain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

internal class TerminologyServiceKtTest {

    private val snomedSystemUrl = "http://snomed.info/sct"

    @Test
    fun `An arbitrary code not declared as SNOMED is not skipped`() {
        val testString = "12344:4356=4545"
        val valResult = isPostCoordinateSnomed("http://loinc.org", testString)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `A normal, single SNOMED code (integer) declared as SNOMED is not skipped`() {
        val testString = "410594000"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `An alphanumeric code declared as SNOMED is not skipped`() {
        val testString = "ad67886asjd"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isFalse()
    }


    @Test
    fun `An alphanumeric code with spaces declared as SNOMED is skipped`() {
        val testString = "ad678 86asjd"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A normal, single SNOMED code (integer) with label declared as SNOMED is skipped`() {
        val testString = "410594000|Definitely not present"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A normal, single SNOMED code (integer) with label and surrounding whitespaces declared as SNOMED is skipped`() {
        val testString = " 410594000 | Definitely not present "
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED multiple focus expression is skipped`() {
        val testString = "421720008+7946007"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED expression with refinements is skipped`() {
        val testString = "83152002:405815000=122456005"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED expression with attribute groups is skipped`() {
        val testString = "71388002:{260686004=129304002,405813007=15497006}"
        val valResult = isPostCoordinateSnomed(snomedSystemUrl, testString)
        assertThat(valResult).isTrue()
    }

}
