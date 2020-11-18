package org.acejump.control

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import org.acejump.control.Handler.regexSearch
import org.acejump.label.Pattern
import org.acejump.label.Pattern.ALL_WORDS
import org.acejump.search.Finder
import org.acejump.search.JumpMode
import org.acejump.search.Jumper
import org.acejump.view.Boundary.*
import org.acejump.view.Model
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.defaultBoundary
import org.acejump.view.Model.editor
import org.acejump.view.Model.viewBounds

/**
 * Entry point for all actions. The IntelliJ Platform calls AceJump here.
 */

sealed class AceAction: DumbAwareAction() {
  val logger = Logger.getInstance(javaClass)

  final override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }

  final override fun actionPerformed(e: AnActionEvent) {
    editor = e.getData(EDITOR) ?: return
    boundaries = defaultBoundary
    logger.debug { "Invoked on ${FileDocumentManager.getInstance().getFile(editor.document)?.presentableName} (${editor.document.textLength})" }
    Handler.activate()
    invoke()
  }

  abstract fun invoke()

  object ActivateOrCycleMode : AceAction() {
    override fun invoke() = Jumper.cycleMode()
  }

  object ToggleJumpMode: AceAction() {
    override fun invoke() = Jumper.toggleMode(JumpMode.JUMP)
  }

  object ToggleJumpEndMode: AceAction() {
    override fun invoke() = Jumper.toggleMode(JumpMode.JUMP_END)
  }

  object ToggleSelectWordMode: AceAction() {
    override fun invoke() = Jumper.toggleMode(JumpMode.TARGET)
  }

  object ToggleDefinitionMode: AceAction() {
    override fun invoke() = Jumper.toggleMode(JumpMode.DEFINE)
  }

  object ToggleAllLinesMode: AceAction() {
    override fun invoke() = regexSearch(Pattern.LINE_MARK)
  }

  object ToggleAllWordsMode: AceAction() {
    override fun invoke() = regexSearch(ALL_WORDS, SCREEN_BOUNDARY)
  }

  object ToggleAllWordsForwardMode: AceAction() {
    override fun invoke() = regexSearch(ALL_WORDS, AFTER_CARET_BOUNDARY)
  }

  object ToggleAllWordsBackwardsMode: AceAction() {
    override fun invoke() = regexSearch(ALL_WORDS, BEFORE_CARET_BOUNDARY)
  }

  object ActOnHighlightedWords: AceAction() {
    override fun invoke() = when(JumpMode.mode) {
      JumpMode.DISABLED -> {}
      JumpMode.JUMP -> if (editor.caretModel.supportsMultipleCarets()) jumpToAll() else Unit
      JumpMode.JUMP_END -> if (editor.caretModel.supportsMultipleCarets()) jumpToWordEnds() else Unit
      JumpMode.TARGET -> if (editor.caretModel.supportsMultipleCarets()) selectAllWords() else Unit
      JumpMode.DEFINE -> {}
    }

    private fun jumpToAll() {
      val carets = Finder.allResults().map {
        CaretState(editor.offsetToLogicalPosition(it), null, null)
      }
      if (carets.isEmpty()) return
      Handler.reset()
      editor.caretModel.caretsAndSelections = carets
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    private fun jumpToWordEnds() {
      val ranges = ArrayList<TextRange>()
      for (offset in Finder.allResults()) {
        SelectWordUtil.addWordSelection(editor.settings.isCamelWords, Model.editorText, offset, ranges)
      }
      if (ranges.isEmpty()) return

      Handler.reset()
      editor.caretModel.caretsAndSelections = ranges.map {
        CaretState(editor.offsetToLogicalPosition(it.endOffset), null, null)
      }
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    private fun selectAllWords() {
      val ranges = ArrayList<TextRange>()
      for (offset in Finder.allResults()) {
        SelectWordUtil.addWordSelection(editor.settings.isCamelWords, Model.editorText, offset, ranges)
      }
      if (ranges.isEmpty()) return

      Handler.reset()
      editor.caretModel.caretsAndSelections = ranges.map {
        val start = editor.offsetToLogicalPosition(it.startOffset)
        val end = editor.offsetToLogicalPosition(it.endOffset)
        CaretState(end, start, end)
      }
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
