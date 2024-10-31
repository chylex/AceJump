package org.acejump.search

import org.acejump.countMatchingCharacters

/**
 * Defines the current search query for a session.
 */
internal sealed class SearchQuery {
  abstract val rawText: String
  
  /**
   * Returns a new query with the given character appended.
   */
  abstract fun refine(char: Char): SearchQuery
  
  /**
   * Returns how many characters the search occurrence highlight should cover.
   */
  abstract fun getHighlightLength(text: CharSequence, offset: Int): Int
  
  /**
   * Converts the query into a regular expression to find the initial matches.
   */
  abstract fun toRegex(invertUppercaseMode: Boolean): Regex?
  
  /**
   * Searches for all occurrences of a literal text query.
   * If the first character of the query is lowercase, then the entire query will be case-insensitive,
   * and only beginnings of words and camel humps will be matched.
   */
  class Literal(override val rawText: String) : SearchQuery() {
    init {
      require(rawText.isNotEmpty())
    }
    
    override fun refine(char: Char): SearchQuery {
      return Literal(rawText + char)
    }
    
    override fun getHighlightLength(text: CharSequence, offset: Int): Int {
      return text.countMatchingCharacters(offset, rawText)
    }
    
    override fun toRegex(invertUppercaseMode: Boolean): Regex {
      val firstChar = rawText.first()
      val pattern = if (firstChar.isLowerCase() xor invertUppercaseMode) {
        val fullPattern = Regex.escape(rawText)
        "(?i)$fullPattern"
      }
      else {
        val firstCharUppercasePattern = Regex.escape(firstChar.uppercase())
        val firstCharLowercasePattern = Regex.escape(firstChar.lowercase())
        val remainingPattern = if (rawText.length > 1) Regex.escape(rawText.drop(1)) else ""
        "(?:$firstCharUppercasePattern|(?<![a-zA-Z])$firstCharLowercasePattern)$remainingPattern"
      }
      
      return Regex(pattern, setOf(RegexOption.MULTILINE))
    }
  }
  
  /**
   * Searches for all matches of a regular expression.
   */
  class RegularExpression(private val pattern: String) : SearchQuery() {
    override val rawText = ""
    
    override fun refine(char: Char): SearchQuery {
      return Literal(char.toString())
    }
    
    override fun getHighlightLength(text: CharSequence, offset: Int): Int {
      return 1
    }
    
    override fun toRegex(invertUppercaseMode: Boolean): Regex {
      return Regex(pattern, setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
    }
  }
}
