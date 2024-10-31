package org.acejump.session

import com.intellij.openapi.editor.Editor
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
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
    private val invertUppercaseMode: Boolean,
  ) : SessionState {
    override fun type(char: Char): TypeResult {
      val searchProcessor = SearchProcessor(jumpEditors, SearchQuery.Literal(char.toString()), defaultBoundary, invertUppercaseMode)
      
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
        if (query is SearchQuery.Literal) {
          val newBoundaries = when (searchProcessor.boundaries) {
            StandardBoundaries.VISIBLE_ON_SCREEN -> StandardBoundaries.AFTER_CARET
            StandardBoundaries.AFTER_CARET       -> StandardBoundaries.BEFORE_CARET
            StandardBoundaries.BEFORE_CARET      -> StandardBoundaries.VISIBLE_ON_SCREEN
            else                                 -> searchProcessor.boundaries
          }
          
          val newSearchProcessor = SearchProcessor(jumpEditors, query, newBoundaries, searchProcessor.invertUppercaseMode)
          return TypeResult.ChangeState(SelectTag(actions, jumpEditors, newSearchProcessor))
        }
      }
      else if (char == '\n') {
        val newSearchProcessor = SearchProcessor(jumpEditors, searchProcessor.query, searchProcessor.boundaries, !searchProcessor.invertUppercaseMode)
        return TypeResult.ChangeState(SelectTag(actions, jumpEditors, newSearchProcessor))
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
