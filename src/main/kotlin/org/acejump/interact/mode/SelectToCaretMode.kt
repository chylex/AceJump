package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionMode

internal class SelectToCaretMode(private val wrapper: (AceTagAction) -> AceTagAction) : AbstractNavigableMode() {
  override val actionMap = mapOf(
    'J' to AceTagAction.JumpToSearchStart,
    'K' to AceTagAction.JumpToSearchEnd,
    'L' to AceTagAction.JumpPastSearchEnd,
    'W' to AceTagAction.JumpToWordStartTag,
    'E' to AceTagAction.JumpToWordEndTag
  ).mapValues {
    wrapper(AceTagAction.SelectToCaret(it.value))
  }
  
  override val modeMap
    get() = emptyMap<Char, () -> SessionMode>()
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint
    get() = arrayOf(
      "<f>[J]</f> at Tag / <f>[K]</f> at Query / <f>[L]</f> past Query",
      "<f>[W]</f>ord Start / Word <f>[E]</f>nd"
    )
  
  fun wrap(wrapper: (AceTagAction) -> AceTagAction): SelectToCaretMode {
    return SelectToCaretMode { wrapper(this.wrapper(it)) }
  }
}
