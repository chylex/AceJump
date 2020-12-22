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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.refactoring.actions.RefactoringQuickListPopupAction
import org.acejump.*
import org.acejump.search.SearchProcessor
import kotlin.math.max

/**
 * Base class for actions available after typing a tag.
 */
internal sealed class AceTagAction {
  abstract operator fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean)
  
  abstract class BaseJumpAction : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val oldOffset = editor.caretModel.offset
      val oldSelection = editor.selectionModel.takeIf { it.hasSelection(false) }?.let { it.selectionStart..it.selectionEnd }
      
      recordCaretPosition(editor)
      moveCaretTo(editor, getCaretOffset(editor, searchProcessor, offset))
      
      if (shiftMode) {
        val newOffset = editor.caretModel.offset
        
        if (oldSelection == null) {
          selectRange(editor, oldOffset, newOffset)
        }
        else {
          selectRange(editor, minOf(oldOffset, newOffset, oldSelection.first), maxOf(oldOffset, newOffset, oldSelection.last), newOffset)
        }
      }
    }
    
    abstract fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int
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
  
  abstract class BaseShiftRestoresCaretsAction : AceTagAction() {
    final override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val oldCarets = editor.caretModel.caretsAndSelections
      
      invoke(editor, searchProcessor, offset)
      
      if (shiftMode) {
        editor.caretModel.caretsAndSelections = oldCarets
      }
    }
    
    protected abstract operator fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int)
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
   * On shift action, does the above but also selects all text between the original and new caret positions.
   */
  object JumpToSearchStart : BaseJumpAction() {
    override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return offset
    }
  }
  
  /**
   * On default action, places the caret at the last character of the search query.
   * On shift action, does the above but also selects all text between the original and new caret positions.
   */
  object JumpToSearchEnd : BaseJumpAction() {
    override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      return offset + max(0, countMatchingCharacters(editor, searchProcessor, offset) - 1)
    }
  }
  
  /**
   * On default action, places the caret just past the last character of the search query.
   * On shift action, does the above but also selects all text between the original and new caret positions.
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
   * On shift action, does the above but also selects all text between the original and new caret positions.
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
   * On shift action, does the above but also selects all text between the original and new caret positions.
   */
  object JumpToWordEndTag : BaseWordAction() {
    override fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int {
      return if (isInsideWord)
        editor.immutableText.wordEnd(queryEndOffset) + 1
      else
        queryEndOffset
    }
  }
  
  /**
   * On default action, places the caret at the end of a word, and also selects the entire word. Word detection uses
   * [Character.isJavaIdentifierPart] to count some special characters, such as underscores, as part of a word. If there is no word at the
   * first character of the search query, then the caret is placed after the last character of the search query, and all text between the
   * start and end of the search query is selected.
   *
   * On shift action, does the above but instead of acting on the whole word, it only acts on a single "camelHump".
   */
  object SelectWordOrHump : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      recordCaretPosition(editor)
      
      val chars = editor.immutableText
      val queryEndOffset = JumpToSearchEnd.getCaretOffset(editor, searchProcessor, offset)
  
      val finalStartOffset: Int
      val finalEndOffset: Int
  
      if (!chars[queryEndOffset].isWordPart) {
        finalStartOffset = JumpToSearchStart.getCaretOffset(editor, searchProcessor, offset)
        finalEndOffset = JumpPastSearchEnd.getCaretOffset(editor, searchProcessor, offset)
      }
      else if (!shiftMode) {
        finalStartOffset = JumpToWordStartTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        finalEndOffset = JumpToWordEndTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
      }
      else {
        finalStartOffset = chars.humpStart(queryEndOffset)
        finalEndOffset = chars.humpEnd(queryEndOffset) + 1
      }
      
      selectRange(editor, finalStartOffset, finalEndOffset)
    }
  }
  
  object SelectQuery : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      recordCaretPosition(editor)
      
      val startOffset = JumpToSearchStart.getCaretOffset(editor, searchProcessor, offset)
      val endOffset = JumpPastSearchEnd.getCaretOffset(editor, searchProcessor, offset)
      
      selectRange(editor, startOffset, endOffset)
    }
  }
  
  object SelectWord : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val chars = editor.immutableText
      val queryEndOffset = JumpToSearchEnd.getCaretOffset(editor, searchProcessor, offset)
  
      if (chars[queryEndOffset].isWordPart) {
        recordCaretPosition(editor)
        
        val startOffset = JumpToWordStartTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        val endOffset = JumpToWordEndTag.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        
        selectRange(editor, startOffset, endOffset)
      }
      else {
        SelectQuery(editor, searchProcessor, offset, shiftMode)
      }
    }
  }
  
  object SelectHump : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      val chars = editor.immutableText
      val queryEndOffset = JumpToSearchEnd.getCaretOffset(editor, searchProcessor, offset)
  
      if (chars[queryEndOffset].isWordPart) {
        recordCaretPosition(editor)
    
        val startOffset = chars.humpStart(queryEndOffset)
        val endOffset = chars.humpEnd(queryEndOffset) + 1
    
        selectRange(editor, startOffset, endOffset)
      }
      else {
        SelectQuery(editor, searchProcessor, offset, shiftMode)
      }
    }
  }
  
  object SelectLine : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      editor.selectionModel.selectLineAtCaret()
    }
  }
  
  class SelectExtended(private val extendCount: Int) : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      
      val action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
      
      repeat(extendCount) {
        performAction(action)
      }
    }
  }
  
  class SelectToCaret(private val jumper: BaseJumpAction) : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      jumper(editor, searchProcessor, offset, shiftMode = true)
    }
  }
  
  class Cut(private val selector: AceTagAction) : BaseShiftRestoresCaretsAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(CutAction())
    }
  }
  
  class Copy(private val selector: AceTagAction) : BaseShiftRestoresCaretsAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(CopyAction())
    }
  }
  
  class Paste(private val selector: AceTagAction) : BaseShiftRestoresCaretsAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      performAction(PasteAction())
    }
  }
  
  class Delete(private val selector: AceTagAction) : BaseShiftRestoresCaretsAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      selector(editor, searchProcessor, offset, shiftMode = false)
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Delete").run<Throwable> {
        editor.selectionModel.let { editor.document.deleteString(it.selectionStart, it.selectionEnd) }
      }
    }
  }
  
  /**
   * On default action, performs the Go To Declaration action, available via `Navigate | Declaration or Usages`.
   * On shift action, performs the Go To Type Declaration action, available via `Navigate | Type Declaration`.
   * Always places the caret at the end of the search query.
   */
  object GoToDeclaration : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(if (shiftMode) GotoTypeDeclarationAction() else GotoDeclarationAction())
    }
  }
  
  /**
   * On default action, performs the Show Usages action, available via the context menu.
   * On shift action, performs the Find Usages action, available via the context menu.
   * Always places the caret at the end of the search query.
   */
  object ShowUsages : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(if (shiftMode) FindUsagesAction() else ShowUsagesAction())
    }
  }
  
  /**
   * On default action, performs the Show Context Actions action, available via the context menu or Alt+Enter. Places the caret at the end
   * of the search query.
   *
   * On shift action, does the above but without changing the caret position.
   */
  object ShowIntentions : BaseShiftRestoresCaretsAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(ShowIntentionActionsAction())
    }
  }
  
  object Refactor : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false)
      performAction(RefactoringQuickListPopupAction())
    }
  }
}
