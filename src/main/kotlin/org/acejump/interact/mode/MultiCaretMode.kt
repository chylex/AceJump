package org.acejump.interact.mode
import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.interact.TypeResult
import org.acejump.interact.VisitDirection
import org.acejump.interact.VisitResult
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import org.acejump.session.SessionMode
import org.acejump.session.SessionSnapshot

internal class MultiCaretMode : SessionMode {
  private companion object {
    private val actionMap = mapOf(
      'J' to AceTagAction.JumpToSearchStart,
      'K' to AceTagAction.JumpToSearchEnd,
      'L' to AceTagAction.JumpPastSearchEnd,
      'W' to AceTagAction.JumpToWordStartTag,
      'E' to AceTagAction.JumpToWordEndTag,
      'S' to AceTagAction.SelectWordOrHump
    )
  }
  
  override val caretColor
    get() = AceConfig.multiCaretModeColor
  
  override val actionHint = arrayOf(
    "<f>[J]</f>ump to Tag / <f>[K]</f> to Query / <f>[L]</f> past Query",
    "<f>[W]</f>ord Start / Word <f>[E]</f>nd / <f>[S]</f>elect Word"
  )
  
  private var isFirst = true
  private lateinit var snapshot: SessionSnapshot
  
  override fun type(editor: Editor, processor: SearchProcessor, tagger: Tagger, charTyped: Char, acceptedTag: Int?): TypeResult {
    val newQuery = processor.query.rawText + charTyped
    
    if (acceptedTag != null) {
      val action = actionMap[charTyped.toUpperCase()] ?: return TypeResult.Nothing
      
      val caretModel = editor.caretModel
      val prevCarets = caretModel.caretsAndSelections
      action(editor, processor, acceptedTag, charTyped.isUpperCase())
      snapshot.removeTag(acceptedTag)
      
      if (isFirst) {
        isFirst = false
      }
      else {
        caretModel.caretsAndSelections = prevCarets + caretModel.caretsAndSelections
      }
      
      return TypeResult.LoadSnapshot(snapshot)
    }
    
    if (!::snapshot.isInitialized && tagger.canQueryMatchAnyTag(newQuery)) {
      snapshot = SessionSnapshot(processor, tagger)
    }
    
    if (processor.type(charTyped, tagger)) {
      return TypeResult.UpdateResults
    }
    
    return TypeResult.Nothing
  }
  
  override fun visit(editor: Editor, processor: SearchProcessor, direction: VisitDirection, acceptedTag: Int?): VisitResult {
    return if (acceptedTag == null) VisitResult.EndSession else VisitResult.Nothing
  }
}
