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
      panel.minQueryLengthInt != settings.minQueryLength ||
      panel.jumpModeColor != settings.jumpModeColor ||
      panel.tagForegroundColor != settings.tagForegroundColor ||
      panel.searchHighlightColor != settings.searchHighlightColor
  
  override fun apply() {
    settings.allowedChars = panel.allowedChars
    settings.layout = panel.keyboardLayout
    settings.minQueryLength = panel.minQueryLengthInt ?: settings.minQueryLength
    panel.jumpModeColor?.let { settings.jumpModeColor = it }
    panel.tagForegroundColor?.let { settings.tagForegroundColor = it }
    panel.searchHighlightColor?.let { settings.searchHighlightColor = it }
    KeyLayoutCache.reset(settings)
  }
  
  override fun reset() = panel.reset(settings)
}
