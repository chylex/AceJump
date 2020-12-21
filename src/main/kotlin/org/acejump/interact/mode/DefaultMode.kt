package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig

internal object DefaultMode : AbstractNavigableMode() {
  override val actionMap = mapOf(
    'J' to AceTagAction.JumpToSearchStart,
    'K' to AceTagAction.JumpToSearchEnd,
    'L' to AceTagAction.JumpPastSearchEnd,
    'W' to AceTagAction.JumpToWordStartTag,
    'E' to AceTagAction.JumpToWordEndTag,
    'B' to AceTagAction.GoToDeclaration,
    'U' to AceTagAction.ShowUsages,
    'I' to AceTagAction.ShowIntentions,
    'R' to AceTagAction.Refactor
  )
  
  override val modeMap = mapOf(
    'S' to { SelectMode },
    'D' to { DeleteMode }
  )
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint = arrayOf(
    "<f>[J]</f>ump to Tag / <f>[K]</f> to Query / <f>[L]</f> past Query",
    "<f>[W]</f>ord Start / Word <f>[E]</f>nd",
    "<f>[S]</f>elect... / <f>[D]</f>elete...",
    "<f>[B]</f> Declaration / <f>[U]</f>sages / <f>[I]</f>ntentions / <f>[R]</f>efactor"
  )
}
