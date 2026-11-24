package com.localentertainmentgames.homeinventory.pdf

/**
 * Minimal PDF 1.4 writer: standard Type1 fonts (WinAnsi), filled rectangles, lines and JPEG images.
 * Pure Kotlin so the report is identical on Android and iOS and testable in common code.
 */
class PdfWriter(val pageWidth: Float = 612f, val pageHeight: Float = 792f) {

    enum class Font(val resource: String, val baseFont: String, val widthFactor: Float) {
        Regular("F1", "Helvetica", 0.5f),
        Bold("F2", "Helvetica-Bold", 0.55f),
        Mono("F3", "Courier", 0.6f),
        MonoBold("F4", "Courier-Bold", 0.6f),
    }

    data class Rgb(val r: Float, val g: Float, val b: Float) {
        companion object {
            fun hex(v: Long) = Rgb(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f)
        }
    }

    class JpegImage internal constructor(val name: String, val bytes: ByteArray, val width: Int, val height: Int, val colorSpace: String)

    inner class Page internal constructor() {
        internal val ops = StringBuilder()

        /** y is measured from the top of the page, as in screen layout. */
        fun text(x: Float, yTop: Float, text: String, size: Float, font: Font = Font.Regular, color: Rgb = BLACK) {
            val baseline = pageHeight - yTop - size
            ops.append(color.fill()).append(" BT /").append(font.resource).append(' ').append(num(size)).append(" Tf ")
                .append(num(x)).append(' ').append(num(baseline)).append(" Td (").append(encode(text)).append(") Tj ET\n")
        }

        fun textRight(xRight: Float, yTop: Float, text: String, size: Float, font: Font = Font.Regular, color: Rgb = BLACK) =
            text(xRight - textWidth(text, size, font), yTop, text, size, font, color)

        fun rect(x: Float, yTop: Float, w: Float, h: Float, color: Rgb) {
            ops.append(color.fill()).append(' ').append(num(x)).append(' ').append(num(pageHeight - yTop - h)).append(' ')
                .append(num(w)).append(' ').append(num(h)).append(" re f\n")
        }

        fun strokeRect(x: Float, yTop: Float, w: Float, h: Float, color: Rgb, width: Float = 1f) {
            ops.append(color.stroke()).append(' ').append(num(width)).append(" w ").append(num(x)).append(' ')
                .append(num(pageHeight - yTop - h)).append(' ').append(num(w)).append(' ').append(num(h)).append(" re S\n")
        }

        fun line(x1: Float, y1Top: Float, x2: Float, y2Top: Float, color: Rgb, width: Float = 0.5f) {
            ops.append(color.stroke()).append(' ').append(num(width)).append(" w ")
                .append(num(x1)).append(' ').append(num(pageHeight - y1Top)).append(" m ")
                .append(num(x2)).append(' ').append(num(pageHeight - y2Top)).append(" l S\n")
        }

        /** Draws [image] scaled to fit inside the box, centred. */
        fun image(image: JpegImage, x: Float, yTop: Float, boxW: Float, boxH: Float) {
            val scale = minOf(boxW / image.width, boxH / image.height)
            val w = image.width * scale
            val h = image.height * scale
            val dx = x + (boxW - w) / 2
            val dy = pageHeight - yTop - boxH + (boxH - h) / 2
            ops.append("q ").append(num(w)).append(" 0 0 ").append(num(h)).append(' ').append(num(dx)).append(' ')
                .append(num(dy)).append(" cm /").append(image.name).append(" Do Q\n")
        }
    }

    private val pages = mutableListOf<Page>()
    private val images = mutableListOf<JpegImage>()

    val pageCount: Int get() = pages.size
    val allPages: List<Page> get() = pages.toList()

    fun newPage(): Page = Page().also { pages += it }

    /** Returns null when the bytes are not a baseline/progressive JPEG we can embed. */
    fun addJpeg(bytes: ByteArray): JpegImage? {
        val info = jpegInfo(bytes) ?: return null
        val cs = when (info.third) {
            1 -> "/DeviceGray"
            4 -> "/DeviceCMYK"
            else -> "/DeviceRGB"
        }
        return JpegImage("Im${images.size + 1}", bytes, info.first, info.second, cs).also { images += it }
    }

    fun build(): ByteArray {
        if (pages.isEmpty()) newPage()
        val out = ByteSink()
        val offsets = mutableListOf<Int>()
        fun obj(body: () -> Unit) {
            offsets += out.size
            out.ascii("${offsets.size} 0 obj\n")
            body()
            out.ascii("\nendobj\n")
        }
        out.ascii("%PDF-1.4\n")
        out.bytes(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        val fontStart = 3
        val imageStart = fontStart + Font.entries.size
        val pageStart = imageStart + images.size
        val pageIds = pages.indices.map { pageStart + it * 2 }

        obj { out.ascii("<< /Type /Catalog /Pages 2 0 R >>") }
        obj { out.ascii("<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>") }
        Font.entries.forEach { f ->
            obj { out.ascii("<< /Type /Font /Subtype /Type1 /BaseFont /${f.baseFont} /Encoding /WinAnsiEncoding >>") }
        }
        images.forEach { img ->
            obj {
                out.ascii(
                    "<< /Type /XObject /Subtype /Image /Width ${img.width} /Height ${img.height} /ColorSpace ${img.colorSpace} " +
                        "/BitsPerComponent 8 /Filter /DCTDecode /Length ${img.bytes.size} >>\nstream\n",
                )
                out.bytes(img.bytes)
                out.ascii("\nendstream")
            }
        }
        val fontRes = Font.entries.withIndex().joinToString(" ") { (i, f) -> "/${f.resource} ${fontStart + i} 0 R" }
        val imageRes = images.withIndex().joinToString(" ") { (i, img) -> "/${img.name} ${imageStart + i} 0 R" }
        pages.forEachIndexed { i, page ->
            val contentId = pageIds[i] + 1
            obj {
                out.ascii(
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${num(pageWidth)} ${num(pageHeight)}] " +
                        "/Resources << /Font << $fontRes >> /XObject << $imageRes >> >> /Contents $contentId 0 R >>",
                )
            }
            val content = page.ops.toString()
            obj {
                out.ascii("<< /Length ${content.length} >>\nstream\n")
                out.ascii(content)
                out.ascii("\nendstream")
            }
        }
        val xref = out.size
        out.ascii("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { out.ascii(it.toString().padStart(10, '0') + " 00000 n \n") }
        out.ascii("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }

    companion object {
        val BLACK = Rgb(0f, 0f, 0f)

        private fun num(v: Float): String {
            val r = kotlin.math.round(v * 100) / 100
            val s = r.toString()
            return if (s.endsWith(".0")) s.dropLast(2) else s
        }

        private fun Rgb.fill() = "${num(r)} ${num(g)} ${num(b)} rg"
        private fun Rgb.stroke() = "${num(r)} ${num(g)} ${num(b)} RG"

        /** Approximate width; exact for the Courier faces. */
        fun textWidth(text: String, size: Float, font: Font): Float {
            if (font == Font.Mono || font == Font.MonoBold) return text.length * 0.6f * size
            var units = 0f
            for (c in text) {
                units += when {
                    c in "iljtf.,:;'!|I " -> 0.28f
                    c in "mwMW" -> 0.83f
                    c.isUpperCase() || c.isDigit() -> 0.64f
                    else -> font.widthFactor
                }
            }
            return units * size
        }

        fun ellipsize(text: String, maxWidth: Float, size: Float, font: Font): String {
            if (textWidth(text, size, font) <= maxWidth) return text
            var t = text
            while (t.isNotEmpty() && textWidth("$t...", size, font) > maxWidth) t = t.dropLast(1)
            return t.trimEnd() + "..."
        }

        /** WinAnsi-encodes and escapes a string for a PDF literal; bytes above 127 become octal escapes. */
        fun encode(text: String): String {
            val sb = StringBuilder()
            for (ch in text) {
                val code: Int? = when (ch) {
                    '→' -> null.also { sb.append("->") }
                    '″' -> '"'.code
                    '′' -> '\''.code
                    '•' -> 0x95
                    '–' -> 0x96
                    '—' -> 0x97
                    '…' -> 0x85
                    '‘' -> 0x91
                    '’' -> 0x92
                    '“' -> 0x93
                    '”' -> 0x94
                    '€' -> 0x80
                    '™' -> 0x99
                    else -> if (ch.code in 32..126 || ch.code in 0xA0..0xFF) ch.code else '?'.code
                }
                if (code == null) continue
                when {
                    code == '('.code || code == ')'.code || code == '\\'.code -> sb.append('\\').append(code.toChar())
                    code > 126 -> sb.append('\\').append(code.toString(8).padStart(3, '0'))
                    else -> sb.append(code.toChar())
                }
            }
            return sb.toString()
        }

        /** (width, height, components) from the JPEG start-of-frame marker. */
        fun jpegInfo(b: ByteArray): Triple<Int, Int, Int>? {
            if (b.size < 4 || b[0] != 0xFF.toByte() || b[1] != 0xD8.toByte()) return null
            var i = 2
            fun u(k: Int) = b[k].toInt() and 0xFF
            while (i + 9 < b.size) {
                if (u(i) != 0xFF) { i++; continue }
                val marker = u(i + 1)
                if (marker == 0xD8 || marker == 0x01 || marker in 0xD0..0xD7 || marker == 0xFF) { i += if (marker == 0xFF) 1 else 2; continue }
                val len = (u(i + 2) shl 8) or u(i + 3)
                if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    val h = (u(i + 5) shl 8) or u(i + 6)
                    val w = (u(i + 7) shl 8) or u(i + 8)
                    val comps = u(i + 9)
                    return if (w > 0 && h > 0) Triple(w, h, comps) else null
                }
                i += 2 + len
            }
            return null
        }
    }
}

private class ByteSink {
    private var buf = ByteArray(8 * 1024)
    var size = 0
        private set

    private fun ensure(extra: Int) {
        if (size + extra > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, size + extra))
    }

    fun ascii(s: String) {
        ensure(s.length)
        for (c in s) buf[size++] = c.code.toByte()
    }

    fun bytes(b: ByteArray) {
        ensure(b.size)
        b.copyInto(buf, size)
        size += b.size
    }

    fun toByteArray() = buf.copyOf(size)
}
