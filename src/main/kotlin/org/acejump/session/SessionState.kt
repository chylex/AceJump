package org.acejump.session

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.boundaries.Boundaries
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import org.acejump.search.TaggingResult
import org.acejump.view.TagMarker

sealed interface SessionState {
  fun type(char: Char): TypeResult
  
  class WaitForKey internal constructor(
    private val actions: SessionActions,
    private val jumpEditors: List<Editor>,
    private val defaultBoundary: Boundaries,
  ) : SessionState {
    override fun type(char: Char): TypeResult {
      val searchProcessor = SearchProcessor.fromString(jumpEditors, char.toString(), defaultBoundary)
      
      return if (searchProcessor.isQueryFinished) {
        TypeResult.ChangeState(SelectTag(actions, jumpEditors, searchProcessor))
      }
      else {
        TypeResult.ChangeState(RefineSearchQuery(actions, jumpEditors, searchProcessor))
      }
    }
  }
  
  class RefineSearchQuery internal constructor(
    private val actions: SessionActions,
    private val jumpEditors: List<Editor>,
    private val searchProcessor: SearchProcessor,
  ) : SessionState {
    init {
      actions.highlight(searchProcessor.resultsCopy, searchProcessor.query)
    }
    
    override fun type(char: Char): TypeResult {
      return if (searchProcessor.refineQuery(char)) {
        TypeResult.ChangeState(SelectTag(actions, jumpEditors, searchProcessor))
      }
      else {
        actions.highlight(searchProcessor.resultsCopy, searchProcessor.query)
        TypeResult.Nothing
      }
    }
  }
  
  class SelectTag internal constructor(
    private val actions: SessionActions,
    jumpEditors: List<Editor>,
    searchProcessor: SearchProcessor,
  ) : SessionState {
    private val tagger = Tagger(jumpEditors, searchProcessor.resultsCopy)
    private val query = searchProcessor.query
    
    init {
      showTagMarkers(tagger.markers)
    }
    
    override fun type(char: Char): TypeResult {
      return when (val result = tagger.type(char)) {
        is TaggingResult.Nothing -> TypeResult.Nothing
        is TaggingResult.Accept  -> TypeResult.AcceptTag(result.tag)
        is TaggingResult.Mark    -> {
          showTagMarkers(result.markers)
          TypeResult.Nothing
        }
      }
    }
    
    private fun showTagMarkers(markers: Map<Editor, Collection<TagMarker>>) {
      actions.highlight(markers.mapValues { getMarkerOffsets(it.value) }, query)
      actions.tag(markers)
    }
    
    private fun getMarkerOffsets(markers: Collection<TagMarker>): IntArrayList {
      return IntArrayList(markers.size).apply {
        for (marker in markers) {
          add(marker.offset)
        }
      }
    }
  }
}
