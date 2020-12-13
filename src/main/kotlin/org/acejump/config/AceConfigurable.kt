package org.acejump.config

import com.intellij.openapi.options.Configurable
import org.acejump.config.AceConfig.Companion.settings
import org.acejump.input.KeyLayoutCache

class AceConfigurable : Configurable {
  private val panel by lazy(::AceSettingsPanel)
  
  override fun getDisplayName() = "AceJump"
  
  override fun createComponent() = panel.rootPanel
  
  override fun isModified() =
    panel.allowedChars != settings.allowedChars ||
      panel.keyboardLayout != settings.layout ||
      panel.singleCaretModeColor != settings.singleCaretModeColor ||
      panel.multiCaretModeColor != settings.multiCaretModeColor ||
      panel.textHighlightColor != settings.textHighlightColor ||
      panel.tagForegroundColor != settings.tagForegroundColor ||
      panel.tagBackgroundColor != settings.tagBackgroundColor ||
      panel.acceptedTagColor != settings.acceptedTagColor ||
      panel.roundedTagCorners != settings.roundedTagCorners ||
      panel.searchWholeFile != settings.searchWholeFile
  
  override fun apply() {
    settings.allowedChars = panel.allowedChars
    settings.layout = panel.keyboardLayout
    panel.singleCaretModeColor?.let { settings.singleCaretModeColor = it }
    panel.multiCaretModeColor?.let { settings.multiCaretModeColor = it }
    panel.textHighlightColor?.let { settings.textHighlightColor = it }
    panel.tagForegroundColor?.let { settings.tagForegroundColor = it }
    panel.tagBackgroundColor?.let { settings.tagBackgroundColor = it }
    panel.acceptedTagColor?.let { settings.acceptedTagColor = it }
    settings.roundedTagCorners = panel.roundedTagCorners
    settings.searchWholeFile = panel.searchWholeFile
    KeyLayoutCache.reset(settings)
  }
  
  override fun reset() = panel.reset(settings)
}
