package name.tachenov.intellij.plugins.copyWithLineNumbers

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDocumentManager
import java.util.*

class CopyWithLineNumbers : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        // I really dislike copy-paste programming, but my knowledge of IntelliJ Platform is very limited,
        // so this was mostly shamelessly copied from com.intellij.codeInsight.editorActions.CopyHandler.doExecute()
        val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val selectionModel = editor.selectionModel
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val startOffsets = selectionModel.blockSelectionStarts
        val endOffsets = selectionModel.blockSelectionEnds
        val transferableDataList = ArrayList<TextBlockTransferableData>()
        DumbService.getInstance(project).withAlternativeResolveEnabled {
            for (processor in CopyPastePostProcessor.EP_NAME.extensionList) {
                transferableDataList.addAll(processor.collectTransferableData(file, editor, startOffsets, endOffsets))
            }
        }
        transferableDataList.retainAll { it is RtfTransferableData || it is HtmlTransferableData }
        var text = if (editor.caretModel.supportsMultipleCarets())
                EditorCopyPasteHelperImpl.getSelectedTextForClipboard(editor, transferableDataList)
            else
                selectionModel.selectedText
        text = TextBlockTransferable.convertLineSeparators(text, "\n", transferableDataList)!!
        text = addLineNumbers(text, transferableDataList)
        val transferable = TextBlockTransferable(text, transferableDataList, null)
        CopyPasteManager.getInstance().setContents(transferable)
    }

    private fun addLineNumbers(text: String, transferableDataList: ArrayList<TextBlockTransferableData>): String {
        return text
    }
}
