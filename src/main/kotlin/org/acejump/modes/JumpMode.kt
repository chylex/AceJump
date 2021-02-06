package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class JumpMode : SessionMode {
  companion object {
    private val JUMP_HINT = arrayOf(
      "<f>[J]</f>ump / <f>[L]</f> past Query / <f>[M]</f> Line End",
      "Word <f>[S]</f>tart / <f>[E]</f>nd"
    )
    
    val JUMP_ALT_HINT = JUMP_HINT.map { it.replace("<f>[J]</f>ump ", "<f>[J]</f> at Tag ") }.toTypedArray()
    
    val JUMP_ACTION_MAP = mapOf(
      'J' to AceTagAction.JumpToSearchStart,
      'L' to AceTagAction.JumpPastSearchEnd,
      'M' to AceTagAction.JumpToLineEnd,
      'S' to AceTagAction.JumpToWordStart,
      'E' to AceTagAction.JumpToWordEnd
    )
    
    val SELECT_HINT = arrayOf(
      "Select <f>[W]</f>ord / <f>[H]</f>ump / <f>[A]</f>round",
      "Select <f>[Q]</f>uery / <f>[N]</f> Line / <f>[1-9]</f> Expansion"
    )
    
    val SELECT_ACTION_MAP = mapOf(
      'W' to AceTagAction.SelectWord,
      'H' to AceTagAction.SelectHump,
      'A' to AceTagAction.SelectAroundWord,
      'Q' to AceTagAction.SelectQuery,
      'N' to AceTagAction.SelectLine,
      *('1'..'9').mapIndexed { index, char -> char to AceTagAction.SelectExtended(index + 1) }.toTypedArray()
    )
    
    private val ALL_HINTS = arrayOf(
      *JUMP_HINT,
      *SELECT_HINT,
      "Select <f>[P]</f>rogressively...",
      "<f>[D]</f>eclaration / <f>[U]</f>sages",
      "<f>[I]</f>ntentions / <f>[R]</f>efactor"
    )
  
    private const val ACTION_SELECT_PROGRESSIVELY = 'P'
    
    private val ALL_ACTION_MAP = mapOf(
      *JUMP_ACTION_MAP.map { it.key to it.value }.toTypedArray(),
      *SELECT_ACTION_MAP.map { it.key to it.value }.toTypedArray(),
      'D' to AceTagAction.GoToDeclaration,
      'U' to AceTagAction.ShowUsages,
      'I' to AceTagAction.ShowIntentions,
      'R' to AceTagAction.Refactor
    )
  }
  
  override val caretColor
    get() = AceConfig.jumpModeColor
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    if (acceptedTag == null) {
      return state.type(charTyped)
    }
  
    val action = ALL_ACTION_MAP[charTyped.toUpperCase()]
    if (action != null) {
      state.act(action, acceptedTag, charTyped.isUpperCase())
      return TypeResult.EndSession
    }
    else if (charTyped.equals(ACTION_SELECT_PROGRESSIVELY, ignoreCase = true)) {
      state.act(AceTagAction.SelectQuery, acceptedTag, charTyped.isUpperCase())
      return TypeResult.ChangeMode(ProgressiveSelectionMode())
    }
  
    return TypeResult.Nothing
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>? {
    return ALL_HINTS.takeIf { acceptedTag != null }
  }
}
