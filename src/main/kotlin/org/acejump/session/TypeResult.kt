package org.acejump.session

import org.acejump.modes.SessionMode
import org.acejump.search.SearchProcessor

sealed class TypeResult {
  object Nothing : TypeResult()
  class UpdateResults(val processor: SearchProcessor) : TypeResult()
  class ChangeMode(val mode: SessionMode) : TypeResult()
  object RestartSearch : TypeResult()
  object EndSession : TypeResult()
}
