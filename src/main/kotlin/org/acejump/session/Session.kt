package org.acejump.session

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import com.intellij.ui.LightweightHint
import org.acejump.ExternalUsage
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.KeyLayoutCache
import org.acejump.interact.TypeResult
import org.acejump.interact.mode.DefaultMode
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val editor: Editor) {
  private val editorSettings = EditorSettings.setup(editor)
  private lateinit var mode: SessionMode
  
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
          setMode(DefaultMode)
        }
      }
    }
  
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
          processor = SearchProcessor.fromChar(editor, charTyped, StandardBoundaries.VISIBLE_ON_SCREEN).also { searchProcessor = it }
          updateSearch(processor)
          return
        }
        
        editorSettings.startEditing(editor)
        val result = mode.type(editor, processor, tagger, charTyped, acceptedTag)
        editorSettings.stopEditing(editor)
        
        when (result) {
          TypeResult.UpdateResults   -> updateSearch(processor)
          TypeResult.EndSession      -> end()
          is TypeResult.LoadSnapshot -> loadSnapshot(result.snapshot)
          is TypeResult.ChangeMode   -> setMode(result.mode)
          else                       -> return
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
      }
    }
  }
  
  /**
   * Loads state from a saved snapshot.
   */
  private fun loadSnapshot(snapshot: SessionSnapshot) {
    acceptedTag = null
    tagger = snapshot.savedTagger
    searchProcessor = snapshot.savedProcessor?.also(::updateSearch)
  }
  
  private fun setMode(mode: SessionMode) {
    this.mode = mode
    editor.colorsScheme.setColor(EditorColors.CARET_COLOR, mode.caretColor)
    
    val acceptedTag = acceptedTag
    if (acceptedTag != null) {
      val hintText = mode.actionHint
        .joinToString("\n")
        .replace("<f>", "<span style=\"font-family:'${editor.colorsScheme.editorFontName}';font-weight:bold\">")
        .replace("</f>", "</span>")
  
      val hint = LightweightHint(HintUtil.createInformationLabel(hintText))
      val pos = HintManagerImpl.getHintPosition(hint, editor, editor.offsetToLogicalPosition(acceptedTag), HintManager.ABOVE)
      val info = HintManagerImpl.createHintHint(editor, pos, hint, HintManager.ABOVE).setShowImmediately(true)
      val flags = HintManager.UPDATE_BY_SCROLLING or HintManager.HIDE_BY_ESCAPE
      HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, pos, flags, 0, true, info)
    }
  }
  
  /**
   * Sets AceJump mode or ends the session if the mode is the same.
   */
  fun toggleMode(mode: SessionMode) {
    if (!this::mode.isInitialized) {
      setMode(mode)
    }
    else if (!this.mode.isSame(mode)) {
      this.mode = mode
      restart()
    }
    else {
      end()
    }
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    toggleMode(DefaultMode)
    tagger = Tagger(editor)
    tagCanvas.setMarkers(emptyList(), isRegex = true)
    updateSearch(SearchProcessor.fromRegex(editor, pattern, boundaries).also { searchProcessor = it })
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern, boundaries: Boundaries) {
    startRegexSearch(pattern.regex, boundaries)
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
    
    HintManagerImpl.getInstanceImpl().hideAllHints()
    editorSettings.onTagUnaccepted(editor)
    editor.colorsScheme.setColor(EditorColors.CARET_COLOR, mode.caretColor)
    editor.contentComponent.repaint()
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
      editor.colorsScheme.setColor(EditorColors.CARET_COLOR, AbstractColorsScheme.INHERITED_COLOR_MARKER)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
