package org.acejump.modes

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.isWordPart
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class ProgressiveSelectionMode : SessionMode {
  private companion object {
    private val EXPANSION_HINT = arrayOf(
      "<f>[W]</f>ord / <f>[C]</f>har / <f>[L]</f>ine / <f>[S]</f>pace"
    )
    
    private val EXPANSION_MODES = mapOf(
      'W' to SelectionMode.Word,
      'C' to SelectionMode.Char,
      'L' to SelectionMode.Line,
      'S' to SelectionMode.Space
    )
  }
  
  override val caretColor
    get() = AceConfig.jumpModeColor
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    val editor = state.editor
    val mode = EXPANSION_MODES[charTyped.toUpperCase()]
    
    if (mode != null) {
      val hintOffset = if (charTyped.isUpperCase()) {
        editor.caretModel.runForEachCaret { mode.extendLeft(editor, it) }
        editor.caretModel.allCarets.first().selectionStart
      }
      else {
        editor.caretModel.runForEachCaret { mode.extendRight(editor, it); it.moveToOffset(it.selectionEnd) }
        editor.caretModel.allCarets.last().selectionEnd
      }
      
      editor.scrollingModel.scrollTo(editor.offsetToLogicalPosition(hintOffset), ScrollType.RELATIVE)
      return TypeResult.MoveHint(hintOffset)
    }
    
    return TypeResult.Nothing
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String> {
    return EXPANSION_HINT
  }
  
  private sealed class SelectionMode {
    abstract fun extendLeft(editor: Editor, caret: Caret)
    abstract fun extendRight(editor: Editor, caret: Caret)
    
    object Word : SelectionMode() {
      override fun extendLeft(editor: Editor, caret: Caret) {
        val text = editor.immutableText
        val wordPart = when {
          caret.selectionStart == 0                 -> caret.selectionStart
          text[caret.selectionStart - 1].isWordPart -> caret.selectionStart - 1
          else                                      -> (caret.selectionStart - 1 downTo 0).find { text[it].isWordPart } ?: return
        }
        
        caret.setSelection(caret.selectionEnd, AceTagAction.JumpToWordStart.getCaretOffset(editor, wordPart, wordPart, isInsideWord = true))
      }
      
      override fun extendRight(editor: Editor, caret: Caret) {
        val text = editor.immutableText
        val wordPart = when {
          text[caret.selectionEnd].isWordPart -> caret.selectionEnd
          else                                -> (caret.selectionEnd until text.length).find { text[it].isWordPart } ?: return
        }
        
        caret.setSelection(caret.selectionStart, AceTagAction.JumpToWordEnd.getCaretOffset(editor, wordPart, wordPart, isInsideWord = true))
      }
    }
    
    object Char : SelectionMode() {
      override fun extendLeft(editor: Editor, caret: Caret) {
        caret.setSelection((caret.selectionStart - 1).coerceAtLeast(0), caret.selectionEnd)
      }
      
      override fun extendRight(editor: Editor, caret: Caret) {
        caret.setSelection(caret.selectionStart, (caret.selectionEnd + 1).coerceAtMost(editor.immutableText.length))
      }
    }
    
    object Line : SelectionMode() {
      override fun extendLeft(editor: Editor, caret: Caret) {
        val document = editor.document
        val line = document.getLineNumber(caret.selectionStart)
        val lineOffset = document.getLineStartOffset(line)
        
        if (caret.selectionStart > lineOffset) {
          caret.setSelection(lineOffset, caret.selectionEnd)
        }
        else if (line - 1 >= 0) {
          caret.setSelection(document.getLineStartOffset(line - 1), caret.selectionEnd)
        }
      }
      
      override fun extendRight(editor: Editor, caret: Caret) {
        val document = editor.document
        val line = document.getLineNumber(caret.selectionEnd)
        val lineOffset = document.getLineEndOffset(line)
        
        if (caret.selectionEnd < lineOffset) {
          caret.setSelection(caret.selectionStart, lineOffset)
        }
        else if (line + 1 < document.lineCount) {
          caret.setSelection(caret.selectionStart, document.getLineEndOffset(line + 1))
        }
      }
    }
    
    object Space : SelectionMode() {
      override fun extendLeft(editor: Editor, caret: Caret) {
        var offset = caret.selectionStart
        
        while (offset > 0 && editor.immutableText[offset - 1].isWhitespace()) {
          --offset
        }
        
        caret.setSelection(offset, caret.selectionEnd)
      }
      
      override fun extendRight(editor: Editor, caret: Caret) {
        var offset = caret.selectionEnd
        
        while (offset < editor.immutableText.length && editor.immutableText[offset].isWhitespace()) {
          ++offset
        }
        
        caret.setSelection(caret.selectionStart, offset)
      }
    }
  }
}
