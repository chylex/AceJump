package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.search.Tag
import org.acejump.session.SessionState

class VimJumpMode : JumpMode() {
  override fun accept(state: SessionState, acceptedTag: Tag): Boolean {
    val action = if (acceptedTag.editor.selectionModel.hasSelection())
      AceTagAction.SelectToCaret(AceTagAction.JumpToSearchStart)
    else
      AceTagAction.JumpToSearchStart
    
    state.act(action, acceptedTag, wasUpperCase, isFinal = true)
    return true
  }
}
