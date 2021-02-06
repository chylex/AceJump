package org.acejump.session

import org.acejump.modes.SessionMode
import org.acejump.search.SearchProcessor

sealed class TypeResult {
  object Nothing : TypeResult()
  object RestartSearch : TypeResult()
  class UpdateResults(val processor: SearchProcessor) : TypeResult()
  class MoveHint(val offset: Int) : TypeResult()
  class ChangeMode(val mode: SessionMode) : TypeResult()
  object EndSession : TypeResult()
}
