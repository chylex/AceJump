package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionMode
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class JumpMode : SessionMode {
  private companion object {
    private val HINT_ACTIONS = arrayOf(
      "<f>[J]</f>ump to Tag / <f>[L]</f> past Query",
      "Word <f>[S]</f>tart / <f>[E]</f>nd",
      "Select <f>[W]</f>ord / <f>[H]</f>ump / <f>[Q]</f>uery / <f>[1-9]</f> Expansion",
      "<f>[D]</f>eclaration / <f>[U]</f>sages",
      "<f>[I]</f>ntentions / <f>[R]</f>efactor"
    )
    
    private val ACTION_MAP = mapOf(
      'J' to AceTagAction.JumpToSearchStart,
      'L' to AceTagAction.JumpPastSearchEnd,
      'S' to AceTagAction.JumpToWordStartTag,
      'E' to AceTagAction.JumpToWordEndTag,
      'W' to AceTagAction.SelectWord,
      'H' to AceTagAction.SelectHump,
      'Q' to AceTagAction.SelectQuery,
      'D' to AceTagAction.GoToDeclaration,
      'U' to AceTagAction.ShowUsages,
      'I' to AceTagAction.ShowIntentions,
      'R' to AceTagAction.Refactor,
      *('1'..'9').mapIndexed { index, char -> char to AceTagAction.SelectExtended(index + 1) }.toTypedArray()
    )
  }
  
  override val caretColor
    get() = AceConfig.jumpModeColor
  
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    if (acceptedTag == null) {
      return state.type(charTyped)
    }
  
    val action = ACTION_MAP[charTyped.toUpperCase()]
    if (action != null) {
      state.act(action, acceptedTag, charTyped.isUpperCase())
      return TypeResult.EndSession
    }
  
    return TypeResult.Nothing
  }
  
  override fun getHint(acceptedTag: Int?): Array<String>? {
    return HINT_ACTIONS.takeIf { acceptedTag != null }
  }
}
