package org.acejump.interact.mode

import org.acejump.action.AceTagAction
import org.acejump.config.AceConfig
import org.acejump.session.SessionMode

internal object SelectMode : AbstractNavigableMode() {
  override val actionMap = mapOf(
    'Q' to AceTagAction.SelectQuery,
    'W' to AceTagAction.SelectWord,
    'H' to AceTagAction.SelectHump,
    'L' to AceTagAction.SelectLine,
    *('1'..'9').mapIndexed { index, char -> char to AceTagAction.SelectExtended(index + 1) }.toTypedArray()
  )
  
  override val modeMap
    get() = emptyMap<Char, () -> SessionMode>()
  
  override val caretColor
    get() = AceConfig.singleCaretModeColor
  
  override val actionHint = arrayOf(
    "<f>[Q]</f>uery / <f>[L]</f>ine",
    "<f>[W]</f>ord / <f>[H]</f>ump",
    "<f>[1-9] Extend Selection</f>"
  )
}
