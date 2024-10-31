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
      panel.invertUppercaseMode != settings.invertUppercaseMode ||
      panel.editorFadeOpacityPercent != settings.editorFadeOpacity ||
      panel.jumpModeColor != settings.jumpModeColor ||
      panel.tagForegroundColor1 != settings.tagForegroundColor1 ||
      panel.tagForegroundColor2 != settings.tagForegroundColor2 ||
      panel.searchHighlightColor != settings.searchHighlightColor
  
  override fun apply() {
    settings.allowedChars = panel.allowedChars
    settings.layout = panel.keyboardLayout
    settings.minQueryLength = panel.minQueryLengthInt ?: settings.minQueryLength
    settings.invertUppercaseMode = panel.invertUppercaseMode
    settings.editorFadeOpacity = panel.editorFadeOpacityPercent
    panel.jumpModeColor?.let { settings.jumpModeColor = it }
    panel.tagForegroundColor1?.let { settings.tagForegroundColor1 = it }
    panel.tagForegroundColor2?.let { settings.tagForegroundColor2 = it }
    panel.searchHighlightColor?.let { settings.searchHighlightColor = it }
    KeyLayoutCache.reset(settings)
  }
  
  override fun reset() = panel.reset(settings)
}
