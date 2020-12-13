package org.acejump.session.mode

import com.intellij.openapi.editor.Editor
import org.acejump.action.AceTagAction
import org.acejump.action.TagVisitor
import org.acejump.config.AceConfig
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import org.acejump.session.SessionMode

internal class SingleCaretSessionMode : SessionMode {
  private companion object {
    private val actionMap = mapOf(
      'J' to AceTagAction.JumpToSearchStart,
      'K' to AceTagAction.JumpToSearchEnd,
      'L' to AceTagAction.JumpPastSearchEnd,
      'W' to AceTagAction.JumpToWordStartTag,
      'E' to AceTagAction.JumpToWordEndTag,
      'S' to AceTagAction.SelectWordOrHump,
      'D' to AceTagAction.GoToDeclaration,
      'U' to AceTagAction.ShowUsages,
      'I' to AceTagAction.ShowIntentions
    )
  }
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint = arrayOf(
    "<f>[J]</f>ump to Tag / <f>[K]</f> to Query / <f>[L]</f> past Query",
    "<f>[W]</f>ord Start / Word <f>[E]</f>nd / <f>[S]</f>elect Word",
    "<f>[D]</f>eclaration / <f>[U]</f>sages / <f>[I]</f>ntentions"
  )
  
  override fun type(editor: Editor, processor: SearchProcessor, tagger: Tagger, charTyped: Char, acceptedTag: Int?): TypeResult {
    if (acceptedTag != null) {
      val action = actionMap[charTyped.toUpperCase()] ?: return TypeResult.Nothing
      action(editor, processor, acceptedTag, charTyped.isUpperCase())
      return TypeResult.EndSession
    }
    else if (processor.type(charTyped, tagger)) {
      return TypeResult.UpdateResults
    }
    
    return TypeResult.Nothing
  }
  
  override fun visit(editor: Editor, processor: SearchProcessor, direction: VisitDirection, acceptedTag: Int?): VisitResult {
    if (acceptedTag != null) {
      AceTagAction.JumpToSearchStart(editor, processor, acceptedTag, shiftMode = false)
      return VisitResult.EndSession
    }
    
    val onlyTagOffset = when (direction) {
      VisitDirection.BACKWARD -> TagVisitor(editor, processor).visitPrevious()
      VisitDirection.FORWARD  -> TagVisitor(editor, processor).visitNext()
    }
    
    return onlyTagOffset?.let(VisitResult::SetAcceptedTag) ?: VisitResult.Nothing
  }
}
