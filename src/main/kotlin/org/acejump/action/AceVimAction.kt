package org.acejump.action

import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.modes.VimJumpMode
import org.acejump.search.Pattern
import org.acejump.session.Session

sealed class AceVimAction : AceKeyboardAction() {
  protected abstract val boundary: Boundaries
  
  override fun invoke(session: Session) {
    session.defaultBoundary = boundary
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
    
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordEndsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsAfterCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
  }
  
  class LWordEndsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsBeforeCaret : AceVimAction() {
    override val boundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
  
    override fun invoke(session: Session) {
      super.invoke(session)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
  }
}
