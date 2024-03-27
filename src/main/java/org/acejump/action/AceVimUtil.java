package org.acejump.action;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.state.mode.SelectionType;

final class AceVimUtil {
  private AceVimUtil() {}

  public static void enterVisualMode(final VimEditor vim, final SelectionType mode) {
    VimPlugin.getVisualMotion().enterVisualMode(vim, mode);
  }
}
