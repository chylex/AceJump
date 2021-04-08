package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.session.SessionState

class VimJumpMode : JumpMode() {
  override fun accept(state: SessionState, acceptedTag: Int): Boolean {
    val action = if (state.editor.selectionModel.hasSelection())
      AceTagAction.SelectToCaret(AceTagAction.JumpToSearchStart)
    else
      AceTagAction.JumpToSearchStart
    
    state.act(action, acceptedTag, wasUpperCase)
    return true
  }
}
