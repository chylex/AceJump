package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.interact.TypeResult
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import java.awt.Color

interface SessionMode {
  val caretColor: Color
  val actionHint: Array<String>
  
  fun type(editor: Editor, processor: SearchProcessor, tagger: Tagger, charTyped: Char, acceptedTag: Int?): TypeResult
  
  fun isSame(mode: SessionMode): Boolean {
    return javaClass == mode.javaClass
  }
}
