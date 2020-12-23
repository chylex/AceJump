package org.acejump.interact.mode

import org.acejump.action.AceTagAction

internal object CopyMode : AbstractSelectionBasedMode() {
  override fun wrap(action: AceTagAction) = AceTagAction.Copy(action)
}
