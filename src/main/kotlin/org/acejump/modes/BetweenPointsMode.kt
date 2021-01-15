package org.acejump.modes

import com.intellij.openapi.editor.CaretState
import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionMode
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class BetweenPointsMode : SessionMode {
  private companion object {
    private val HINT_TYPE_TAG = arrayOf(
      "<b>Type to Search...</b>"
    )
    
    private val HINT_ACTION_MODE = arrayOf(
      "<h>Between Points Mode</h>",
      "<f>[S]</f>elect... / <f>[D]</f>elete...",
      "<f>[C]</f>lone to Caret...",
      "<f>[M]</f>ove to Caret..."
    )
    
    private val HINT_JUMP_MODE = arrayOf(
      "<f>[J]</f> at Tag / <f>[L]</f> past Query",
      "Word <f>[S]</f>tart / Word <f>[E]</f>nd"
    )
    
    private val HINT_JUMP_OR_SELECT_MODE = HINT_JUMP_MODE + arrayOf(
      "Select <f>[W]</f>ord / <f>[H]</f>ump / <f>[Q]</f>uery / <f>[1-9]</f> Expansion"
    )
    
    private val ACTION_MODE_MAP = mapOf(
      'S' to ({ action: AceTagAction.BaseSelectAction -> action }),
      'D' to (AceTagAction::Delete),
      'C' to (AceTagAction::CloneToCaret),
      'M' to (AceTagAction::MoveToCaret)
    )
    
    private val JUMP_MODE_MAP = mapOf(
      'J' to AceTagAction.JumpToSearchStart,
      'L' to AceTagAction.JumpPastSearchEnd,
      'S' to AceTagAction.JumpToWordStartTag,
      'E' to AceTagAction.JumpToWordEndTag
    )
    
    private val SELECTION_MODE_MAP = mapOf(
      'W' to AceTagAction.SelectWord,
      'H' to AceTagAction.SelectHump,
      'Q' to AceTagAction.SelectQuery,
      *('1'..'9').mapIndexed { index, char -> char to AceTagAction.SelectExtended(index + 1) }.toTypedArray()
    )
  }
  
  override val caretColor
    get() = AceConfig.betweenPointsModeColor
  
  private var actionMode: ((AceTagAction.BaseSelectAction) -> AceTagAction)? = null
  private var originalCarets: List<CaretState>? = null
  private var firstOffset: Int? = null
  
  override fun type(state: SessionState, charTyped: Char, acceptedTag: Int?): TypeResult {
    val actionMode = actionMode
    if (actionMode == null) {
      this.actionMode = ACTION_MODE_MAP[charTyped.toUpperCase()]
      return TypeResult.Nothing
    }
    
    if (acceptedTag == null) {
      return state.type(charTyped)
    }
    
    if (firstOffset == null) {
      val selectAction = SELECTION_MODE_MAP[charTyped.toUpperCase()]
      if (selectAction != null) {
        state.act(actionMode(selectAction), acceptedTag, shiftMode = charTyped.isUpperCase())
        return TypeResult.EndSession
      }
    }
    
    val jumpAction = JUMP_MODE_MAP[charTyped.toUpperCase()]
    if (jumpAction == null) {
      return TypeResult.Nothing
    }
  
    val firstOffset = firstOffset
    if (firstOffset == null) {
      val caretModel = state.editor.caretModel
      this.originalCarets = caretModel.caretsAndSelections
      
      state.act(jumpAction, acceptedTag, shiftMode = false)
      this.firstOffset = caretModel.offset
      return TypeResult.RestartSearch
    }
    
    originalCarets?.let { state.editor.caretModel.caretsAndSelections = it }
    state.act(actionMode(AceTagAction.SelectBetweenPoints(firstOffset, jumpAction)), acceptedTag, shiftMode = charTyped.isUpperCase())
    
    return TypeResult.EndSession
  }
  
  override fun getHint(acceptedTag: Int?): Array<String> {
    return when {
      actionMode == null  -> HINT_ACTION_MODE
      acceptedTag == null -> HINT_TYPE_TAG
      firstOffset == null -> HINT_JUMP_OR_SELECT_MODE
      else                -> HINT_JUMP_MODE
    }
  }
}
