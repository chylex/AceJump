package org.acejump.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.IncorrectOperationException
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.search.Pattern
import org.acejump.session.Session
import org.acejump.session.SessionManager

/**
 * Base class for keyboard-activated actions that create or update an AceJump [Session].
 */
abstract class AceKeyboardAction : DumbAwareAction() {
  final override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }
  
  final override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(EDITOR) ?: return
    val project = e.project
    
    if (project != null) {
      try {
        val openEditors = FileEditorManagerEx.getInstanceEx(project)
          .splitters
          .selectedEditors
          .mapNotNull { (it as? TextEditor)?.editor }
          .sortedBy { if (it === editor) 0 else 1 }
        invoke(SessionManager.start(editor, openEditors))
      } catch (e: IncorrectOperationException) {
        invoke(SessionManager.start(editor))
      }
    }
    else {
      invoke(SessionManager.start(editor))
    }
  }
  
  abstract operator fun invoke(session: Session)
  
  /**
   * Generic action type that starts a regex search.
   */
  abstract class BaseRegexSearchAction(private val pattern: Pattern, private val boundaries: Boundaries) : AceKeyboardAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = boundaries
      session.startRegexSearch(pattern)
    }
  }
  
  /**
   * Starts or ends an AceJump session in quick jump mode.
   */
  object ActivateAceJump : AceKeyboardAction() {
    override fun invoke(session: Session) = session.startJumpMode()
  }
  
  /**
   * Starts or cycles main AceJump modes.
   */
  object ActivateAceJumpSpecial : AceKeyboardAction() {
    override fun invoke(session: Session) = session.startOrCycleSpecialModes()
  }
  
  // @formatter:off
  
  object StartAllWordsMode          : BaseRegexSearchAction(Pattern.ALL_WORDS, VISIBLE_ON_SCREEN)
  object StartAllWordsBackwardsMode : BaseRegexSearchAction(Pattern.ALL_WORDS, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  object StartAllWordsForwardMode   : BaseRegexSearchAction(Pattern.ALL_WORDS, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  object StartAllLineStartsMode     : BaseRegexSearchAction(Pattern.LINE_STARTS, VISIBLE_ON_SCREEN)
  object StartAllLineEndsMode       : BaseRegexSearchAction(Pattern.LINE_ENDS, VISIBLE_ON_SCREEN)
  object StartAllLineIndentsMode    : BaseRegexSearchAction(Pattern.LINE_INDENTS, VISIBLE_ON_SCREEN)
  object StartAllLineMarksMode      : BaseRegexSearchAction(Pattern.LINE_ALL_MARKS, VISIBLE_ON_SCREEN)
  
  // @formatter:on
}
