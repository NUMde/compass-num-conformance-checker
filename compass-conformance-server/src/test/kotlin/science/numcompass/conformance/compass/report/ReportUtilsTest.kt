package science.numcompass.conformance.compass.report

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test

internal class ReportUtilsTest {

    @Test
    fun testSplitEmptyIntoPages() {
        assertThat(splitIntoPages(emptyList(), 1, 1)).isEmpty()
    }

    @Test
    fun testSplitInto2Pages() {
        val lines = listOf("a", "b")

        val pageContents = splitIntoPages(lines, 1, 1)

        assertThat(pageContents).hasSize(2)
        assertThat(pageContents[0]).containsExactly("a")
        assertThat(pageContents[1]).containsExactly("b")
    }

    @Test
    fun testSplitInto3Pages() {
        val lines = listOf("a", "b", "c", "d")

        val pageContents = splitIntoPages(lines, 1, 2)

        assertThat(pageContents).hasSize(3)
        assertThat(pageContents[0]).containsExactly("a")
        assertThat(pageContents[1]).containsExactly("b", "c")
        assertThat(pageContents[2]).containsExactly("d")
    }

}
