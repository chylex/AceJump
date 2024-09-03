package org.acejump.input

import org.acejump.config.AceSettings
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Stores data specific to the selected keyboard layout. We want to assign tags with easily reachable keys first, and ideally have tags
 * with repeated keys (ex. FF, JJ) or adjacent keys (ex. GH, UJ).
 */
internal object KeyLayoutCache {
  lateinit var allowedTagsSorted: List<String>
    private set
  
  /**
   * Called before any lazily initialized properties are used, to ensure that they are initialized even if the settings are missing.
   */
  fun ensureInitialized(settings: AceSettings) {
    if (!::allowedTagsSorted.isInitialized) {
      reset(settings)
    }
  }
  
  /**
   * Re-initializes cached data according to updated settings.
   */
  fun reset(settings: AceSettings) {
    val allowedChars = processCharList(settings.allowedChars).ifEmpty { processCharList(settings.layout.allChars) }
    val allowedTags = mutableSetOf<String>()
    
    for (c1 in allowedChars) {
      allowedTags.add("$c1")
      
      for (c2 in allowedChars) {
        if (c1 != c2) {
          allowedTags.add("$c1$c2")
        }
      }
    }
    
    allowedTagsSorted = allowedTags.sortedBy { rankPriority(settings.layout, it) }
  }
  
  private fun processCharList(charList: String): List<Char> {
    return charList.toCharArray().map(Char::lowercaseChar).distinct()
  }
  
  private fun rankPriority(layout: KeyLayout, tag: String): Int {
    val c1 = tag.first()
    val p1 = (1.0 + layout.priority(c1)).pow(3)
    
    if (tag.length == 1) {
      return p1.roundToInt()
    }
    
    val c2 = tag.last()
    val p2 = (1.0 + layout.priority(c2)).pow(3)
    
    val multiplier = if (layout.areOnSameSide(c1, c2)) 2 else 1
    return (((p1 * 50) + p2 + 1000) * multiplier).roundToInt()
  }
}
