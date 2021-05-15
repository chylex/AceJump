package org.acejump.action

import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries.AFTER_CARET
import org.acejump.boundaries.StandardBoundaries.BEFORE_CARET
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.modes.ActionMode
import org.acejump.modes.VimJumpMode
import org.acejump.search.Pattern
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
  
  class JumpToChar : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN
  }
  
  class JumpToCharAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
  }
  
  class JumpToCharBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
  }
  
  class LWordsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordEndsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
  }
  
  class LWordEndsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
    override fun start(session: Session) {
      super.start(session)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
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
