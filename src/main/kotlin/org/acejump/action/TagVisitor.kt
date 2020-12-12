package org.acejump.action

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.SelectionModel
import org.acejump.search.SearchProcessor
import kotlin.math.abs

/**
 * Enables navigation between currently active tags.
 */
internal class TagVisitor(private val editor: Editor, private val searchProcessor: SearchProcessor) {
  /**
   * Places caret at the closest tag following the caret position.
   * If the caret is at or past the last tag, it moves to the first tag instead.
   * If there is only one tag, it returns its offset.
   */
  fun visitNext(): Int? {
    return visit(SelectionModel::getSelectionEnd) { if (it < 0) -it - 1 else it + 1 }
  }
  
  /**
   * Places caret at the closest tag preceding the caret position.
   * If the caret is at or before the first tag, it moves to the last tag instead.
   * If there is only one tag, it returns its offset.
   */
  fun visitPrevious(): Int? {
    return visit(SelectionModel::getSelectionStart) { if (it < 0) -it - 2 else it - 1 }
  }
  
  /**
   * Scrolls to the closest result to the caret.
   */
  fun scrollToClosest() {
    val caret = editor.caretModel.offset
    val results = searchProcessor.results.takeUnless { it.isEmpty } ?: return
    val index = results.binarySearch(caret).let { if (it < 0) -it - 1 else it }
    
    val targetOffset = listOfNotNull(
      results.getOrNull(index - 1),
      results.getOrNull(index)
    ).minBy {
      abs(it - caret)
    }
    
    if (targetOffset != null) {
      editor.scrollingModel.scrollTo(editor.offsetToLogicalPosition(targetOffset), ScrollType.RELATIVE)
    }
  }
  
  private inline fun visit(caretPosition: SelectionModel.() -> Int, indexModifier: (Int) -> Int): Int? {
    val results = searchProcessor.results.takeUnless { it.isEmpty } ?: return null
    val nextIndex = indexModifier(results.binarySearch(caretPosition(editor.selectionModel)))
    
    val targetOffset = results.getInt(when {
      nextIndex < 0                 -> results.lastIndex
      nextIndex > results.lastIndex -> 0
      else                          -> nextIndex
    })
    
    AceTagAction.JumpToSearchStart(editor, searchProcessor, targetOffset, shiftMode = false)
    editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
    return targetOffset.takeIf { results.size == 1 }
  }
}
