package org.acejump.session.mode

sealed class VisitResult {
  object Nothing : VisitResult()
  class SetAcceptedTag(val offset: Int) : VisitResult()
  object EndSession : VisitResult()
}
