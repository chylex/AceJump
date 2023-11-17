package org.acejump.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.commands.ActionCommand
import org.acejump.search.SearchProcessor

/**
 * Base class for actions available after typing a tag.
 */
sealed class AceTagAction {
  abstract operator fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean)
  
  abstract class BaseJumpAction : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      val caretModel = editor.caretModel
      val oldCarets = if (shiftMode) caretModel.caretsAndSelections else emptyList()
      
      recordCaretPosition(editor)
      
      if (isFinal) {
        ensureEditorFocused(editor)
      }
      
      moveCaretTo(editor, getCaretOffset(editor, searchProcessor, offset))
      
      if (shiftMode) {
        caretModel.caretsAndSelections = oldCarets + caretModel.caretsAndSelections
      }
    }
    
    abstract fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int
  }
  
  private companion object {
    fun recordCaretPosition(editor: Editor) = with(editor) {
      project?.let { addCurrentPositionToHistory(it, document) }
    }
    
    fun moveCaretTo(editor: Editor, offset: Int) = with(editor) {
      selectionModel.removeSelection(true)
      caretModel.removeSecondaryCarets()
      caretModel.moveToOffset(offset)
    }
    
    fun performAction(actionName: String) {
      val actionManager = ActionManager.getInstance()
      val action = actionManager.getAction(actionName)
      if (action != null) {
        actionManager.tryToExecute(action, ActionCommand.getInputEvent(null), null, null, true)
      }
    }
    
    fun ensureEditorFocused(editor: Editor) {
      val project = editor.project ?: return
      val fem = FileEditorManagerEx.getInstanceEx(project)
      
      val window = fem.windows.firstOrNull { (it.selectedComposite?.selectedWithProvider?.fileEditor as? TextEditor)?.editor === editor }
      if (window != null && window !== fem.currentWindow) {
        fem.currentWindow = window
      }
    }
    
    private fun addCurrentPositionToHistory(project: Project, document: Document) {
      CommandProcessor.getInstance().executeCommand(project, {
        with(IdeDocumentHistory.getInstance(project)) {
          setCurrentCommandHasMoves()
          includeCurrentCommandAsNavigation()
          includeCurrentPlaceAsChangePlace()
        }
      }, "AceJumpHistoryAppender", DocCommandGroupId.noneGroupId(document), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION, document)
    }
  }
  
  // Actions
  
  /**
   * On default action, places the caret at the first character of the search query.
   * On shift action, adds the new caret to existing carets.
   */
  object JumpToSearchStart : BaseJumpAction() {
    override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return offset
    }
  }
  
  /**
   * On default action, performs the Go To Declaration action, available via `Navigate | Declaration or Usages`.
   * On shift action, performs the Go To Type Declaration action, available via `Navigate | Type Declaration`.
   * Always places the caret at the start of the word.
   */
  object GoToDeclaration : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      JumpToSearchStart(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      ApplicationManager.getApplication().invokeLater { performAction(if (shiftMode) IdeActions.ACTION_GOTO_TYPE_DECLARATION else IdeActions.ACTION_GOTO_DECLARATION) }
    }
  }
}
