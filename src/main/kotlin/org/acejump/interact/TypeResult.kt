package org.acejump.interact

import org.acejump.session.SessionMode
import org.acejump.session.SessionSnapshot

sealed class TypeResult {
  object Nothing : TypeResult()
  object UpdateResults : TypeResult()
  class LoadSnapshot(val snapshot: SessionSnapshot) : TypeResult()
  class ChangeMode(val mode: SessionMode) : TypeResult()
  object EndSession : TypeResult()
}
