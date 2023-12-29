package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.boundaries.Boundaries
import org.acejump.config.AceConfig
import org.acejump.search.SearchProcessor
import org.acejump.search.SearchQuery
import org.acejump.search.Tagger
import org.acejump.search.TaggingResult

sealed interface SessionState {
  fun type(char: Char): TypeResult
  
  class WaitForKey internal constructor(
    private val actions: SessionActions,
    private val jumpEditors: List<Editor>,
    private val defaultBoundary: Boundaries,
  ) : SessionState {
    override fun type(char: Char): TypeResult {
      val searchProcessor = SearchProcessor(jumpEditors, SearchQuery.Literal(char.toString(), excludeMiddlesOfWords = true), defaultBoundary)
      
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
      actions.showHighlights(searchProcessor.resultsCopy, searchProcessor.query)
    }
    
    override fun type(char: Char): TypeResult {
      return if (searchProcessor.refineQuery(char)) {
        actions.hideHighlights()
        TypeResult.ChangeState(SelectTag(actions, jumpEditors, searchProcessor))
      }
      else {
        actions.showHighlights(searchProcessor.resultsCopy, searchProcessor.query)
        TypeResult.Nothing
      }
    }
  }
  
  class SelectTag internal constructor(
    private val actions: SessionActions,
    private val jumpEditors: List<Editor>,
    private val searchProcessor: SearchProcessor,
  ) : SessionState {
    private val tagger = Tagger(jumpEditors, searchProcessor.resultsCopy)
    
    init {
      actions.setTagMarkers(tagger.markers)
    }
    
    override fun type(char: Char): TypeResult {
      if (char == ' ') {
        val query = searchProcessor.query
        if (query is SearchQuery.Literal && query.excludeMiddlesOfWords) {
          val newQuery = SearchQuery.Literal(query.rawText, excludeMiddlesOfWords = false)
          val newSearchProcessor = SearchProcessor(jumpEditors, newQuery, searchProcessor.boundaries)
          return TypeResult.ChangeState(SelectTag(actions, jumpEditors, newSearchProcessor))
        }
      }
      
      return when (val result = tagger.type(AceConfig.layout.characterRemapping.getOrDefault(char, char))) {
        is TaggingResult.Nothing -> TypeResult.Nothing
        is TaggingResult.Accept  -> TypeResult.AcceptTag(result.tag)
        is TaggingResult.Mark    -> {
          actions.setTagMarkers(result.markers)
          TypeResult.Nothing
        }
      }
    }
  }
}
