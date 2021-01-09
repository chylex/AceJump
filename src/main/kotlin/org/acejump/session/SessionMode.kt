package org.acejump.session

import java.awt.Color

interface SessionMode {
  val caretColor: Color
  
  fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult
  fun getHint(acceptedTag: Int?): Array<String>?
}
