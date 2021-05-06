package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.search.Tag
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

open class JumpMode : SessionMode {
  override val caretColor
    get() = AceConfig.jumpModeColor
  
  protected var wasUpperCase = false
    private set
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Tag?): TypeResult {
    wasUpperCase = charTyped.isUpperCase()
    return state.type(charTyped)
  }
  
  override fun accept(state: SessionState, acceptedTag: Tag): Boolean {
    state.act(AceTagAction.JumpToSearchStart, acceptedTag, wasUpperCase, isFinal = true)
    return true
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>? {
    return null
  }
}
