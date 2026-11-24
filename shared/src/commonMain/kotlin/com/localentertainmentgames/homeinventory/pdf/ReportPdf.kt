package com.localentertainmentgames.homeinventory.pdf

import com.localentertainmentgames.homeinventory.domain.InsuranceReport
import com.localentertainmentgames.homeinventory.model.Format
import com.localentertainmentgames.homeinventory.pdf.PdfWriter.Font
import com.localentertainmentgames.homeinventory.pdf.PdfWriter.Rgb

/** Lays out the insurance report as US Letter pages, with a photo thumbnail per item where one exists. */
object ReportPdf {
    private val Fg1 = Rgb.hex(0x212121)
    private val Fg3 = Rgb.hex(0x616161)
    private val Border = Rgb.hex(0xE0E0E0)
    private val Surface = Rgb.hex(0xF5F5F5)
    private val Brand = Rgb.hex(0x6949FF)
    private val WarnBg = Rgb.hex(0xFFFCEB)
    private val WarnFg = Rgb.hex(0xA67800)

    private const val MARGIN = 40f
    private const val ROW_H = 48f
    private const val HEADER_ROW_H = 22f
    private const val FOOTER_H = 30f

    fun render(report: InsuranceReport, thumbnail: (String) -> ByteArray? = { null }): ByteArray {
        val pdf = PdfWriter()
        val contentW = pdf.pageWidth - MARGIN * 2
        // Columns: photo, item+serial, location, bought, qty, value
        val colPhoto = MARGIN + 6
        val colItem = MARGIN + 56
        val colLoc = MARGIN + 250
        val colBought = MARGIN + 390
        val colQtyRight = MARGIN + 470
        val colValueRight = MARGIN + contentW - 6

        var page = pdf.newPage()
        var y = MARGIN

        // Title block
        page.text(MARGIN, y, "Home Inventory", 22f, Font.Bold, Fg1)
        page.textRight(colValueRight, y + 4, report.dateLabel, 11f, Font.Regular, Fg3)
        y += 30
        page.text(MARGIN, y, "Insurance report · stored offline on this device", 11f, Font.Regular, Fg3)
        y += 26

        // Summary boxes
        val boxW = (contentW - 16) / 3
        listOf(
            "TOTAL VALUE" to Format.money(report.totalValueCents),
            "ITEMS" to report.itemCount.toString(),
            "WITH PHOTOS" to (report.itemCount - report.missingPhotoCount).toString(),
        ).forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * (boxW + 8)
            page.rect(x, y, boxW, 54f, Surface)
            page.text(x + 12, y + 10, label, 8f, Font.Bold, Fg3)
            page.text(x + 12, y + 26, value, 16f, Font.MonoBold, if (i == 0) Brand else Fg1)
        }
        y += 66
        if (report.missingPhotoCount > 0) {
            page.rect(MARGIN, y, contentW, 24f, WarnBg)
            page.text(
                MARGIN + 10, y + 7,
                "${report.missingPhotoCount} ${Format.plural(report.missingPhotoCount, "item")} have no photo. Insurers may ask for one.",
                10f, Font.Bold, WarnFg,
            )
            y += 34
        }

        fun tableHeader() {
            page.rect(MARGIN, y, contentW, HEADER_ROW_H, Surface)
            val ty = y + 7
            page.text(colPhoto, ty, "PHOTO", 7.5f, Font.Bold, Fg3)
            page.text(colItem, ty, "ITEM · SERIAL", 7.5f, Font.Bold, Fg3)
            page.text(colLoc, ty, "LOCATION · CATEGORY", 7.5f, Font.Bold, Fg3)
            page.text(colBought, ty, "BOUGHT", 7.5f, Font.Bold, Fg3)
            page.textRight(colQtyRight, ty, "QTY", 7.5f, Font.Bold, Fg3)
            page.textRight(colValueRight, ty, "VALUE", 7.5f, Font.Bold, Fg3)
            y += HEADER_ROW_H
        }
        tableHeader()

        val pageBottom = pdf.pageHeight - MARGIN - FOOTER_H
        for (row in report.rows) {
            if (y + ROW_H > pageBottom) {
                page = pdf.newPage()
                y = MARGIN
                tableHeader()
            }
            val thumbBytes = row.photoPath?.let { runCatching { thumbnail(it) }.getOrNull() }
            val img = thumbBytes?.let { pdf.addJpeg(it) }
            if (img != null) {
                page.rect(colPhoto, y + 6, 36f, 36f, Surface)
                page.image(img, colPhoto, y + 6, 36f, 36f)
            } else {
                page.strokeRect(colPhoto, y + 6, 36f, 36f, if (row.photoPath == null) WarnFg else Border)
                page.text(colPhoto + 5, y + 20, if (row.photoPath == null) "NONE" else "IMG", 6.5f, Font.Mono, Fg3)
            }
            page.text(colItem, y + 12, PdfWriter.ellipsize(row.name, colLoc - colItem - 8, 10f, Font.Bold), 10f, Font.Bold, Fg1)
            page.text(colItem, y + 27, PdfWriter.ellipsize(row.serial, colLoc - colItem - 8, 8f, Font.Mono), 8f, Font.Mono, Fg3)
            page.text(colLoc, y + 12, PdfWriter.ellipsize(row.location, colBought - colLoc - 8, 9f, Font.Regular), 9f, Font.Regular, Fg1)
            page.text(colLoc, y + 27, PdfWriter.ellipsize(row.category.ifBlank { "—" }, colBought - colLoc - 8, 8f, Font.Regular), 8f, Font.Regular, Fg3)
            page.text(colBought, y + 18, row.bought, 9f, Font.Regular, Fg1)
            page.textRight(colQtyRight, y + 18, row.quantity.toString(), 9f, Font.Mono, Fg1)
            page.textRight(colValueRight, y + 18, Format.money(row.valueCents), 10f, Font.MonoBold, Fg1)
            page.line(MARGIN, y + ROW_H, MARGIN + contentW, y + ROW_H, Border)
            y += ROW_H
        }
        if (report.rows.isEmpty()) {
            page.text(MARGIN + 6, y + 14, "No items yet.", 10f, Font.Regular, Fg3)
        }

        // Footers need the final page count, so they are drawn last.
        val total = pdf.pageCount
        return pdf.withFooters(total, Fg3).build()
    }

    private fun PdfWriter.withFooters(total: Int, color: Rgb): PdfWriter {
        allPages.forEachIndexed { i, p ->
            p.text(MARGIN, pageHeight - MARGIN - 10, "Home Inventory · Insurance report", 8f, Font.Regular, color)
            p.textRight(pageWidth - MARGIN, pageHeight - MARGIN - 10, "Page ${i + 1} of $total", 8f, Font.Regular, color)
        }
        return this
    }
}
