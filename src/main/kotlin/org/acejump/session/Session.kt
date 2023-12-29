package org.acejump.session

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.KeyLayoutCache
import org.acejump.modes.JumpMode
import org.acejump.modes.SessionMode
import org.acejump.search.Pattern
import org.acejump.search.SearchProcessor
import org.acejump.search.SearchQuery
import org.acejump.search.Tag
import org.acejump.session.TypeResult.AcceptTag
import org.acejump.session.TypeResult.ChangeMode
import org.acejump.session.TypeResult.ChangeState
import org.acejump.session.TypeResult.EndSession
import org.acejump.session.TypeResult.Nothing
import org.acejump.view.TagCanvas
import org.acejump.view.TagMarker
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val mainEditor: Editor, private val jumpEditors: List<Editor>) {
  private val editorSettings = EditorSettings.setup(mainEditor)
  private lateinit var mode: SessionMode
  
  private var state: SessionState? = null
  
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
  
  var defaultBoundary: Boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
  
  private val actions = object : SessionActions {
    override fun showHighlights(results: Map<Editor, IntList>, query: SearchQuery) {
      textHighlighter.renderOccurrences(results, query)
    }
    
    override fun hideHighlights() {
      textHighlighter.reset()
    }
    
    override fun setTagMarkers(markers: Map<Editor, Collection<TagMarker>>) {
      for ((editor, canvas) in tagCanvases) {
        canvas.setMarkers(markers[editor].orEmpty())
      }
    }
  }
  
  init {
    KeyLayoutCache.ensureInitialized(AceConfig.settings)
    EditorKeyListener.attach(mainEditor) { editor, charTyped, _ -> typeCharacter(editor, charTyped) }
  }
  
  private fun typeCharacter(editor: Editor, charTyped: Char) {
    val state = state ?: return
    
    editorSettings.startEditing(editor)
    val result = mode.type(state, charTyped, acceptedTag)
    editorSettings.stopEditing(editor)
    
    when (result) {
      Nothing        -> return
      is ChangeState -> this.state = result.state
      is ChangeMode  -> setMode(result.mode)
      
      is AcceptTag   -> {
        acceptedTag = result.tag
        mode.accept(state, result.tag)
        end()
      }
      
      EndSession     -> end()
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
    state = SessionState.WaitForKey(actions, jumpEditors, defaultBoundary)
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern) {
    if (!this::mode.isInitialized) {
      setMode(JumpMode())
    }
    
    for (canvas in tagCanvases.values) {
      canvas.setMarkers(emptyList())
    }
    
    val processor = SearchProcessor(jumpEditors, SearchQuery.RegularExpression(pattern.regex), defaultBoundary)
    textHighlighter.renderOccurrences(processor.resultsCopy, processor.query)
    
    state = SessionState.SelectTag(actions, jumpEditors, processor)
  }
  
  fun tagImmediately() {
    typeCharacter(mainEditor, '\n')
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
