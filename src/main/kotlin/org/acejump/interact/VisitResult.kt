package org.acejump.interact

sealed class VisitResult {
  object Nothing : VisitResult()
  class SetAcceptedTag(val offset: Int) : VisitResult()
  object EndSession : VisitResult()
}
