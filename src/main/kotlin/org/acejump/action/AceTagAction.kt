package org.acejump.action

import com.intellij.codeInsight.intention.actions.ShowIntentionActionsAction
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.find.actions.FindUsagesAction
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.ide.actions.CopyAction
import com.intellij.ide.actions.CutAction
import com.intellij.ide.actions.PasteAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.openapi.util.TextRange
import com.intellij.refactoring.actions.RefactoringQuickListPopupAction
import org.acejump.*
import org.acejump.search.SearchProcessor
import kotlin.math.max

/**
 * Base class for actions available after typing a tag.
 */
sealed class AceTagAction {
  abstract operator fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean)
  
  abstract class BaseJumpAction : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val caretModel = editor.caretModel
      val oldCarets = if (shiftMode) caretModel.caretsAndSelections else emptyList()
      
      recordCaretPosition(editor)
      moveCaretTo(editor, getCaretOffset(editor, searchProcessor, offset))
      
      if (shiftMode) {
        caretModel.caretsAndSelections = oldCarets + caretModel.caretsAndSelections
      }
    }
    
    abstract fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int
  }
  
  abstract class BaseSelectAction : AceTagAction() {
    final override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      if (shiftMode) {
        val caretModel = editor.caretModel
        val oldCarets = caretModel.caretsAndSelections
        val oldOffsetPosition = caretModel.logicalPosition
        
        invoke(editor, searchProcessor, offset)
        
        if (caretModel.caretsAndSelections.any { isSelectionOverlapping(oldOffsetPosition, it) }) {
          oldCarets.removeAll { isSelectionOverlapping(oldOffsetPosition, it) }
        }
        
        caretModel.caretsAndSelections = oldCarets + caretModel.caretsAndSelections
      }
      else {
        invoke(editor, searchProcessor, offset)
      }
    }
    
    private fun isSelectionOverlapping(offset: LogicalPosition, oldCaret: CaretState): Boolean {
      return oldCaret.caretPosition == offset || oldCaret.selectionStart == offset || oldCaret.selectionEnd == offset
    }
    
    protected abstract operator fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int)
  }
  
  abstract class BaseWordAction : BaseJumpAction() {
    final override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      val matchingChars = countMatchingCharacters(editor, searchProcessor, offset)
      val targetOffset = offset + matchingChars
      val isInsideWord = matchingChars > 0 && editor.immutableText.let { it[targetOffset - 1].isWordPart && it[targetOffset].isWordPart }
      
      return getCaretOffset(editor, offset, targetOffset, isInsideWord)
    }
    
    abstract fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int
  }
  
  abstract class BaseCaretRestoringAction : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val oldCarets = editor.caretModel.caretsAndSelections
      doInvoke(editor, searchProcessor, offset, shiftMode)
      editor.caretModel.caretsAndSelections = oldCarets
    }
    
    protected abstract fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean)
  }
  
  private companion object {
    fun countMatchingCharacters(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return editor.immutableText.countMatchingCharacters(offset, searchProcessor.query.rawText)
    }
    
    fun recordCaretPosition(editor: Editor) = with(editor) {
      project?.let { addCurrentPositionToHistory(it, document) }
    }
    
    fun moveCaretTo(editor: Editor, offset: Int) = with(editor) {
      selectionModel.removeSelection(true)
      caretModel.removeSecondaryCarets()
      caretModel.moveToOffset(offset)
    }
    
    fun selectRange(editor: Editor, fromOffset: Int, toOffset: Int, cursorOffset: Int = toOffset) = with(editor) {
      selectionModel.removeSelection(true)
      selectionModel.setSelection(fromOffset, toOffset)
      caretModel.moveToOffset(cursorOffset)
    }
    
    fun performAction(action: AnAction) {
      ActionManager.getInstance().tryToExecute(action, ActionCommand.getInputEvent(null), null, null, true)
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
   * On default action, places the caret at the last character of the search query.
   * On shift action, adds the new caret to existing carets.
   */
  object JumpToSearchEnd : BaseJumpAction() {
    override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return offset + max(0, countMatchingCharacters(editor, searchProcessor, offset) - 1)
    }
  }
  
  /**
   * On default action, places the caret just past the last character of the search query.
   * On shift action, adds the new caret to existing carets.
   */
  object JumpPastSearchEnd : BaseJumpAction() {
    override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return offset + countMatchingCharacters(editor, searchProcessor, offset)
    }
  }
  
  /**
   * On default action, places the caret at the start of a word. Word detection uses [Character.isJavaIdentifierPart] to count some special
   * characters, such as underscores, as part of a word. If there is no word at the last character of the search query, then the caret is
   * placed at the first character of the search query.
   *
   * On shift action, adds the new caret to existing carets.
   */
  object JumpToWordStartTag : BaseWordAction() {
    override fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int {
      return if (isInsideWord)
        editor.immutableText.wordStart(queryEndOffset)
      else
        queryStartOffset
    }
  }
  
  /**
   * On default action, places the caret at the end of a word. Word detection uses [Character.isJavaIdentifierPart] to count some special
   * characters, such as underscores, as part of a word. If there is no word at the last character of the search query, then the caret is
   * placed after the last character of the search query.
   *
   * On shift action, adds the new caret to existing carets.
   */
  object JumpToWordEndTag : BaseWordAction() {
    override fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int {
      return if (isInsideWord)
        editor.immutableText.wordEnd(queryEndOffset) + 1
      else
        queryEndOffset
    }
  }
  
  object SelectQuery : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      recordCaretPosition(editor)
      
      val startOffset = JumpToSearchStart.getCaretOffset(editor, searchProcessor, offset)
      val endOffset = JumpPastSearchEnd.getCaretOffset(editor, searchProcessor, offset)
      
      selectRange(editor, startOffset, endOffset)
    }
  }
  
  /**
   * On default action, places the caret at the end of a word, and also selects the entire word. Word detection uses
   * [Character.isJavaIdentifierPart] to count some special characters, such as underscores, as part of a word. If there is no word at the
   * first character of the search query, then the caret is placed after the last character of the search query, and all text between the
   * start and end of the search query is selected.
   *
   * On shift action, adds the new selection to existing selections.
   */
  object SelectWord : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      val chars = editor.immutableText
      val queryEndOffset = JumpToSearchEnd.getCaretOffset(editor, searchProcessor, offset)
      
      if (chars[queryEndOffset].isWordPart) {
        recordCaretPosition(editor)
        
        val startOffset = JumpToWordStartTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        val endOffset = JumpToWordEndTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        
        selectRange(editor, startOffset, endOffset)
      }
      else {
        SelectQuery(editor, searchProcessor, offset, shiftMode = false)
      }
    }
  }
  
  object SelectHump : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      val chars = editor.immutableText
      val queryEndOffset = JumpToSearchEnd.getCaretOffset(editor, searchProcessor, offset)
      
      if (chars[queryEndOffset].isWordPart) {
        recordCaretPosition(editor)
        
        val startOffset = chars.humpStart(queryEndOffset)
        val endOffset = chars.humpEnd(queryEndOffset) + 1
        
        selectRange(editor, startOffset, endOffset)
      }
      else {
        SelectQuery(editor, searchProcessor, offset, shiftMode = false)
      }
    }
  }
  
  object SelectLine : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      editor.selectionModel.selectLineAtCaret()
    }
  }
  
  class SelectExtended(private val extendCount: Int) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      
      val action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
      
      repeat(extendCount) {
        performAction(action)
      }
    }
  }
  
  class SelectToCaret(private val jumper: BaseJumpAction) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      val caretModel = editor.caretModel
      val oldOffset = caretModel.offset
      val oldSelection = editor.selectionModel.takeIf { it.hasSelection(false) }?.let { it.selectionStart..it.selectionEnd }
      
      jumper(editor, searchProcessor, offset, shiftMode = false)
      
      val newOffset = caretModel.offset
      
      if (oldSelection == null) {
        selectRange(editor, oldOffset, newOffset)
      }
      else {
        selectRange(editor, minOf(oldOffset, newOffset, oldSelection.first), maxOf(oldOffset, newOffset, oldSelection.last), newOffset)
      }
    }
  }
  
  class SelectBetweenPoints(private val firstOffset: Int, private val secondOffsetJumper: BaseJumpAction) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      secondOffsetJumper(editor, searchProcessor, offset, shiftMode = false)
      selectRange(editor, firstOffset, editor.caretModel.offset)
    }
  }
  
  class Cut(private val selector: AceTagAction) : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(CutAction())
    }
  }
  
  class Copy(private val selector: AceTagAction) : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(CopyAction())
    }
  }
  
  class Paste(private val selector: AceTagAction) : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(PasteAction())
    }
  }
  
  class Delete(private val selector: AceTagAction) : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Delete").run<Throwable> {
        editor.selectionModel.let { editor.document.deleteString(it.selectionStart, it.selectionEnd) }
      }
    }
  }
  
  class CloneToCaret(private val selector: AceTagAction) : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val document = editor.document
      val oldCarets = editor.caretModel.caretsAndSelections
  
      selector(editor, searchProcessor, offset, shiftMode = false)
      val text = document.getText(editor.selectionModel.let { TextRange(it.selectionStart, it.selectionEnd) })
  
      editor.caretModel.caretsAndSelections = oldCarets
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Clone").run<Throwable> {
        insertAtCarets(editor, text)
      }
    }
    
    companion object {
      fun insertAtCarets(editor: Editor, text: String) {
        val document = editor.document
        
        editor.caretModel.runForEachCaret {
          if (it.hasSelection()) {
            document.replaceString(it.selectionStart, it.selectionEnd, text)
          }
          else {
            document.insertString(it.offset, text)
          }
        }
      }
    }
  }
  
  class MoveToCaret(private val selector: AceTagAction) : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val document = editor.document
      val oldCarets = editor.caretModel.caretsAndSelections
  
      selector(editor, searchProcessor, offset, shiftMode = false)
      val start = editor.selectionModel.selectionStart
      val end = editor.selectionModel.selectionEnd
      val text = document.getText(TextRange(start, end))
  
      editor.caretModel.caretsAndSelections = oldCarets
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Move").run<Throwable> {
        document.deleteString(start, end)
        CloneToCaret.insertAtCarets(editor, text)
      }
    }
  }
  
  /**
   * On default action, performs the Go To Declaration action, available via `Navigate | Declaration or Usages`.
   * On shift action, performs the Go To Type Declaration action, available via `Navigate | Type Declaration`.
   * Always places the caret at the end of the search query.
   */
  object GoToDeclaration : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(if (shiftMode) GotoTypeDeclarationAction() else GotoDeclarationAction())
    }
  }
  
  /**
   * On default action, performs the Show Usages action, available via the context menu.
   * On shift action, performs the Find Usages action, available via the context menu.
   * Always places the caret at the end of the search query.
   */
  object ShowUsages : BaseCaretRestoringAction() {
    override fun doInvoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(if (shiftMode) FindUsagesAction() else ShowUsagesAction())
    }
  }
  
  /**
   * Performs the Show Context Actions action, available via the context menu or Alt+Enter.
   */
  object ShowIntentions : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToWordStartTag(editor, searchProcessor, offset, shiftMode = false)
      performAction(ShowIntentionActionsAction())
    }
  }
  
  object Refactor : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToWordStartTag(editor, searchProcessor, offset, shiftMode = false)
      performAction(RefactoringQuickListPopupAction())
    }
  }
}
