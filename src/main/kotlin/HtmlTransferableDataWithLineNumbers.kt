package name.tachenov.intellij.plugins.copyWithLineNumbers

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import java.awt.datatransfer.DataFlavor
import java.io.Reader
import java.io.StringReader

class HtmlTransferableDataWithLineNumbers(rawText: String, rtf: HtmlTransferableData)
    : Reader(), TextBlockTransferableData {

    private val dataWithNumbers: String

    init {
        /*
        This is REALLY ugly, but HtmlTransferableData doesn't seem to be very extensible.
        So it's either make our own HTML builder (which is very tiresome and brittle),
        or hack the existing one by using the only one “extension point”:
        hacking the output as raw char stream.
         */
        rtf.setRawText(rawText)
        val charBuffer = StringBuilder()
        val buffer = CharArray(16384)
        rtf.use { original ->
            while (true) { // really need a ReaderUtil.readAll()-like thing here
                val read = original.read(buffer)
                if (read == -1)
                    break
                charBuffer.append(buffer, 0, read)
            }
        }
        val htmlString = charBuffer.toString()
        val lines = htmlString.split(HTML_NEW_LINE).toMutableList()
        val firstLine = lines[0]
        val fontSizePos = firstLine.indexOf("font-size")
        val headerEnd = firstLine.indexOf('>', fontSizePos) + 1
        val header = firstLine.substring(0, headerEnd)
        lines[0] = firstLine.substring(headerEnd)
        dataWithNumbers = header + lines.asSequence()
                .withIndex()
                .map { "${it.index + 1}&#32;&#32;&#32;&#32;${it.value}" }
                .joinToString(HTML_NEW_LINE)
    }

    private val readerWithNumbers = StringReader(dataWithNumbers)

    override fun read() = readerWithNumbers.read()
    override fun read(cbuf: CharArray, off: Int, len: Int) = readerWithNumbers.read(cbuf, off, len)
    override fun close() = readerWithNumbers.close()
    override fun reset() = readerWithNumbers.reset()
    override fun mark(readlimit: Int) = readerWithNumbers.mark(readlimit)
    override fun markSupported() = readerWithNumbers.markSupported()

    override fun getOffsetCount() = 0
    override fun setOffsets(offsets: IntArray?, index: Int) = index
    override fun getOffsets(offsets: IntArray?, index: Int) = index
    override fun getFlavor(): DataFlavor = HtmlTransferableData.FLAVOR
    override fun getPriority(): Int = HtmlTransferableData.PRIORITY
}

private const val HTML_NEW_LINE = "<br>"
