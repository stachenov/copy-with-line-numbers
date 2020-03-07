package name.tachenov.intellij.plugins.copyWithLineNumbers

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData
import com.intellij.openapi.util.SystemInfo
import java.awt.datatransfer.DataFlavor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class RtfTransferableDataWithLineNumbers(rawText: String, rtf: RtfTransferableData)
    : InputStream(), TextBlockTransferableData {

    private val dataWithNumbers: ByteArray

    init {
        /*
        This is REALLY ugly, but RtfTransferableData doesn't seem to be very extensible.
        So it's either make our own RTF builder (which is very tiresome and brittle),
        or hack the existing one by using the only one “extension point”:
        hacking the output as raw byte stream.
         */
        rtf.setRawText(rawText)
        val byteBuffer = ByteArrayOutputStream()
        val buffer = ByteArray(16384)
        rtf.use { original ->
            while (true) { // really need a StreamUtil.readAll()-like thing here
                val read = original.read(buffer)
                if (read == -1)
                    break
                byteBuffer.write(buffer, 0, read)
            }
        }
        val rtfString = byteBuffer.toByteArray().toString(StandardCharsets.US_ASCII)
        val lines = rtfString.split(RTF_NEW_LINE).toMutableList()
        val firstLine = lines[0]
        val fontSizePos = firstLine.indexOf("\\fs")
        val headerEnd = firstLine.indexOf('\n', fontSizePos) + 1
        val header = firstLine.substring(0, headerEnd)
        lines[0] = firstLine.substring(headerEnd)
        val text = header + lines.asSequence()
                .withIndex()
                .map { "${it.index + 1}$RTF_TAB${it.value}" }
                .joinToString(RTF_NEW_LINE)
        dataWithNumbers = text.toByteArray(StandardCharsets.US_ASCII)
    }

    private val inputStreamWithNumbers = ByteArrayInputStream(dataWithNumbers)

    override fun read() = inputStreamWithNumbers.read()
    override fun read(b: ByteArray, off: Int, len: Int) = inputStreamWithNumbers.read(b, off, len)
    override fun available() = inputStreamWithNumbers.available()
    override fun reset() = inputStreamWithNumbers.reset()
    override fun mark(readlimit: Int) = inputStreamWithNumbers.mark(readlimit)
    override fun markSupported() = inputStreamWithNumbers.markSupported()

    override fun getOffsetCount() = 0
    override fun setOffsets(offsets: IntArray?, index: Int) = index
    override fun getOffsets(offsets: IntArray?, index: Int) = index
    override fun getFlavor(): DataFlavor = RtfTransferableData.FLAVOR
    override fun getPriority(): Int = RtfTransferableData.PRIORITY
}

private const val RTF_TAB = "\\tab\n"
private val RTF_NEW_LINE = if (SystemInfo.isMac) "\\\n" else "\\line\n"
