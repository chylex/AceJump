package org.acejump.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAwareAction
import org.acejump.boundaries.Boundaries
import org.acejump.openEditors
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
      val jumpEditors = project.openEditors
        .sortedBy { if (it === editor) 0 else 1 }
        .ifEmpty { listOf(editor) }
      invoke(SessionManager.start(editor, jumpEditors))
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
}
