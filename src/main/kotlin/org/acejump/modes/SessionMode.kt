package org.acejump.modes

import org.acejump.search.Tag
import org.acejump.session.SessionState
import org.acejump.session.TypeResult
import java.awt.Color

interface SessionMode {
  val caretColor: Color
  
  fun type(state: SessionState, charTyped: Char, acceptedTag: Tag?): TypeResult
  fun accept(state: SessionState, acceptedTag: Tag)
}
