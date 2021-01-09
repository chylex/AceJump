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
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.KeyLayoutCache
import org.acejump.modes.BetweenPointsMode
import org.acejump.modes.FromCaretMode
import org.acejump.modes.JumpMode
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val editor: Editor) {
  private val editorSettings = EditorSettings.setup(editor)
  private lateinit var mode: SessionMode
  
  private var state: SessionState? = null
  private var tagger = Tagger(editor)
  
  private var acceptedTag: Int? = null
    set(value) {
      field = value
      
      if (value != null) {
        tagCanvas.removeMarkers()
        editorSettings.onTagAccepted(editor)
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
        val state = state ?: return
        
        editorSettings.startEditing(editor)
        val result = mode.type(state, charTyped, acceptedTag)
        editorSettings.stopEditing(editor)
        
        when (result) {
          TypeResult.Nothing          -> updateHint()
          TypeResult.RestartSearch    -> restart().also { this@Session.state = SessionState(editor, tagger); updateHint() }
          is TypeResult.UpdateResults -> updateSearch(result.processor)
          is TypeResult.ChangeMode    -> setMode(result.mode)
          TypeResult.EndSession       -> end()
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
        val offset = result.offset
        acceptedTag = offset
        textHighlighter.renderFinal(offset, processor.query)
      }
      
      is TaggingResult.Mark -> {
        val tags = result.tags
        tagCanvas.setMarkers(tags, isRegex = query is SearchQuery.RegularExpression)
        textHighlighter.renderOccurrences(results, query)
      }
    }
    
    updateHint()
  }
  
  private fun setMode(mode: SessionMode) {
    this.mode = mode
    editor.colorsScheme.setColor(EditorColors.CARET_COLOR, mode.caretColor)
    updateHint()
  }
  
  private fun updateHint() {
    val hintArray = mode.getHint(acceptedTag) ?: return
    val hintText = hintArray
      .joinToString("\n")
      .replace("<f>", "<span style=\"font-family:'${editor.colorsScheme.editorFontName}';font-weight:bold\">")
      .replace("</f>", "</span>")
      .replace("<h>", "<b><u>")
      .replace("</h>", "</u></b>")
    
    val hint = LightweightHint(HintUtil.createInformationLabel(hintText))
    val pos = acceptedTag?.let(editor::offsetToLogicalPosition) ?: editor.caretModel.logicalPosition
    val point = HintManagerImpl.getHintPosition(hint, editor, pos, HintManager.ABOVE)
    val info = HintManagerImpl.createHintHint(editor, point, hint, HintManager.ABOVE).setShowImmediately(true)
    val flags = HintManager.UPDATE_BY_SCROLLING or HintManager.HIDE_BY_ESCAPE
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, point, flags, 0, true, info)
  }
  
  fun cycleMode() {
    if (!this::mode.isInitialized) {
      setMode(JumpMode())
      state = SessionState(editor, tagger)
      return
    }
    
    restart()
    setMode(when (mode) {
      is JumpMode      -> FromCaretMode()
      is FromCaretMode -> BetweenPointsMode()
      else             -> JumpMode()
    })
    
    state = SessionState(editor, tagger)
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    if (this::mode.isInitialized) {
      end()
      return
    }
    
    setMode(JumpMode())
    tagger = Tagger(editor)
    tagCanvas.setMarkers(emptyList(), isRegex = true)
    updateSearch(SearchProcessor.fromRegex(editor, pattern, boundaries).also { state = SessionState(editor, tagger, it) })
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
    state = null
    tagger = Tagger(editor)
    acceptedTag = null
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
