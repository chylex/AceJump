package org.acejump.interact.mode

import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction
import org.acejump.interact.TypeResult
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import org.acejump.session.SessionMode

internal abstract class AbstractNavigableMode : SessionMode {
  abstract val actionMap: Map<Char, AceTagAction>
  abstract val modeMap: Map<Char, () -> SessionMode>
  
  override fun type(editor: Editor, processor: SearchProcessor, tagger: Tagger, charTyped: Char, acceptedTag: Int?): TypeResult {
    if (acceptedTag != null) {
      val action = actionMap[charTyped.toUpperCase()]
      if (action != null) {
        action(editor, processor, acceptedTag, charTyped.isUpperCase())
        return TypeResult.EndSession
      }
      
      val mode = modeMap[charTyped] ?: modeMap[charTyped.toLowerCase()]
      if (mode != null) {
        return TypeResult.ChangeMode(mode())
      }
      
      return TypeResult.Nothing
    }
    
    if (processor.type(charTyped, tagger)) {
      return TypeResult.UpdateResults
    }
    
    return TypeResult.Nothing
  }
}
