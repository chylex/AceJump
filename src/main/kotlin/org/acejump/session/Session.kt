package org.acejump.session

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.ui.LightweightHint
import org.acejump.ExternalUsage
import org.acejump.action.TagVisitor
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.KeyLayoutCache
import org.acejump.input.TagActionKeys
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val editor: Editor) {
  private companion object {
    private val defaultBoundaries
      get() = if (AceConfig.searchWholeFile) StandardBoundaries.WHOLE_FILE else StandardBoundaries.VISIBLE_ON_SCREEN
  }
  
  private val editorSettings = EditorSettings.setup(editor)
  private var activated = false
  
  private var searchProcessor: SearchProcessor? = null
  private var tagger = Tagger(editor)
  private var acceptedTag: Int? = null
    set(value) {
      field = value
      
      if (value != null) {
        tagCanvas.removeMarkers()
        editorSettings.onTagAccepted(editor)
        
        val processor = searchProcessor
        
        if (processor != null) {
          textHighlighter.renderFinal(value, processor.query)
          
          val hintText = TagActionKeys.hint
            .joinToString("\n")
            .replace("<f>", "<span style=\"font-family:'${editor.colorsScheme.editorFontName}';font-weight:bold\">")
            .replace("</f>", "</span>")
          
          val hint = LightweightHint(HintUtil.createInformationLabel(hintText))
          val pos = HintManagerImpl.getHintPosition(hint, editor, editor.offsetToLogicalPosition(value), HintManager.ABOVE)
          val info = HintManagerImpl.createHintHint(editor, pos, hint, HintManager.ABOVE).setShowImmediately(true)
          val flags = HintManager.UPDATE_BY_SCROLLING or HintManager.HIDE_BY_ESCAPE
          HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, pos, flags, 0, true, info)
        }
      }
    }
  
  private val tagVisitor
    get() = searchProcessor?.let { TagVisitor(editor, it) }
  
  private val textHighlighter = TextHighlighter(editor)
  private val tagCanvas = TagCanvas(editor)
  
  @ExternalUsage
  val tags
    get() = tagger.tags
  
  init {
    KeyLayoutCache.ensureInitialized(AceConfig.settings)
    
    EditorKeyListener.attach(editor, object : TypedActionHandler {
      override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
        var processor = searchProcessor
        
        if (processor == null) {
          processor = SearchProcessor.fromChar(editor, charTyped, defaultBoundaries).also { searchProcessor = it }
          updateSearch(processor)
        }
        else {
          val tag = acceptedTag
          
          if (tag != null) {
            val action = TagActionKeys[charTyped] ?: return
            action(editor, processor, tag, charTyped.isUpperCase())
            end()
          }
          else if (processor.type(charTyped, tagger)) {
            updateSearch(processor)
          }
        }
      }
    })
  }
  
  /**
   * Updates text highlights and tag markers according to the current search state. Dispatches jumps if the search query matches a tag.
   * If all tags are outside view, scrolls to the closest one.
   */
  private fun updateSearch(processor: SearchProcessor) {
    val query = processor.query
    val results = processor.results
    
    when (val result = tagger.update(query, results.clone())) {
      is TaggingResult.Accept -> {
        acceptedTag = result.offset
      }
      
      is TaggingResult.Mark   -> {
        val tags = result.tags
        tagCanvas.setMarkers(tags, isRegex = query is SearchQuery.RegularExpression)
        textHighlighter.renderOccurrences(results, query)
        
        val cache = EditorOffsetCache.new()
        val boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
        
        if (tags.none { boundaries.isOffsetInside(editor, it.offsetL, cache) || boundaries.isOffsetInside(editor, it.offsetR, cache) }) {
          tagVisitor?.scrollToClosest()
        }
      }
    }
  }
  
  /**
   * Starts or ends AceJump mode.
   */
  fun startOrEnd() {
    if (activated) {
      end()
    }
    else {
      activated = true
    }
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    activated = true
    tagger = Tagger(editor)
    tagCanvas.setMarkers(emptyList(), isRegex = true)
    updateSearch(SearchProcessor.fromRegex(editor, pattern, boundaries.intersection(defaultBoundaries)).also { searchProcessor = it })
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern, boundaries: Boundaries) {
    startRegexSearch(pattern.regex, boundaries)
  }
  
  /**
   * See [TagVisitor.visitPrevious]. If there are no tags, nothing happens.
   */
  fun visitPreviousTag() {
    tagVisitor?.visitPrevious()?.let { acceptedTag = it }
  }
  
  /**
   * See [TagVisitor.visitNext]. If there are no tags, nothing happens.
   */
  fun visitNextTag() {
    tagVisitor?.visitNext()?.let { acceptedTag = it }
  }
  
  /**
   * Ends this session.
   */
  fun end() {
    SessionManager.end(editor)
  }
  
  /**
   * Clears any currently active search, tags, and highlights.
   */
  fun restart() {
    tagger = Tagger(editor)
    acceptedTag = null
    searchProcessor = null
    tagCanvas.removeMarkers()
    textHighlighter.reset()
    editorSettings.onTagUnaccepted(editor)
    HintManagerImpl.getInstanceImpl().hideAllHints()
  }
  
  /**
   * Should only be used from [SessionManager] to dispose a successfully ended session.
   */
  internal fun dispose() {
    tagger = Tagger(editor)
    tagCanvas.unbind()
    textHighlighter.reset()
    EditorKeyListener.detach(editor)
    
    if (!editor.isDisposed) {
      HintManagerImpl.getInstanceImpl().hideAllHints()
      editorSettings.restore(editor)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
