package name.tachenov.intellij.plugins.copyWithLineNumbers

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDocumentManager
import java.awt.datatransfer.Transferable
import java.util.*

class CopyWithLineNumbers : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Mostly shamelessly copied from com.intellij.codeInsight.editorActions.CopyHandler.doExecute()
        val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return
        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.component)) ?: return
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val selectionModel = editor.selectionModel
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val startOffsets = selectionModel.blockSelectionStarts
        val endOffsets = selectionModel.blockSelectionEnds
        val transferableDataList: MutableList<TextBlockTransferableData> = ArrayList()
        DumbService.getInstance(project).withAlternativeResolveEnabled {
            for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
                transferableDataList.addAll(processor.collectTransferableData(file, editor, startOffsets, endOffsets))
            }
        }
        val text = (if (editor.caretModel.supportsMultipleCarets())
            EditorCopyPasteHelperImpl.getSelectedTextForClipboard(editor, transferableDataList)
        else
            selectionModel.selectedText)
        ?: return
        val rawText = TextBlockTransferable.convertLineSeparators(text, "\n", transferableDataList)!!
        var escapedText: String? = null
        for (processor in CopyPastePreProcessor.EP_NAME.extensionList) {
            escapedText = processor.preprocessOnCopy(file, startOffsets, endOffsets, rawText)
            if (escapedText != null)
                break
        }
        val transferable = TextBlockTransferable(escapedText ?: rawText,
                transferableDataList,
                if (escapedText != null) RawText(rawText) else null)
        CopyPasteManager.getInstance().setContents(transferable)

    }
}