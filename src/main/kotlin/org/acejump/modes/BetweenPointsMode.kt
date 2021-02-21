package org.acejump.modes

import com.intellij.openapi.editor.CaretState
import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionState
import org.acejump.session.TypeResult

class BetweenPointsMode : SessionMode {
  private companion object {
    private val TYPE_TAG_HINT = arrayOf(
      "<b>Type to Search...</b>"
    )
  
    private val ACTION_MODE_HINT = arrayOf(
      "<f>[S]</f>elect... / <f>[F]</f>rom Caret...",
      "<f>[D]</f>elete...",
      "<f>[C]</f>lone to Caret...",
      "<f>[M]</f>ove to Caret..."
    )
    
    private const val ACTION_MODE_FROM_CARET = 'F'
    
    private val ACTION_MODE_MAP = mapOf(
      'S' to ({ action: AceTagAction.BaseSelectAction -> action }),
      'D' to (AceTagAction::Delete),
      'C' to (AceTagAction::CloneToCaret),
      'M' to (AceTagAction::MoveToCaret)
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
      if (charTyped.equals(ACTION_MODE_FROM_CARET, ignoreCase = true)) {
        return TypeResult.ChangeMode(SelectFromCaretMode())
      }
      
      this.actionMode = ACTION_MODE_MAP[charTyped.toUpperCase()]
      return TypeResult.Nothing
    }
    
    if (acceptedTag == null) {
      return state.type(charTyped)
    }
    
    if (firstOffset == null) {
      val selectAction = JumpMode.SELECT_ACTION_MAP[charTyped.toUpperCase()]
      if (selectAction != null) {
        state.act(actionMode(selectAction), acceptedTag, shiftMode = charTyped.isUpperCase())
        return TypeResult.EndSession
      }
    }
    
    val jumpAction = JumpMode.JUMP_ACTION_MAP[charTyped.toUpperCase()]
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
  
  override fun accept(state: SessionState, acceptedTag: Int): Boolean {
    return false
  }
  
  override fun getHint(acceptedTag: Int?, hasQuery: Boolean): Array<String>? {
    return when {
      actionMode == null  -> ACTION_MODE_HINT
      acceptedTag == null -> TYPE_TAG_HINT.takeUnless { hasQuery }
      firstOffset == null -> JumpMode.JUMP_ALT_HINT + JumpMode.SELECT_HINT
      else                -> JumpMode.JUMP_ALT_HINT
    }
  }
}
