package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction
import org.acejump.boundaries.Boundaries
import org.acejump.search.SearchProcessor
import org.acejump.search.Tag
import org.acejump.search.Tagger

internal class SessionStateImpl(
  private val jumpEditors: List<Editor>,
  private val tagger: Tagger,
  private val defaultBoundary: Boundaries,
  processor: SearchProcessor? = null
) : SessionState {
  internal var currentProcessor: SearchProcessor? = processor
  
  override fun type(char: Char): TypeResult {
    val processor = currentProcessor
    
    if (processor == null) {
      val newProcessor = SearchProcessor.fromChar(jumpEditors, char, defaultBoundary)
      return TypeResult.UpdateResults(newProcessor.also { currentProcessor = it })
    }
    
    if (processor.type(char, tagger)) {
      return TypeResult.UpdateResults(processor)
    }
    
    return TypeResult.Nothing
  }
  
  override fun act(action: AceTagAction, tag: Tag, shiftMode: Boolean, isFinal: Boolean) {
    currentProcessor?.let { action(tag.editor, it, tag.offset, shiftMode, isFinal) }
  }
}
