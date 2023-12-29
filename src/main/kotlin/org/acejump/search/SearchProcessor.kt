package org.acejump.search

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.boundaries.Boundaries
import org.acejump.clone
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.matchesAt

/**
 * Searches editor text for matches of a [SearchQuery], and updates previous results when the user [refineQuery]s a character.
 */
class SearchProcessor private constructor(query: SearchQuery, val boundaries: Boundaries, private val results: MutableMap<Editor, IntArrayList>) {
  internal constructor(editors: List<Editor>, query: SearchQuery, boundaries: Boundaries) : this(query, boundaries, mutableMapOf()) {
    val regex = query.toRegex()
    
    if (regex != null) {
      for (editor in editors) {
        val offsets = IntArrayList()
        
        val offsetRange = boundaries.getOffsetRange(editor)
        var result = regex.find(editor.immutableText, offsetRange.first)
        
        while (result != null) {
          val index = result.range.first // For some reason regex matches can be out of bounds, but boundary check prevents an exception.
          val highlightEnd = index + query.getHighlightLength("", index)
          
          if (highlightEnd > offsetRange.last) {
            break
          }
          else if (boundaries.isOffsetInside(editor, index) && !editor.foldingModel.isOffsetCollapsed(index)) {
            offsets.add(index)
          }
          
          result = result.next()
        }
        
        results[editor] = offsets
      }
    }
  }
  
  internal var query = query
    private set
  
  val resultsCopy
    get() = results.clone()
  
  val isQueryFinished
    get() = query.rawText.length >= AceConfig.minQueryLength
  
  fun refineQuery(char: Char): Boolean {
    if (char == '\n') {
      return true
    }
    else {
      query = query.refine(char)
      removeObsoleteResults()
      return isQueryFinished
    }
  }
  
  /**
   * After updating the query, removes all results that no longer match the search query.
   */
  private fun removeObsoleteResults() {
    val query = query.rawText
    
    for (entry in results) {
      val editor = entry.key
      val offsetIter = entry.value.iterator()
      
      while (offsetIter.hasNext()) {
        val offset = offsetIter.nextInt()
        
        if (!editor.immutableText.matchesAt(offset, query, ignoreCase = true)) {
          offsetIter.remove()
        }
      }
    }
  }
}
