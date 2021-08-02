package science.numcompass.conformance.compass.report

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream

internal class PdfDslTest {

    @Test
    fun getFieldNames() {
        pdf(exampleInputStream()) {
            form {
                assertThat(fieldNames()).hasSize(11)
            }
        }
    }

    @Test
    fun fillFormField() {
        val output = ByteArrayOutputStream()
        val fields = mutableListOf<String>()

        pdf(exampleInputStream()) {
            form {
                fields.addAll(fieldNames())
                fieldNames().forEach {
                    field(it)("Value for $it")
                }
            }
        }.save(output)

        val rawPdfString = output.toString()

        fields.forEach {
            assertThat(rawPdfString.contains("/V (Value for $it)")).isTrue()
        }
    }

    @Test
    fun fillUnknownField() {
        assertThrows<NullPointerException> {
            pdf(exampleInputStream()) {
                form {
                    field("<missing field>")("")
                }
            }
        }
    }

    private fun exampleInputStream() = javaClass.getResourceAsStream("/report/certificate.pdf")!!
}
