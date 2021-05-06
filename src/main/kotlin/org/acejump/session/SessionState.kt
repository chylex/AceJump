package org.acejump.session

import org.acejump.action.AceTagAction
import org.acejump.search.Tag

interface SessionState {
  fun type(char: Char): TypeResult
  fun act(action: AceTagAction, tag: Tag, shiftMode: Boolean, isFinal: Boolean)
}
