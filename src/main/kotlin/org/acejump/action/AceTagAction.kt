package org.acejump.action

import com.intellij.codeInsight.intention.actions.ShowIntentionActionsAction
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction
import com.intellij.find.actions.FindUsagesAction
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.actions.RefactoringQuickListPopupAction
import com.intellij.refactoring.actions.RenameElementAction
import org.acejump.countMatchingCharacters
import org.acejump.humpEnd
import org.acejump.humpStart
import org.acejump.immutableText
import org.acejump.isWordPart
import org.acejump.search.SearchProcessor
import org.acejump.wordEnd
import org.acejump.wordStart
import kotlin.math.max

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
  
  abstract class BaseWordAction : BaseJumpAction() {
    final override fun getCaretOffset(editor: Editor, searchProcessor: SearchProcessor, offset: Int): Int {
      val matchingChars = countMatchingCharacters(editor, searchProcessor, offset)
      val targetOffset = offset + matchingChars
      val isInsideWord = matchingChars > 0 && editor.immutableText.let { it[targetOffset - 1].isWordPart && it[targetOffset].isWordPart }
      
      return getCaretOffset(editor, offset, targetOffset, isInsideWord)
    }
    
    abstract fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int
  }
  
  abstract class BaseSelectAction : AceTagAction() {
    final override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
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
  
  abstract class BasePerCaretWriteAction(private val selector: AceTagAction) : AceTagAction() {
    final override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      val oldCarets = editor.caretModel.caretsAndSelections
      selector(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      val range = editor.selectionModel.let { TextRange(it.selectionStart, it.selectionEnd) }
      
      editor.caretModel.caretsAndSelections = oldCarets
      invoke(editor, range, shiftMode)
    }
    
    protected abstract operator fun invoke(editor: Editor, range: TextRange, shiftMode: Boolean)
    
    protected fun insertAtCarets(editor: Editor, text: String) {
      val document = editor.document
      
      editor.caretModel.runForEachCaret {
        if (it.hasSelection()) {
          document.replaceString(it.selectionStart, it.selectionEnd, text)
          fixIndents(editor, it.selectionStart, it.selectionEnd)
        }
        else {
          document.insertString(it.offset, text)
          fixIndents(editor, it.offset, it.offset + text.length)
        }
      }
    }
    
    private fun fixIndents(editor: Editor, startOffset: Int, endOffset: Int) {
      val project = editor.project ?: return
      val document = editor.document
      val documentManager = PsiDocumentManager.getInstance(project)
      
      documentManager.commitAllDocuments()
      
      val file = documentManager.getPsiFile(document) ?: return
      val text = document.charsSequence
      
      if (startOffset > 0 && endOffset > startOffset + 1 && text[endOffset - 1] == '\n' && text[startOffset - 1] == '\n') {
        CodeStyleManager.getInstance(project).adjustLineIndent(file, TextRange(startOffset, endOffset - 1))
      }
      else {
        CodeStyleManager.getInstance(project).adjustLineIndent(file, TextRange(startOffset, endOffset))
      }
    }
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
    
    fun ensureEditorFocused(editor: Editor) {
      val project = editor.project ?: return
      val fem = FileEditorManagerEx.getInstanceEx(project)
      
      val window = fem.windows.firstOrNull { (it.selectedEditor?.selectedWithProvider?.fileEditor as? TextEditor)?.editor === editor }
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
  object JumpToWordStart : BaseWordAction() {
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
  object JumpToWordEnd : BaseWordAction() {
    override fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int {
      return if (isInsideWord)
        editor.immutableText.wordEnd(queryEndOffset) + 1
      else
        queryEndOffset
    }
  }
  
  /**
   * On default action, places the caret at the end of the line.
   * On shift action, adds the new caret to existing carets.
   */
  object JumpToLineEnd : BaseWordAction() {
    override fun getCaretOffset(editor: Editor, queryStartOffset: Int, queryEndOffset: Int, isInsideWord: Boolean): Int {
      val document = editor.document
      val line = document.getLineNumber(queryEndOffset)
      return document.getLineEndOffset(line)
    }
  }
  
  /**
   * On default action, selects all characters covered by the search query.
   * On shift action, adds the new selection to existing selections.
   */
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
   * last character of the search query, then the caret is placed after the last character of the search query, and all text between the
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
        
        val startOffset = JumpToWordStart.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        val endOffset = JumpToWordEnd.getCaretOffset(editor, offset, queryEndOffset, isInsideWord = true)
        
        selectRange(editor, startOffset, endOffset)
      }
      else {
        SelectQuery(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      }
    }
  }
  
  /**
   * On default action, places the caret at the end of a camel hump inside a word, and also selects the hump. If there is no word at the
   * last character of the search query, then the search query is selected. See [SelectWord] and [SelectQuery] for details.
   *
   * On shift action, adds the new selection to existing selections.
   */
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
        SelectQuery(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      }
    }
  }
  
  /**
   * On default action, selects the line at the tag, excluding the indent.
   * On shift action, adds the new selection to existing selections.
   */
  object SelectLine : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      
      val document = editor.document
      val line = editor.caretModel.logicalPosition.line
      val lineStart = EditorActionUtil.findFirstNonSpaceOffsetOnTheLine(document, line)
      val lineEnd = document.getLineEndOffset(line)
      
      selectRange(editor, lineStart, lineEnd)
    }
  }
  
  /**
   * On default action, places the caret at the last character of the search query, and then performs Extend Selection a set amount of
   * times.
   *
   * On shift action, adds the new selection to existing selections.
   */
  class SelectExtended(private val extendCount: Int) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      JumpToSearchEnd(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      
      val action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
      
      repeat(extendCount) {
        performAction(action)
      }
    }
  }
  
  /**
   * On default action, selects the range between the caret and a position decided by the provided [BaseJumpAction].
   * On shift action, adds the new selection to existing selections.
   */
  class SelectToCaret(private val jumper: BaseJumpAction) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      val caretModel = editor.caretModel
      val oldOffset = caretModel.offset
      val oldSelection = editor.selectionModel.takeIf { it.hasSelection(false) }?.let { it.selectionStart..it.selectionEnd }
  
      jumper(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      
      val newOffset = caretModel.offset
      
      if (oldSelection == null) {
        selectRange(editor, oldOffset, newOffset)
      }
      else {
        selectRange(editor, minOf(oldOffset, newOffset, oldSelection.first), maxOf(oldOffset, newOffset, oldSelection.last), newOffset)
      }
    }
  }
  
  /**
   * On default action, selects the range between [firstOffset] and a position decided by the provided [BaseJumpAction].
   * On shift action, adds the new selection to existing selections.
   */
  class SelectBetweenPoints(private val firstOffset: Int, private val secondOffsetJumper: BaseJumpAction) : BaseSelectAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int) {
      secondOffsetJumper(editor, searchProcessor, offset, shiftMode = false, isFinal = true)
      selectRange(editor, firstOffset, editor.caretModel.offset)
    }
  }
  
  /**
   * Selects text based on the provided [selector] action and clones it at every existing caret, selecting the cloned text. If a caret
   * has a selection, the selected text will be replaced.
   */
  class CloneToCaret(selector: AceTagAction) : BasePerCaretWriteAction(selector) {
    override fun invoke(editor: Editor, range: TextRange, shiftMode: Boolean) {
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Clone").run<Throwable> {
        insertAtCarets(editor, editor.document.getText(range))
      }
    }
  }
  
  /**
   * Selects text based on the provided [selector] action and clones it to every existing caret, selecting the cloned text and deleting
   * the original. If a caret has a selection, the selected text will be replaced.
   */
  open class MoveToCaret(selector: AceTagAction) : BasePerCaretWriteAction(selector) {
    override fun invoke(editor: Editor, range: TextRange, shiftMode: Boolean) {
      val difference = if (shiftMode) editor.caretModel.caretsAndSelections.sumBy {
        val start = it.selectionStart?.let(editor::logicalPositionToOffset)
        val end = it.selectionEnd?.let(editor::logicalPositionToOffset)
        if (start == null || end == null || end > range.endOffset) 0 else range.length - (end - start)
      } else 0
      
      WriteCommandAction.writeCommandAction(editor.project).withName("AceJump Move").run<Throwable> {
        val document = editor.document
        val text = document.getText(range)
        document.deleteString(range.startOffset, range.endOffset)
        insertAtCarets(editor, text)
      }
      
      if (shiftMode) {
        editor.selectionModel.removeSelection(true)
        editor.caretModel.moveToOffset(range.startOffset + difference)
      }
    }
  }
  
  /**
   * On default action, performs the Go To Declaration action, available via `Navigate | Declaration or Usages`.
   * On shift action, performs the Go To Type Declaration action, available via `Navigate | Type Declaration`.
   * Always places the caret at the start of the word.
   */
  object GoToDeclaration : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      JumpToWordStart(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      ApplicationManager.getApplication().invokeLater { performAction(if (shiftMode) GotoTypeDeclarationAction() else GotoDeclarationAction()) }
    }
  }
  
  /**
   * On default action, performs the Show Usages action, available via the context menu.
   * On shift action, performs the Find Usages action, available via the context menu.
   * Always places the caret at the start of the word.
   */
  object ShowUsages : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      JumpToWordStart(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      ApplicationManager.getApplication().invokeLater { performAction(if (shiftMode) FindUsagesAction() else ShowUsagesAction()) }
    }
  }
  
  /**
   * Performs the Show Context Actions action, available via the context menu or Alt+Enter.
   * Always places the caret at the start of the word.
   */
  object ShowIntentions : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      JumpToWordStart(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      ApplicationManager.getApplication().invokeLater { performAction(ShowIntentionActionsAction()) }
    }
  }
  
  /**
   * On default action, performs the Refactor This action, available via the main menu.
   * On shift action, performs the Rename... refactoring, available via the main menu.
   * Always places the caret at the start of the word.
   */
  object Refactor : AceTagAction() {
    override fun invoke(editor: Editor, searchProcessor: SearchProcessor, offset: Int, shiftMode: Boolean, isFinal: Boolean) {
      JumpToWordStart(editor, searchProcessor, offset, shiftMode = false, isFinal = isFinal)
      ApplicationManager.getApplication().invokeLater { performAction(if (shiftMode) RenameElementAction() else RefactoringQuickListPopupAction()) }
    }
  }
}
