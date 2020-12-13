package org.acejump.session.mode

import org.acejump.session.SessionSnapshot

sealed class TypeResult {
  object Nothing : TypeResult()
  object UpdateResults : TypeResult()
  class LoadSnapshot(val snapshot: SessionSnapshot) : TypeResult()
  object EndSession : TypeResult()
}
