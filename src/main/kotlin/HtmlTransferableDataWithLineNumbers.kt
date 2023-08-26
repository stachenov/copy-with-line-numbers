package name.tachenov.intellij.plugins.copyWithLineNumbers

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import java.awt.datatransfer.DataFlavor
import java.io.Reader
import java.io.StringReader

class HtmlTransferableDataWithLineNumbers(rawText: String, html: HtmlTransferableData)
    : Reader(), TextBlockTransferableData {

    private val dataWithNumbers: String

    init {
        /*
        This is REALLY ugly, but HtmlTransferableData doesn't seem to be very extensible.
        So it's either make our own HTML builder (which is very tiresome and brittle),
        or hack the existing one by using the only one “extension point”:
        hacking the output as raw char stream.

        It isn't too DRY either because most of this is sort of duplicated in RtfTransferableDataWithLineNumbers.
        Well, with all this ugliness and brittleness I expect dirty hotfixes as the original data changes,
        additional tags are added and so on. Better keep HTML mess separate from RTF mess to ensure a hotfix
        to one doesn't break another. It's not like we have any unit tests here. It's not like it'd make sense
        to have any because we can't construct input (HtmlTransferableData) anyway. There wouldn't be any need
        for all of this if we could.
         */
        html.setRawText(rawText)
        val charBuffer = StringBuilder()
        val buffer = CharArray(16384)
        html.use { original ->
            while (true) { // really need a ReaderUtil.readAll()-like thing here
                val read = original.read(buffer)
                if (read == -1)
                    break
                charBuffer.appendRange(buffer, 0, read)
            }
        }
        val htmlString = charBuffer.toString()
        val lines = htmlString.split(HTML_NEW_LINE).toMutableList()
        val firstLine = lines[0]
        val prePos = firstLine.indexOf("<pre")
        val prefixEnd = firstLine.indexOf('>', prePos) + 1
        val prefix = firstLine.substring(0, prefixEnd)
        lines[0] = firstLine.substring(prefixEnd)
        val lastLine = lines.last()
        val suffixStart = lastLine.indexOf("</pre></div></body></html>")
        val suffix = lastLine.substring(suffixStart)
        lines[lines.lastIndex] = lastLine.substring(0, suffixStart)
        if (lines[lines.lastIndex].isBlank())
            lines.removeAt(lines.lastIndex) // looks ugly otherwise
        val lineNumbers = lineNumbers(lines.size)
        val color = findColor(prefix)
        val htmlLineNumbers = lineNumbers
                .map { "<span style=\"color:#$color;font-style:normal;font-weight:normal\">$it&#32;&#32;&#32;&#32;</span>" }
        dataWithNumbers =
                prefix +
                lines.asSequence()
                    .withIndex()
                    .map { "${htmlLineNumbers[it.index]}${it.value}" }
                    .joinToString(HTML_NEW_LINE) +
                suffix
    }

    private val readerWithNumbers = StringReader(dataWithNumbers)

    override fun read() = readerWithNumbers.read()
    override fun read(cbuf: CharArray, off: Int, len: Int) = readerWithNumbers.read(cbuf, off, len)
    override fun close() = readerWithNumbers.close()
    override fun reset() = readerWithNumbers.reset()
    override fun mark(readlimit: Int) = readerWithNumbers.mark(readlimit)
    override fun markSupported() = readerWithNumbers.markSupported()

    override fun getOffsetCount() = 0
    override fun setOffsets(offsets: IntArray, index: Int): Int = index
    override fun getOffsets(offsets: IntArray, index: Int): Int = index
    override fun getFlavor(): DataFlavor = HtmlTransferableData.FLAVOR
    override fun getPriority(): Int = HtmlTransferableData.PRIORITY
}

private const val HTML_NEW_LINE = "<br>"

private fun findColor(html: String): String {
    val keyString = ";color:#"
    val keyStringIndex = html.indexOf(keyString)
    val colorStartIndex = keyStringIndex + keyString.length
    val colorLength = 6
    return html.substring(colorStartIndex, colorStartIndex + colorLength)
}
