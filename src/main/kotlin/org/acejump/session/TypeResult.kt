package org.acejump.session

import org.acejump.modes.SessionMode
import org.acejump.search.Tag

sealed class TypeResult {
  object Nothing : TypeResult()
  class ChangeState(val state: SessionState) : TypeResult()
  class ChangeMode(val mode: SessionMode) : TypeResult()
  class AcceptTag(val tag: Tag) : TypeResult()
  object EndSession : TypeResult()
}
