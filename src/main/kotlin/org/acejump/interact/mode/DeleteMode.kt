package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig

internal object DeleteMode : AbstractNavigableMode() {
  override val actionMap = SelectMode.actionMap.mapValues { AceTagAction.Delete(it.value) }
  
  override val modeMap
    get() = SelectMode.modeMap
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint
    get() = SelectMode.actionHint
}
