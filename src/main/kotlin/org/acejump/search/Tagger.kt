package org.acejump.search

import com.google.common.collect.ArrayListMultimap
import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.immutableText
import org.acejump.input.KeyLayoutCache
import org.acejump.isWordPart
import org.acejump.view.TagMarker
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.max
import kotlin.math.min

/**
 * Assigns tags to search occurrences.
 * The ordering of [editors] may be used to prioritize tagging editors earlier in the list in case of conflicts.
 */
class Tagger(private val editors: List<Editor>, results: Map<Editor, IntList>) {
  private var tagMap: Map<String, Tag>
  private var typedTag = ""
  
  internal val markers: Map<Editor, Collection<TagMarker>>
    get() {
      val markers = ArrayListMultimap.create<Editor, TagMarker>(editors.size, min(tagMap.values.size, 40))
      
      for ((mark, tag) in tagMap) {
        val marker = TagMarker.create(mark, tag.offset, typedTag)
        markers.put(tag.editor, marker)
      }
      
      return markers.asMap()
    }
  
  init {
    val caches = results.keys.associateWith { EditorOffsetCache.new() }
    
    sortResults(results, caches)
    
    val tagSites = results
      .flatMap { (editor, sites) -> sites.map { site -> Tag(editor, site) } }
      .sortedWith(siteOrder(editors, caches))
    
    tagMap = KeyLayoutCache.allPossibleTags.zip(tagSites).toMap()
  }
  
  internal fun type(char: Char): TaggingResult {
    val newTypedTag = typedTag + char
    val matchingTag = tagMap[newTypedTag]
    if (matchingTag != null) {
      return TaggingResult.Accept(matchingTag)
    }
    
    val newTagMap = tagMap.filter { it.key.startsWith(newTypedTag, ignoreCase = true) }
    if (newTagMap.isEmpty()) {
      return TaggingResult.Nothing
    }
    
    typedTag = newTypedTag
    tagMap = newTagMap
    return TaggingResult.Mark(markers)
  }
  
  private companion object {
    private fun sortResults(results: Map<Editor, IntList>, caches: Map<Editor, EditorOffsetCache>) {
      for ((editor, offsets) in results) {
        val cache = caches.getValue(editor)
        
        offsets.sort { a, b ->
          val aIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(editor, a, cache)
          val bIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(editor, b, cache)
          
          when {
            aIsVisible && !bIsVisible -> -1
            bIsVisible && !aIsVisible -> 1
            else                      -> 0
          }
        }
      }
    }
    
    private fun siteOrder(editorPriority: List<Editor>, caches: Map<Editor, EditorOffsetCache>) = Comparator<Tag> { a, b ->
      val aEditor = a.editor
      val bEditor = b.editor
      
      if (aEditor !== bEditor) {
        val aEditorIndex = editorPriority.indexOf(aEditor)
        val bEditorIndex = editorPriority.indexOf(bEditor)
        // For multiple editors, prioritize them based on the provided order.
        return@Comparator if (aEditorIndex < bEditorIndex) -1 else 1
      }
      
      val aIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(aEditor, a.offset, caches.getValue(aEditor))
      val bIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(bEditor, b.offset, caches.getValue(bEditor))
      if (aIsVisible != bIsVisible) {
        // Sites in immediate view should come first.
        return@Comparator if (aIsVisible) -1 else 1
      }
      
      val aIsNotWordStart = aEditor.immutableText[max(0, a.offset - 1)].isWordPart
      val bIsNotWordStart = bEditor.immutableText[max(0, b.offset - 1)].isWordPart
      if (aIsNotWordStart != bIsNotWordStart) {
        // Ensure that the first letter of a word is prioritized for tagging.
        return@Comparator if (bIsNotWordStart) -1 else 1
      }
      
      when {
        a.offset < b.offset -> -1
        a.offset > b.offset -> 1
        else                -> 0
      }
    }
    
  }
}
