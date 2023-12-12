package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.scale.JBUIScale
import org.acejump.boundaries.EditorOffsetCache
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

/**
 * Describes a 1 or 2 character shortcut that points to a specific character in the editor.
 */
internal class TagMarker(
  private val tag: String,
  val offset: Int
) {
  private val length = tag.length
  
  companion object {
    private const val ARC = 1
    
    /**
     * TODO This might be due to DPI settings.
     */
    private val HIGHLIGHT_OFFSET = if (SystemInfo.isMac) -0.5 else 0.0
    
    private val SHADOW_COLOR = Color(0F, 0F, 0F, 0.35F)
    
    /**
     * Creates a new tag, precomputing some information about the nearby characters to reduce rendering overhead. If the last typed
     * character ([typedTag]) matches the first [tag] character, only the second [tag] character is displayed.
     */
    fun create(tag: String, offset: Int, typedTag: String): TagMarker {
      val displayedTag = if (typedTag.isNotEmpty() && typedTag.last().equals(tag.first(), ignoreCase = true))
        tag.drop(1).toUpperCase()
      else
        tag.toUpperCase()
      
      return TagMarker(displayedTag, offset)
    }
    
    /**
     * Renders the tag background.
     */
    private fun drawHighlight(g: Graphics2D, rect: Rectangle, color: Color) {
      g.color = color
      g.translate(0.0, HIGHLIGHT_OFFSET)
      g.fillRoundRect(rect.x, rect.y, rect.width, rect.height + 1, ARC, ARC)
      g.translate(0.0, -HIGHLIGHT_OFFSET)
    }
    
    /**
     * Renders the tag text.
     */
    private fun drawForeground(g: Graphics2D, font: TagFont, point: Point, text: String) {
      val x = point.x + 2
      val y = point.y + font.baselineDistance
      
      g.font = font.tagFont
      
      if (!font.isForegroundDark) {
        g.color = SHADOW_COLOR
        g.drawString(text, x + 1, y + 1)
      }
      
      g.color = font.foregroundColor
      g.drawString(text, x, y)
    }
  }
  
  /**
   * Paints the tag, taking into consideration visual space around characters in the editor, as well as all other previously painted tags.
   * Returns a rectangle indicating the area where the tag was rendered, or null if the tag could not be rendered due to overlap.
   */
  fun paint(g: Graphics2D, editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: MutableList<Rectangle>): Rectangle? {
    val rect = alignTag(editor, cache, font, occupied) ?: return null
    
    drawHighlight(g, rect, font.backgroundColor)
    drawForeground(g, font, rect.location, tag)
    
    occupied.add(JBUIScale.scale(2).let { Rectangle(rect.x - it, rect.y, rect.width + (2 * it), rect.height) })
    return rect
  }
  
  private fun alignTag(editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: List<Rectangle>): Rectangle? {
    val pos = cache.offsetToXY(editor, offset)
    val rect = Rectangle(pos.x - 2, pos.y, (font.tagCharWidth * length) + 4, font.lineHeight)
    return rect.takeIf { occupied.none(it::intersects) }
  }
}
