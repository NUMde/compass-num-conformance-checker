package science.numcompass.conformance.fhir.chain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

internal class TerminologyServiceKtTest {

    private val snomedSystemUrl = "http://snomed.info/sct"

    @Test
    fun `An arbitrary code not declared as SNOMED should be validated`() {
        val validSnomedExpression = "83152002:405815000=122456005"
        val valResult = isSnomedCodeThatShouldNotBeValidated("http://loinc.org", validSnomedExpression)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `A valid single SNOMED code declared as SNOMED should be validated`() {
        val validSnomedCode = "410594000"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validSnomedCode)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `An integer that is not a valid SNOMED code but is declared as SNOMED should be validated`() {
        val invalidSnomedCode = "999999999"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, invalidSnomedCode)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `An alphanumeric code declared as SNOMED should be validated`() {
        val alphanumericCode = "ad67886asjd"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, alphanumericCode)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `An alphanumeric code with spaces declared as SNOMED should be validated`() {
        val alphanumericCodeWithSpace = "ad678 86asjd"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, alphanumericCodeWithSpace)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `A valid single SNOMED code with label and leading whitespace declared as SNOMED should be validated (will correctly fail validation)`() {
        val validSnomedCodeWithDisplayAndLeadingSpace = " 410594000|Definitely not present"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validSnomedCodeWithDisplayAndLeadingSpace)
        assertThat(valResult).isFalse()
    }

    @Test
    fun `A  valid single SNOMED code with display label declared as SNOMED should not be validated`() {
        val validSnomedCodeWithDisplay = "410594000|Definitely not present"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validSnomedCodeWithDisplay)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED multiple focus expression should not be validated`() {
        val validMultiFocusSnomedExpression = "421720008+7946007"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validMultiFocusSnomedExpression)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED expression with refinements should not be validated`() {
        val validSnomedRefinementExpression = "83152002:405815000=122456005"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validSnomedRefinementExpression)
        assertThat(valResult).isTrue()
    }

    @Test
    fun `A SNOMED expression with attribute groups should not be validated`() {
        val validSnomedExporessionWithGroups = "71388002:{260686004=129304002,405813007=15497006}"
        val valResult = isSnomedCodeThatShouldNotBeValidated(snomedSystemUrl, validSnomedExporessionWithGroups)
        assertThat(valResult).isTrue()
    }

}
