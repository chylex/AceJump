package org.acejump.modes

import org.acejump.session.SessionState
import org.acejump.session.TypeResult
import java.awt.Color

interface SessionMode {
  val caretColor: Color
  
  fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult
  fun accept(state: SessionState, acceptedTag: Int): Boolean
  fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>?
}
