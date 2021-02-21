package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class QuickJumpMode : SessionMode {
  override val caretColor
    get() = AceConfig.jumpModeColor
  
  private var wasUpperCase = false
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    wasUpperCase = charTyped.isUpperCase()
    return state.type(charTyped)
  }
  
  override fun accept(state: SessionState, acceptedTag: Int): Boolean {
    state.act(AceTagAction.JumpToSearchStart, acceptedTag, wasUpperCase)
    return true
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>? {
    return null
  }
}
