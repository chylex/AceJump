package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction

interface SessionState {
  val editor: Editor
  fun type(char: Char): TypeResult
  fun act(action: AceTagAction, offset: Int, shiftMode: Boolean)
}
