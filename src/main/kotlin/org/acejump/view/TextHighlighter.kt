package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.search.SearchQuery
import java.awt.Color
import java.awt.Graphics

/**
 * Renders highlights for search occurrences.
 */
internal class TextHighlighter(private val editor: Editor) {
  private var previousHighlights: Array<RangeHighlighter>? = null
  
  /**
   * Removes all current highlights and re-creates them from scratch. Must be called whenever any of the method parameters change.
   */
  fun renderOccurrences(offsets: IntList, query: SearchQuery) {
    render(offsets, when (query) {
      is SearchQuery.RegularExpression -> RegexRenderer
      else                             -> SearchedWordRenderer
    }, query::getHighlightLength)
  }
  
  /**
   * Removes all current highlights and re-adds a single highlight at the position of the accepted tag with a different color.
   */
  fun renderFinal(offset: Int, query: SearchQuery) {
    render(IntArrayList(intArrayOf(offset)), AcceptedTagRenderer, query::getHighlightLength)
  }
  
  private inline fun render(offsets: IntList, renderer: CustomHighlighterRenderer, getHighlightLength: (CharSequence, Int) -> Int) {
    val markup = editor.markupModel
    val chars = editor.immutableText
    
    ARC = TagFont(editor).tagCornerArc
    
    val modifications = (previousHighlights?.size ?: 0) + offsets.size
    val enableBulkEditing = modifications > 1000
    
    val document = editor.document
    
    try {
      if (enableBulkEditing) {
        document.isInBulkUpdate = true
      }
      
      previousHighlights?.forEach(markup::removeHighlighter)
      previousHighlights = Array(offsets.size) { index ->
        val start = offsets.getInt(index)
        val end = start + getHighlightLength(chars, start)
        
        markup.addRangeHighlighter(start, end, LAYER, null, HighlighterTargetArea.EXACT_RANGE).apply {
          customRenderer = renderer
        }
      }
    } finally {
      if (enableBulkEditing) {
        document.isInBulkUpdate = false
      }
    }
  }
  
  fun reset() {
    editor.markupModel.removeAllHighlighters()
    previousHighlights = null
  }
  
  /**
   * Renders a filled highlight in the background of a searched text occurrence.
   */
  private object SearchedWordRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawFilled(g, editor, highlighter.startOffset, highlighter.endOffset, AceConfig.textHighlightColor)
    }
  }
  
  /**
   * Renders a filled highlight in the background of the first highlighted position. Used for regex search queries.
   */
  private object RegexRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawSingle(g, editor, highlighter.startOffset, AceConfig.textHighlightColor)
    }
  }
  
  /**
   * Renders a filled highlight in the background of the accepted tag position and search query.
   */
  private object AcceptedTagRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawFilled(g, editor, highlighter.startOffset, highlighter.endOffset, AceConfig.acceptedTagColor)
    }
  }
  
  private companion object {
    private const val LAYER = HighlighterLayer.LAST + 1
    private var ARC = 0
  
    private fun drawFilled(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int, color: Color) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)
    
      g.color = color
      g.fillRoundRect(start.x, start.y + 1, end.x - start.x, editor.lineHeight - 1, ARC, ARC)
    }
    
    private fun drawSingle(g: Graphics, editor: Editor, offset: Int, color: Color) {
      val pos = EditorOffsetCache.Uncached.offsetToXY(editor, offset)
      val char = editor.immutableText.getOrNull(offset)?.takeUnless { it == '\n' || it == '\t' } ?: ' '
      val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
      val lastCharWidth = editor.component.getFontMetrics(font).charWidth(char)
      
      g.color = color
      g.fillRoundRect(pos.x, pos.y + 1, lastCharWidth, editor.lineHeight - 1, ARC, ARC)
    }
  }
}
