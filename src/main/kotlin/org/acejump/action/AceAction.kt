package org.acejump.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAwareAction
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.input.JumpMode
import org.acejump.search.Pattern
import org.acejump.session.Session
import org.acejump.session.SessionManager

/**
 * Base class for keyboard-activated actions that create or update an AceJump [Session].
 */
sealed class AceAction : DumbAwareAction() {
  final override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }
  
  final override fun actionPerformed(e: AnActionEvent) {
    invoke(SessionManager.start(e.getData(EDITOR) ?: return))
  }
  
  abstract operator fun invoke(session: Session)
  
  /**
   * Generic action type that toggles a specific [JumpMode].
   */
  abstract class BaseToggleJumpModeAction(private val mode: JumpMode) : AceAction() {
    final override fun invoke(session: Session) = session.toggleJumpMode(mode)
  }
  
  /**
   * Generic action type that starts a regex search.
   */
  abstract class BaseRegexSearchAction(private val pattern: Pattern, private val boundaries: Boundaries) : AceAction() {
    override fun invoke(session: Session) = session.startRegexSearch(pattern, boundaries)
  }
  
  /**
   * Initiates an AceJump session in the first [JumpMode], or cycles to the next [JumpMode] as defined in configuration.
   */
  object ActivateOrCycleMode : AceAction() {
    override fun invoke(session: Session) = session.cycleNextJumpMode()
  }
  
  /**
   * Initiates an AceJump session in the last [JumpMode], or cycles to the previous [JumpMode] as defined in configuration.
   */
  object ActivateOrReverseCycleMode : AceAction() {
    override fun invoke(session: Session) = session.cyclePreviousJumpMode()
  }
  
  // @formatter:off
  
  object ToggleJumpMode        : BaseToggleJumpModeAction(JumpMode.JUMP)
  object ToggleJumpEndMode     : BaseToggleJumpModeAction(JumpMode.JUMP_END)
  object ToggleTargetMode      : BaseToggleJumpModeAction(JumpMode.TARGET)
  object ToggleDeclarationMode : BaseToggleJumpModeAction(JumpMode.DEFINE)
  
  object StartAllWordsMode          : BaseRegexSearchAction(Pattern.ALL_WORDS, VISIBLE_ON_SCREEN)
  object StartAllWordsBackwardsMode : BaseRegexSearchAction(Pattern.ALL_WORDS, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  object StartAllWordsForwardMode   : BaseRegexSearchAction(Pattern.ALL_WORDS, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  object StartAllLineStartsMode     : BaseRegexSearchAction(Pattern.LINE_STARTS, VISIBLE_ON_SCREEN)
  object StartAllLineEndsMode       : BaseRegexSearchAction(Pattern.LINE_ENDS, VISIBLE_ON_SCREEN)
  object StartAllLineIndentsMode    : BaseRegexSearchAction(Pattern.LINE_INDENTS, VISIBLE_ON_SCREEN)
  object StartAllLineMarksMode      : BaseRegexSearchAction(Pattern.LINE_ALL_MARKS, VISIBLE_ON_SCREEN)
  
  // @formatter:on
}
