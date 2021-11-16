package org.acejump.session

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import org.acejump.ExternalUsage
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.clone
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.KeyLayoutCache
import org.acejump.modes.JumpMode
import org.acejump.modes.SessionMode
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val mainEditor: Editor, private val jumpEditors: List<Editor>) {
  private val editorSettings = EditorSettings.setup(mainEditor)
  private lateinit var mode: SessionMode
  
  private var state: SessionStateImpl? = null
  private var tagger = Tagger(jumpEditors)
  
  private var acceptedTag: Tag? = null
    set(value) {
      field = value
      
      if (value != null) {
        tagCanvases.values.forEach(TagCanvas::removeMarkers)
        editorSettings.onTagAccepted(mainEditor)
      }
    }
  
  private val textHighlighter = TextHighlighter()
  private val tagCanvases = jumpEditors.associateWith(::TagCanvas)
  
  @ExternalUsage
  val tags
    get() = tagger.tags
  
  var defaultBoundary: Boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
  
  init {
    KeyLayoutCache.ensureInitialized(AceConfig.settings)
    
    EditorKeyListener.attach(mainEditor, object : TypedActionHandler {
      override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
        val state = state ?: return
        val hadTags = tagger.hasTags
        
        editorSettings.startEditing(editor)
        val result = mode.type(state, charTyped, acceptedTag)
        editorSettings.stopEditing(editor)
        
        when (result) {
          TypeResult.Nothing          -> return;
          is TypeResult.UpdateResults -> updateSearch(result.processor, markImmediately = hadTags)
          is TypeResult.ChangeMode    -> setMode(result.mode)
          
          TypeResult.RestartSearch    -> restart().also {
            this@Session.state = SessionStateImpl(jumpEditors, tagger, defaultBoundary)
          }
          
          TypeResult.EndSession       -> end()
        }
      }
    })
  }
  
  /**
   * Updates text highlights and tag markers according to the current search state. Dispatches jumps if the search query matches a tag.
   */
  private fun updateSearch(processor: SearchProcessor, markImmediately: Boolean) {
    val query = processor.query
    val results = processor.results
    
    if (!markImmediately && query.rawText.let { it.length < AceConfig.minQueryLength && it.all(Char::isLetterOrDigit) }) {
      textHighlighter.renderOccurrences(results, query)
      return
    }
    
    when (val result = tagger.update(query, results.clone())) {
      is TaggingResult.Accept -> {
        acceptedTag = result.tag
        textHighlighter.renderFinal(result.tag, processor.query)
        
        if (state?.let { mode.accept(it, result.tag) } == true) {
          end()
          return
        }
      }
      
      is TaggingResult.Mark   -> {
        for ((editor, canvas) in tagCanvases) {
          canvas.setMarkers(result.markers[editor].orEmpty())
        }
        
        textHighlighter.renderOccurrences(results, query)
      }
    }
  }
  
  private fun setMode(mode: SessionMode) {
    this.mode = mode
    mainEditor.colorsScheme.setColor(EditorColors.CARET_COLOR, mode.caretColor)
  }
  
  fun startJumpMode() {
    startJumpMode(::JumpMode)
  }
  
  fun startJumpMode(mode: () -> JumpMode) {
    if (this::mode.isInitialized && mode is JumpMode) {
      end()
      return
    }
    
    if (this::mode.isInitialized) {
      restart()
    }
    
    setMode(mode())
    state = SessionStateImpl(jumpEditors, tagger, defaultBoundary)
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern) {
    if (!this::mode.isInitialized) {
      setMode(JumpMode())
    }
    
    tagger = Tagger(jumpEditors)
    tagCanvases.values.forEach { it.setMarkers(emptyList()) }
    
    val processor = SearchProcessor.fromRegex(jumpEditors, pattern.regex, defaultBoundary)
    state = SessionStateImpl(jumpEditors, tagger, defaultBoundary, processor)
    
    updateSearch(processor, markImmediately = true)
  }
  
  fun tagImmediately() {
    val state = state ?: return
    val processor = state.currentProcessor
    
    if (processor != null) {
      updateSearch(processor, markImmediately = true)
    }
  }
  
  /**
   * Ends this session.
   */
  fun end() {
    SessionManager.end(mainEditor)
  }
  
  /**
   * Clears any currently active search, tags, and highlights.
   */
  fun restart() {
    state = null
    tagger = Tagger(jumpEditors)
    acceptedTag = null
    tagCanvases.values.forEach(TagCanvas::removeMarkers)
    textHighlighter.reset()
    
    HintManagerImpl.getInstanceImpl().hideAllHints()
    editorSettings.onTagUnaccepted(mainEditor)
    mainEditor.colorsScheme.setColor(EditorColors.CARET_COLOR, mode.caretColor)
    jumpEditors.forEach { it.contentComponent.repaint() }
  }
  
  /**
   * Should only be used from [SessionManager] to dispose a successfully ended session.
   */
  internal fun dispose() {
    tagger = Tagger(jumpEditors)
    tagCanvases.values.forEach(TagCanvas::unbind)
    textHighlighter.reset()
    EditorKeyListener.detach(mainEditor)
    
    if (!mainEditor.isDisposed) {
      HintManagerImpl.getInstanceImpl().hideAllHints()
      editorSettings.restore(mainEditor)
      mainEditor.colorsScheme.setColor(EditorColors.CARET_COLOR, AbstractColorsScheme.INHERITED_COLOR_MARKER)
      
      val focusedEditor = acceptedTag?.editor ?: mainEditor
      focusedEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
