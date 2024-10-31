package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.acejump.input.KeyLayoutCache

/**
 * Ensures consistiency between [AceSettings] and [AceSettingsPanel]. Persists the state of the AceJump IDE settings across IDE restarts.
 * [https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html]
 */
@State(name = "AceConfig", storages = [(Storage("\$APP_CONFIG\$/AceJump.xml"))])
class AceConfig : PersistentStateComponent<AceSettings> {
  private var aceSettings = AceSettings()
  
  companion object {
    val settings
      get() = ServiceManager.getService(AceConfig::class.java).aceSettings
    
    val layout get() = settings.layout
    val minQueryLength get() = settings.minQueryLength
    val invertUppercaseMode get() = settings.invertUppercaseMode
    val editorFadeOpacity get() = settings.editorFadeOpacity
    val jumpModeColor get() = settings.jumpModeColor
    val tagForegroundColor1 get() = settings.tagForegroundColor1
    val tagForegroundColor2 get() = settings.tagForegroundColor2
    val searchHighlightColor get() = settings.searchHighlightColor
  }
  
  override fun getState(): AceSettings {
    return aceSettings
  }
  
  override fun loadState(state: AceSettings) {
    aceSettings = state
    KeyLayoutCache.reset(state)
  }
}
