package org.acejump.interact.mode

import org.acejump.action.AceTagAction

internal object DeleteMode : AbstractSelectionBasedMode() {
  override fun wrap(action: AceTagAction) = AceTagAction.Delete(action)
}
