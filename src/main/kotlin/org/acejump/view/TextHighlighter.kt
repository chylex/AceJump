package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.ColorUtil
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
internal class TextHighlighter {
  private var previousHighlights = mutableMapOf<Editor, Array<RangeHighlighter>>()
  
  /**
   * Removes all current highlights and re-creates them from scratch. Must be called whenever any of the method parameters change.
   */
  fun renderOccurrences(results: Map<Editor, IntList>, query: SearchQuery) {
    render(results, when (query) {
      is SearchQuery.RegularExpression -> RegexRenderer
      else                             -> SearchedWordRenderer
    }, query::getHighlightLength)
  }
  
  private inline fun render(results: Map<Editor, IntList>, renderer: CustomHighlighterRenderer, getHighlightLength: (CharSequence, Int) -> Int) {
    for ((editor, offsets) in results) {
      val highlights = previousHighlights[editor]
      
      val markup = editor.markupModel
      val document = editor.document
      val chars = editor.immutableText
      
      val modifications = (highlights?.size ?: 0) + offsets.size
      val enableBulkEditing = modifications > 1000
      
      try {
        if (enableBulkEditing) {
          document.isInBulkUpdate = true
        }
        
        highlights?.forEach(markup::removeHighlighter)
        previousHighlights[editor] = Array(offsets.size) { index ->
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
    
    for (editor in previousHighlights.keys.toList()) {
      if (!results.containsKey(editor)) {
        previousHighlights.remove(editor)?.forEach(editor.markupModel::removeHighlighter)
      }
    }
  }
  
  fun reset() {
    previousHighlights.keys.forEach { it.markupModel.removeAllHighlighters() }
    previousHighlights.clear()
  }
  
  /**
   * Renders a filled highlight in the background of a searched text occurrence.
   */
  private object SearchedWordRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawFilled(g, editor, highlighter.startOffset, highlighter.endOffset, AceConfig.searchHighlightColor)
    }
  }
  
  /**
   * Renders a filled highlight in the background of the first highlighted position. Used for regex search queries.
   */
  private object RegexRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawSingle(g, editor, highlighter.startOffset, AceConfig.searchHighlightColor)
    }
  }
  
  private companion object {
    private const val LAYER = HighlighterLayer.LAST + 1
    
    private fun drawFilled(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int, color: Color) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)
      
      g.color = ColorUtil.withAlpha(AceConfig.searchHighlightColor, 0.2)
      g.fillRect(start.x, start.y + 1, end.x - start.x, editor.lineHeight - 1)
      
      g.color = color
      g.drawRect(start.x, start.y, end.x - start.x, editor.lineHeight)
    }
    
    private fun drawSingle(g: Graphics, editor: Editor, offset: Int, color: Color) {
      val pos = EditorOffsetCache.Uncached.offsetToXY(editor, offset)
      val char = editor.immutableText.getOrNull(offset)?.takeUnless { it == '\n' || it == '\t' } ?: ' '
      val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
      val lastCharWidth = editor.component.getFontMetrics(font).charWidth(char)
      
      g.color = ColorUtil.withAlpha(AceConfig.searchHighlightColor, 0.2)
      g.fillRect(pos.x, pos.y + 1, lastCharWidth, editor.lineHeight - 1)
      
      g.color = color
      g.drawRect(pos.x, pos.y, lastCharWidth, editor.lineHeight)
    }
  }
}
