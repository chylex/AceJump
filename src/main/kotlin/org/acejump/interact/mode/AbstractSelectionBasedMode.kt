package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig

internal abstract class AbstractSelectionBasedMode : AbstractNavigableMode() {
  override val actionMap = SelectMode.actionMap.mapValues { wrap(it.value) }
  
  override val modeMap = SelectMode.modeMap.mapValues { { it.value().wrap(::wrap) } }
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint
    get() = SelectMode.actionHint
  
  protected abstract fun wrap(action: AceTagAction): AceTagAction
}
