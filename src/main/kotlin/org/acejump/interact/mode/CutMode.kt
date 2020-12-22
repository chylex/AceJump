package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig

internal object CutMode : AbstractNavigableMode() {
  override val actionMap = SelectMode.actionMap.mapValues { AceTagAction.Cut(it.value) }
  
  override val modeMap = SelectMode.modeMap.mapValues { { it.value().wrap(AceTagAction::Cut) } }
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint
    get() = SelectMode.actionHint
}
