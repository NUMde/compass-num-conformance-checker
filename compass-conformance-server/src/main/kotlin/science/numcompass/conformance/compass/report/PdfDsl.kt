package science.numcompass.conformance.compass.report

import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDField
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import java.io.InputStream

fun pdf(inputStream: InputStream, use: Boolean = true, init: PDDocument.() -> Unit = { }): PDDocument {
    val document = if (use) {
        inputStream.use { PDDocument.load(inputStream) }
    } else {
        PDDocument.load(inputStream)
    }
    return document.apply(init)
}

fun PDDocument.form(init: PDAcroForm.() -> Unit = { }) = documentCatalog.acroForm.apply(init)!!

fun PDAcroForm.fieldNames() = fields.map { it.fullyQualifiedName }

fun PDAcroForm.field(name: String, init: PDField.() -> Unit = {}) = getField(name).apply(init)!! as PDTextField

fun PDPage.copy() = PDPage(COSDictionary(this.cosObject).apply { removeItem(COSName.ANNOTS) })

/**
 * Copy the field, set a new [name] and [value] and add id to the [page] and the [PDTextField.acroForm].
 */
fun PDTextField.copy(page: PDPage, name: String, value: String): PDTextField {
    val copy = PDTextField(acroForm)
    copy.defaultAppearance = acroForm.defaultAppearance
    copy.q = q
    copy.widgets = arrayListOf(
        widgets.last().let { // if more than one widget is present, we only care about the last one
            PDAnnotationWidget().apply {
                rectangle = it.rectangle
                cosObject.setString(COSName.DA, it.cosObject.getString(COSName.DA))
            }
        }
    )
    copy.partialName = name
    copy.isMultiline = isMultiline
    copy.isRichText = isRichText
    copy.value = value // triggers reconstructions; needs to be called last

    // insert field
    acroForm.fields.add(copy)
    page.annotations.addAll(copy.widgets)

    return copy
}

operator fun PDField.invoke(value: String): PDField {
    setValue(value)
    return this
}
