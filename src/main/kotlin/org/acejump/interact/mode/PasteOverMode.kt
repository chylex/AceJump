package org.acejump.interact.mode

import org.acejump.action.AceTagAction

internal object PasteOverMode : AbstractSelectionBasedMode() {
  override fun wrap(action: AceTagAction) = AceTagAction.Paste(action)
}
