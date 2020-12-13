package org.acejump.session

import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger

/**
 * Saves the current search and tag state.
 */
class SessionSnapshot(processor: SearchProcessor?, tagger: Tagger) {
  private val processor = processor?.clone()
  private val tagger = tagger.clone()
  
  val savedProcessor
    get() = processor?.clone()
  
  val savedTagger
    get() = tagger.clone()
  
  fun removeTag(offset: Int) {
    processor?.let { it.results.removeInt(it.results.indexOf(offset)) }
  }
}
