package org.acejump.vim

import com.intellij.openapi.editor.Editor
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.openEditors
import org.acejump.search.Pattern
import org.acejump.session.Session

sealed class AceVimMode {
  open val boundaries: Boundaries
    get() = StandardBoundaries.VISIBLE_ON_SCREEN
  
  open fun getJumpEditors(mainEditor: Editor): List<Editor> {
    return listOf(mainEditor)
  }
  
  open fun setupSession(editor: Editor, session: Session) {}
  open fun finishSession(editor: Editor, session: Session) {}
  
  class Jump(override val boundaries: Boundaries) : AceVimMode()
  
  object JumpAllEditors : AceVimMode() {
    override fun getJumpEditors(mainEditor: Editor): List<Editor> {
      val project = mainEditor.project ?: return super.getJumpEditors(mainEditor)
      
      return project.openEditors
        .sortedBy { if (it === mainEditor) 0 else 1 }
        .ifEmpty { listOf(mainEditor) }
    }
  }
  
  class JumpTillForward(override val boundaries: Boundaries) : AceVimMode() {
    override fun finishSession(editor: Editor, session: Session) {
      val document = editor.document
      
      for (caret in editor.caretModel.allCarets) {
        val offset = caret.offset
        if (offset > document.getLineStartOffset(document.getLineNumber(offset))) {
          caret.moveToOffset(offset - 1, false)
        }
      }
    }
  }
  
  class JumpTillBackward(override val boundaries: Boundaries) : AceVimMode() {
    override fun finishSession(editor: Editor, session: Session) {
      val document = editor.document
      
      for (caret in editor.caretModel.allCarets) {
        val offset = caret.offset
        if (offset < document.getLineEndOffset(document.getLineNumber(offset))) {
          caret.moveToOffset(offset + 1, false)
        }
      }
    }
  }
  
  class JumpToPattern(private val pattern: Pattern, override val boundaries: Boundaries) : AceVimMode() {
    override fun setupSession(editor: Editor, session: Session) {
      session.startRegexSearch(pattern)
    }
  }
}
