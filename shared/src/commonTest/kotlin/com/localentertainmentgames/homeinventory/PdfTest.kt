package com.localentertainmentgames.homeinventory

import com.localentertainmentgames.homeinventory.domain.InsuranceReport
import com.localentertainmentgames.homeinventory.model.Item
import com.localentertainmentgames.homeinventory.pdf.PdfWriter
import com.localentertainmentgames.homeinventory.pdf.ReportPdf
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PdfTest {
    private val today = LocalDate(2026, 11, 24)

    private fun ByteArray.latin1(): String = buildString { this@latin1.forEach { append((it.toInt() and 0xFF).toChar()) } }

    /** Smallest valid-looking JPEG header: SOI, SOF0 (16x8, 3 components), EOI. */
    private val tinyJpeg = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xE0.toByte(), 0x00, 0x04, 0x00, 0x00,
        0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08, 0x00, 0x08, 0x00, 0x10, 0x03,
        0x01, 0x22, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
        0xFF.toByte(), 0xD9.toByte(),
    )

    @Test
    fun encodesTextForWinAnsi() {
        assertEquals("a\\(b\\)c\\\\", PdfWriter.encode("a(b)c\\"))
        assertEquals("Home -> Garage", PdfWriter.encode("Home → Garage"))
        assertEquals("14\"", PdfWriter.encode("14″"))
        assertEquals("\\267", PdfWriter.encode("·"))
        assertEquals("?", PdfWriter.encode("日"))
    }

    @Test
    fun readsJpegSize() {
        assertEquals(Triple(16, 8, 3), PdfWriter.jpegInfo(tinyJpeg))
        assertNull(PdfWriter.jpegInfo(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun ellipsizesLongText() {
        val t = PdfWriter.ellipsize("A very long item name that will not fit", 60f, 10f, PdfWriter.Font.Regular)
        assertTrue(t.endsWith("..."))
        assertTrue(PdfWriter.textWidth(t, 10f, PdfWriter.Font.Regular) <= 60f)
        assertEquals("Short", PdfWriter.ellipsize("Short", 200f, 10f, PdfWriter.Font.Regular))
    }

    @Test
    fun writesStructurallyValidPdf() {
        val pdf = PdfWriter()
        val page = pdf.newPage()
        page.text(40f, 40f, "Hello (world)", 12f)
        assertNotNull(pdf.addJpeg(tinyJpeg))
        val bytes = pdf.build()
        val text = bytes.latin1()
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        assertTrue("/DCTDecode" in text)
        assertTrue("(Hello \\(world\\)) Tj" in text)
        // startxref points at the xref table
        val xrefAt = text.substringAfterLast("startxref\n").substringBefore("\n").toInt()
        assertTrue(text.startsWith("xref", xrefAt))
        // each xref offset points at "N 0 obj"
        val count = text.substringAfter("xref\n0 ").substringBefore("\n").toInt()
        val offsets = text.substring(xrefAt).lines().drop(3).take(count - 1).map { it.take(10).toInt() }
        offsets.forEachIndexed { i, off -> assertTrue(text.startsWith("${i + 1} 0 obj", off), "object ${i + 1} at $off") }
    }

    @Test
    fun reportPaginatesAndEmbedsThumbnails() {
        val items = (1..40).map { Item(id = "i$it", name = "Item $it", priceCents = it * 1000L, photos = if (it % 2 == 0) listOf("/p$it.jpg") else emptyList()) }
        val report = InsuranceReport.build(items, today)
        var asked = 0
        val bytes = ReportPdf.render(report) { asked++; tinyJpeg }
        val text = bytes.latin1()
        val pages = Regex("/Type /Page ").findAll(text).count()
        assertTrue(pages >= 3, "expected several pages, got $pages")
        assertEquals(20, asked)
        assertEquals(20, Regex("/Subtype /Image").findAll(text).count())
        assertTrue("Page 1 of $pages" in text)
        assertTrue("20 items have no photo" in text)
        assertTrue("$8,200" in text) // 1..40 × $10, thousands grouped
    }

    @Test
    fun emptyReportStillRenders() {
        val text = ReportPdf.render(InsuranceReport.build(emptyList(), today)).latin1()
        assertTrue("No items yet." in text)
        assertEquals(1, Regex("/Type /Page ").findAll(text).count())
    }
}
