package org.acejump.action

import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.modes.ActionMode
import org.acejump.modes.VimJumpMode
import org.acejump.session.Session

sealed class AceVimAction : AceKeyboardAction() {
  protected abstract val boundary: Boundaries
  
  final override fun invoke(session: Session) {
    session.defaultBoundary = boundary
    start(session)
  }
  
  protected open fun start(session: Session) {
    session.startJumpMode(::VimJumpMode)
  }
  
  class GoToDeclaration : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.GoToDeclaration, shiftMode = false) }
    }
  }
  
  class GoToTypeDeclaration : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.GoToDeclaration, shiftMode = true) }
    }
  }
  
  class ShowIntentions : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.ShowIntentions, shiftMode = false) }
    }
  }
  
  class ShowUsages : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.ShowUsages, shiftMode = false) }
    }
  }
  
  class FindUsages : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.ShowUsages, shiftMode = true) }
    }
  }
  
  class Refactor : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.Refactor, shiftMode = false) }
    }
  }
  
  class Rename : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
    override fun start(session: Session) {
      session.startJumpMode { ActionMode(AceTagAction.Refactor, shiftMode = true) }
    }
  }
}
