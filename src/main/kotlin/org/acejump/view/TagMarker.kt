package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.SystemInfo
import org.acejump.boundaries.EditorOffsetCache
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

/**
 * Describes a 1 or 2 character shortcut that points to a specific character in the editor.
 */
internal class TagMarker(
  private val firstChar: String,
  private val secondChar: String,
  val offset: Int
) {
  private constructor(tag: String, offset: Int) : this(tag.first().toString(), tag.drop(1), offset)
  
  private val length = firstChar.length + secondChar.length
  
  companion object {
    /**
     * TODO This might be due to DPI settings.
     */
    private val HIGHLIGHT_OFFSET = if (SystemInfo.isMac) -0.5 else 0.0
    
    /**
     * Creates a new tag, precomputing some information about the nearby characters to reduce rendering overhead. If the last typed
     * character ([typedTag]) matches the first [tag] character, only the second [tag] character is displayed.
     */
    fun create(tag: String, offset: Int, typedTag: String): TagMarker {
      return TagMarker(tag.drop(typedTag.length), offset)
    }
  }
  
  /**
   * Paints the tag, taking into consideration visual space around characters in the editor, as well as all other previously painted tags.
   * Returns a rectangle indicating the area where the tag was rendered, or null if the tag could not be rendered due to overlap.
   */
  fun paint(g: Graphics2D, editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: MutableList<Rectangle>): Rectangle? {
    val rect = alignTag(editor, cache, font, occupied) ?: return null
    
    drawHighlight(g, rect, editor.colorsScheme.defaultBackground)
    drawForeground(g, font, rect.location)
    
    occupied.add(rect)
    return rect
  }
  
  /**
   * Renders the tag background.
   */
  private fun drawHighlight(g: Graphics2D, rect: Rectangle, color: Color) {
    g.color = color
    g.translate(0.0, HIGHLIGHT_OFFSET)
    g.fillRect(rect.x, rect.y, rect.width, rect.height + 1)
    g.translate(0.0, -HIGHLIGHT_OFFSET)
  }
  
  /**
   * Renders the tag text.
   */
  private fun drawForeground(g: Graphics2D, font: TagFont, point: Point) {
    val x = point.x
    val y = point.y + font.baselineDistance
    
    g.font = font.tagFont
    g.color = font.foregroundColor1
    g.drawString(firstChar, x, y)
    
    if (secondChar.isNotEmpty()) {
      g.color = font.foregroundColor2
      g.drawString(secondChar, x + font.tagCharWidth, y)
    }
  }
  
  private fun alignTag(editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: List<Rectangle>): Rectangle? {
    val pos = cache.offsetToXY(editor, offset)
    val rect = Rectangle(pos.x, pos.y, font.tagCharWidth * length, font.lineHeight)
    return rect.takeIf { occupied.none(it::intersects) }
  }
}
