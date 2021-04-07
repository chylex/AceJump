package org.acejump.action

import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.search.Pattern
import org.acejump.session.Session

sealed class AceVimAction : AceKeyboardAction() {
  class JumpToChar : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN
      session.startJumpMode()
    }
  }
  
  class JumpToCharAfterCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
      session.startJumpMode()
    }
  }

  class JumpToCharBeforeCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
      session.startJumpMode()
    }
  }
  
  class LWordsAfterCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsAfterCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordsBeforeCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
      session.startRegexSearch(Pattern.VIM_LWORD)
    }
  }
  
  class UWordsBeforeCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
      session.startRegexSearch(Pattern.VIM_UWORD)
    }
  }
  
  class LWordEndsAfterCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsAfterCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(AFTER_CARET)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
  }
  
  class LWordEndsBeforeCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
      session.startRegexSearch(Pattern.VIM_LWORD_END)
    }
  }
  
  class UWordEndsBeforeCaret : AceVimAction() {
    override fun invoke(session: Session) {
      session.defaultBoundary = VISIBLE_ON_SCREEN.intersection(BEFORE_CARET)
      session.startRegexSearch(Pattern.VIM_UWORD_END)
    }
  }
}
