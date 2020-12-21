package org.acejump.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAwareAction
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.interact.mode.DefaultMode
import org.acejump.interact.mode.MultiCaretMode
import org.acejump.search.Pattern
import org.acejump.session.Session
import org.acejump.session.SessionManager

/**
 * Base class for keyboard-activated actions that create or update an AceJump [Session].
 */
sealed class AceKeyboardAction : DumbAwareAction() {
  final override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }
  
  final override fun actionPerformed(e: AnActionEvent) {
    invoke(SessionManager.start(e.getData(EDITOR) ?: return))
  }
  
  abstract operator fun invoke(session: Session)
  
  /**
   * Generic action type that starts a regex search.
   */
  abstract class BaseRegexSearchAction(private val pattern: Pattern, private val boundaries: Boundaries) : AceKeyboardAction() {
    override fun invoke(session: Session) = session.startRegexSearch(pattern, boundaries)
  }
  
  /**
   * Starts or ends an AceJump session.
   */
  object ActivateAceJump : AceKeyboardAction() {
    override fun invoke(session: Session) = session.toggleMode(DefaultMode)
  }
  
  /**
   * Starts or ends an AceJump session in multicaret mode.
   */
  object ActivateAceJumpMultiCaret : AceKeyboardAction() {
    override fun invoke(session: Session) = session.toggleMode(MultiCaretMode())
  }
  
  // @formatter:off
  
  object StartAllWordsMode          : BaseRegexSearchAction(Pattern.ALL_WORDS, StandardBoundaries.WHOLE_FILE)
  object StartAllWordsBackwardsMode : BaseRegexSearchAction(Pattern.ALL_WORDS, StandardBoundaries.BEFORE_CARET)
  object StartAllWordsForwardMode   : BaseRegexSearchAction(Pattern.ALL_WORDS, StandardBoundaries.AFTER_CARET)
  object StartAllLineStartsMode     : BaseRegexSearchAction(Pattern.LINE_STARTS, StandardBoundaries.WHOLE_FILE)
  object StartAllLineEndsMode       : BaseRegexSearchAction(Pattern.LINE_ENDS, StandardBoundaries.WHOLE_FILE)
  object StartAllLineIndentsMode    : BaseRegexSearchAction(Pattern.LINE_INDENTS, StandardBoundaries.WHOLE_FILE)
  object StartAllLineMarksMode      : BaseRegexSearchAction(Pattern.LINE_ALL_MARKS, StandardBoundaries.WHOLE_FILE)
  
  // @formatter:on
}
