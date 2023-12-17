package org.acejump.input

import org.acejump.config.AceSettings

/**
 * Stores data specific to the selected keyboard layout. We want to assign tags with easily reachable keys first, and ideally have tags
 * with repeated keys (ex. FF, JJ) or adjacent keys (ex. GH, UJ).
 */
internal object KeyLayoutCache {
  /**
   * Returns all possible two key tags, pre-sorted according to [tagOrder].
   */
  lateinit var allPossibleTagsLowercase: List<String>
    private set
  
  /**
   * Called before any lazily initialized properties are used, to ensure that they are initialized even if the settings are missing.
   */
  fun ensureInitialized(settings: AceSettings) {
    if (!::allPossibleTagsLowercase.isInitialized) {
      reset(settings)
    }
  }
  
  /**
   * Re-initializes cached data according to updated settings.
   */
  fun reset(settings: AceSettings) {
    @Suppress("ConvertLambdaToReference")
    val allSuffixChars = processCharList(settings.allowedChars).ifEmpty { processCharList(settings.layout.allChars).toList() }
    val allPrefixChars = processCharList(settings.prefixChars).filterNot(allSuffixChars::contains).plus("")
    
    val tagOrder = compareBy(
      String::length,
      { if (it.length == 1) Int.MIN_VALUE else allPrefixChars.indexOf(it.first().toString()) },
      settings.layout.priority(String::last)
    )
    
    allPossibleTagsLowercase = allSuffixChars
      .flatMap { suffix -> allPrefixChars.map { prefix -> "$prefix$suffix" } }
      .sortedWith(tagOrder)
  }
  
  private fun processCharList(charList: String): Set<String> {
    return charList.toCharArray().map(Char::lowercase).toSet()
  }
}
