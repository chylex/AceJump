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
    's' to { SelectMode },
    'S' to { SelectToCaretMode() },
    'x' to { CutMode },
    'X' to { SelectToCaretMode(CutMode::wrap) },
    'c' to { CopyMode },
    'C' to { SelectToCaretMode(CopyMode::wrap) },
    'p' to { PasteAtMode },
    'o' to { PasteOverMode },
    'O' to { SelectToCaretMode(PasteOverMode::wrap) },
    'd' to { DeleteMode },
    'D' to { SelectToCaretMode(DeleteMode::wrap) }
  )
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint = arrayOf(
    "<f>[J]</f>ump to Tag / <f>[K]</f> to Query / <f>[L]</f> past Query",
    "<f>[W]</f>ord Start / Word <f>[E]</f>nd",
    "<f>[S]</f>elect... / <f>[D]</f>elete...",
    "<f>[X]</f> Cut... / <f>[C]</f>opy... / <f>[P]</f>aste... / Paste <f>[O]</f>ver...",
    "<f>[B]</f> Declaration / <f>[U]</f>sages / <f>[I]</f>ntentions / <f>[R]</f>efactor"
  )
}
