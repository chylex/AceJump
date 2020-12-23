package org.acejump.interact.mode

import org.acejump.action.AceTagAction

internal object CutMode : AbstractSelectionBasedMode() {
  override fun wrap(action: AceTagAction) = AceTagAction.Cut(action)
}
