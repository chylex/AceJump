package org.acejump.boundaries

import com.intellij.openapi.editor.Editor

enum class StandardBoundaries : Boundaries {
  VISIBLE_ON_SCREEN {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      val (topLeft, bottomRight) = cache.visibleArea(editor)
      val startOffset = cache.xyToOffset(editor, topLeft)
      val endOffset = cache.xyToOffset(editor, bottomRight)
      
      return startOffset..endOffset
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return cache.isVisible(editor, offset)
    }
  },
  
  BEFORE_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      return 0 until editor.caretModel.offset
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset < editor.caretModel.offset
    }
  },
  
  AFTER_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      return (editor.caretModel.offset + 1) until editor.document.textLength
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset > editor.caretModel.offset
    }
  },
  
  CARET_LINE {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      val document = editor.document
      val offset = editor.caretModel.offset
      val line = document.getLineNumber(offset)
      return document.getLineStartOffset(line)..document.getLineEndOffset(line)
    }
  
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset in getOffsetRange(editor, cache)
    }
  }
}
