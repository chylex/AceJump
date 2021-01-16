package org.acejump.session

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors
import org.acejump.ExternalUsage
import org.acejump.action.TagJumper
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.JumpMode
import org.acejump.input.JumpModeTracker
import org.acejump.input.KeyLayoutCache
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val editor: Editor) {
  private val originalSettings = EditorSettings.setup(editor)
  
  private val jumpModeTracker = JumpModeTracker()
  private var jumpMode = JumpMode.DISABLED
    set(value) {
      field = value
      
      if (value === JumpMode.DISABLED) {
        end()
      }
      else {
        searchProcessor?.let { textHighlighter.render(it.results, it.query, jumpMode) }
        editor.colorsScheme.setColor(EditorColors.CARET_COLOR, value.caretColor)
        editor.contentComponent.repaint()
      }
    }
  
  private var searchProcessor: SearchProcessor? = null
  private var tagger = Tagger(editor)
  
  private val tagJumper
    get() = TagJumper(editor, jumpMode, searchProcessor)
  
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
        val hadTags = tagger.hasTags
        
        if (processor == null) {
          processor = SearchProcessor.fromChar(editor, charTyped, StandardBoundaries.VISIBLE_ON_SCREEN).also { searchProcessor = it }
        }
        else if (!processor.type(charTyped, tagger)) {
          return
        }
        
        updateSearch(processor, markImmediately = hadTags, shiftMode = charTyped.isUpperCase())
      }
    })
  }
  
  /**
   * Updates text highlights and tag markers according to the current search state. Dispatches jumps if the search query matches a tag.
   */
  private fun updateSearch(processor: SearchProcessor, markImmediately: Boolean, shiftMode: Boolean = false) {
    val query = processor.query
    val results = processor.results
    
    textHighlighter.render(results, query, jumpMode)
    
    if (!markImmediately && query.rawText.let { it.length < AceConfig.minQueryLength && it.all(Char::isLetterOrDigit) }) {
      return
    }
    
    when (val result = tagger.markOrJump(query, results.clone())) {
      is TaggingResult.Jump -> {
        tagJumper.jump(result.offset, shiftMode)
        tagCanvas.removeMarkers()
        end()
      }
      
      is TaggingResult.Mark -> {
        val tags = result.tags
        tagCanvas.setMarkers(tags, isRegex = query is SearchQuery.RegularExpression)
      }
    }
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    tagger = Tagger(editor)
    tagCanvas.setMarkers(emptyList(), isRegex = true)
    
    val processor = SearchProcessor.fromRegex(editor, pattern, boundaries).also { searchProcessor = it }
    updateSearch(processor, markImmediately = true)
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern, boundaries: Boundaries) {
    startRegexSearch(pattern.regex, boundaries)
  }
  
  /**
   * See [JumpModeTracker.cycle].
   */
  fun cycleNextJumpMode() {
    jumpMode = jumpModeTracker.cycle(forward = true)
  }
  
  /**
   * See [JumpModeTracker.cycle].
   */
  fun cyclePreviousJumpMode() {
    jumpMode = jumpModeTracker.cycle(forward = false)
  }
  
  /**
   * See [JumpModeTracker.toggle]
   */
  fun toggleJumpMode(newMode: JumpMode) {
    jumpMode = jumpModeTracker.toggle(newMode)
  }
  
  fun tagImmediately() {
    searchProcessor?.let { updateSearch(it, markImmediately = true) }
  }
  
  /**
   * Ends this session.
   */
  fun end() {
    SessionManager.end(editor)
  }
  
  /**
   * Clears any currently active search, tags, and highlights. Does not reset [JumpMode].
   */
  fun restart() {
    tagger = Tagger(editor)
    searchProcessor = null
    tagCanvas.removeMarkers()
    textHighlighter.reset()
  }
  
  /**
   * Should only be used from [SessionManager] to dispose a successfully ended session.
   */
  internal fun dispose() {
    tagger = Tagger(editor)
    EditorKeyListener.detach(editor)
    tagCanvas.unbind()
    textHighlighter.reset()
    
    if (!editor.isDisposed) {
      originalSettings.restore(editor)
      editor.colorsScheme.setColor(EditorColors.CARET_COLOR, JumpMode.DISABLED.caretColor)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
