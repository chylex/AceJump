package org.acejump.input

import org.acejump.action.AceTagAction

internal object TagActionKeys {
  private val map = mapOf(
    'J' to AceTagAction.JumpToSearchStart,
    'K' to AceTagAction.JumpToSearchEnd,
    'L' to AceTagAction.JumpPastSearchEnd,
    'W' to AceTagAction.JumpToWordStartTag,
    'E' to AceTagAction.JumpToWordEndTag,
    'S' to AceTagAction.SelectWordOrHump,
    'D' to AceTagAction.GoToDeclaration,
    'U' to AceTagAction.ShowUsages,
    'I' to AceTagAction.ShowIntentions,
    'C' to AceTagAction.AddRemoveCaret
  )
  
  val hint = arrayOf(
    "<f>[J]</f>ump to Tag / <f>[K]</f> to Query / <f>[L]</f> past Query",
    "<f>[W]</f>ord Start / Word <f>[E]</f>nd / <f>[S]</f>elect Word",
    "<f>[D]</f>eclaration / <f>[U]</f>sages / <f>[I]</f>ntentions / <f>[C]</f>aret"
  )
  
  operator fun get(char: Char): AceTagAction? {
    return map[char.toUpperCase()]
  }
}
