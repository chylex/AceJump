package org.acejump.modes

import org.acejump.action.AceTagAction
import org.acejump.session.SessionState

class ActionMode(private val action: AceTagAction, private val shiftMode: Boolean) : JumpMode() {
  override fun accept(state: SessionState, acceptedTag: Int): Boolean {
    state.act(action, acceptedTag, shiftMode)
    return true
  }
}
