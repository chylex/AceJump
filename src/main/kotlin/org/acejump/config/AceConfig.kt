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
    val cycleModes get() = settings.let { arrayOf(it.cycleMode1, it.cycleMode2, it.cycleMode3, it.cycleMode4) }
    val minQueryLength get() = settings.minQueryLength
    val jumpModeColor get() = settings.jumpModeColor
    val jumpEndModeColor get() = settings.jumpEndModeColor
    val targetModeColor get() = settings.targetModeColor
    val definitionModeColor get() = settings.definitionModeColor
    val textHighlightColor get() = settings.textHighlightColor
    val tagForegroundColor get() = settings.tagForegroundColor
    val tagBackgroundColor get() = settings.tagBackgroundColor
    val roundedTagCorners get() = settings.roundedTagCorners
    val searchWholeFile get() = settings.searchWholeFile
  }
  
  override fun getState(): AceSettings {
    return aceSettings
  }
  
  override fun loadState(state: AceSettings) {
    aceSettings = state
    KeyLayoutCache.reset(state)
  }
}
