package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class SelectFromCaretMode : SessionMode {
  override val caretColor
    get() = AceConfig.fromCaretModeColor
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    if (acceptedTag == null) {
      return state.type(charTyped)
    }
    
    val jumpAction = JumpMode.JUMP_ACTION_MAP[charTyped.toUpperCase()]
    if (jumpAction == null) {
      return TypeResult.Nothing
    }
    
    state.act(AceTagAction.SelectToCaret(jumpAction), acceptedTag, shiftMode = charTyped.isUpperCase())
    return TypeResult.EndSession
  }
  
  override fun accept(state: SessionState, acceptedTag: Int): Boolean {
    return false
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>? {
    return JumpMode.JUMP_ALT_HINT.takeIf { acceptedTag != null }
  }
}
