package org.acejump.input

import org.acejump.config.AceSettings

/**
 * Stores data specific to the selected keyboard layout. We want to assign tags with easily reachable keys first, and ideally have tags
 * with repeated keys (ex. FF, JJ) or adjacent keys (ex. GH, UJ).
 */
internal object KeyLayoutCache {
  lateinit var allowedCharsSorted: List<Char>
    private set
  
  /**
   * Called before any lazily initialized properties are used, to ensure that they are initialized even if the settings are missing.
   */
  fun ensureInitialized(settings: AceSettings) {
    if (!::allowedCharsSorted.isInitialized) {
      reset(settings)
    }
  }
  
  /**
   * Re-initializes cached data according to updated settings.
   */
  fun reset(settings: AceSettings) {
    val allowedCharList = processCharList(settings.allowedChars)
    
    allowedCharsSorted = if (allowedCharList.isEmpty()) {
      processCharList(settings.layout.allChars)
    }
    else {
      allowedCharList.sortedWith(compareBy(settings.layout.priority()))
    }
  }
  
  private fun processCharList(charList: String): List<Char> {
    return charList.toCharArray().map(Char::lowercaseChar).distinct()
  }
}
