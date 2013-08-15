package ch.passenger.kotlin.util.pdf

import org.apache.pdfbox.PDFReader
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import org.apache.pdfbox.pdmodel.PDPageNode
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline


/**
 * Created by sdju on 14.08.13.
 */
fun main(args:Array<String>) {
    val doc : PDDocument = PDDocument.load(File("D:/dev/svd/proj/kotlin/svd-kt-html/test/KD_Sudoku_CH_8_v1.pdf"))!!

    val pages = doc.getDocumentCatalog()!!.getPages()!!

    pages.getKids()?.forEach {
        println(it)
        val pn = it as PDPageNode
        pn.getResources()
    }

}