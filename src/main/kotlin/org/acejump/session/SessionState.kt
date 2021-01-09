package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction
import org.acejump.boundaries.StandardBoundaries
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger

class SessionState(private val editor: Editor, private val tagger: Tagger, processor: SearchProcessor? = null) {
  private var currentProcessor: SearchProcessor? = processor
  
  fun type(char: Char): TypeResult {
    val processor = currentProcessor
    
    if (processor == null) {
      val newProcessor = SearchProcessor.fromChar(editor, char, StandardBoundaries.VISIBLE_ON_SCREEN)
      return TypeResult.UpdateResults(newProcessor.also { currentProcessor = it })
    }
    
    if (processor.type(char, tagger)) {
      return TypeResult.UpdateResults(processor)
    }
    
    return TypeResult.Nothing
  }
  
  fun act(action: AceTagAction, offset: Int, shiftMode: Boolean) {
    currentProcessor?.let { action(editor, it, offset, shiftMode) }
  }
}
