package org.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Holds all active tag markers and renders them on top of the editor.
 */
internal class TagCanvas(private val editor: Editor) : JComponent(), CaretListener {
  private var markers: Collection<TagMarker>? = null
  private var caret = -1
  
  init {
    val contentComponent = editor.contentComponent
    contentComponent.add(this)
    setBounds(0, 0, contentComponent.width, contentComponent.height)
    
    if (ApplicationInfo.getInstance().build.components.first() < 173) {
      SwingUtilities.convertPoint(this, location, editor.component.rootPane).let { setLocation(-it.x, -it.y) }
    }
    
    editor.caretModel.addCaretListener(this)
  }
  
  fun unbind() {
    markers = null
    editor.contentComponent.remove(this)
    editor.caretModel.removeCaretListener(this)
  }
  
  /**
   * Ensures that all tags and the outline around the selected tag are repainted. It should not be necessary to repaint the entire tag
   * canvas, but the cost of repainting visible tags is negligible.
   */
  override fun caretPositionChanged(event: CaretEvent) {
    caret = editor.caretModel.offset
    repaint()
  }
  
  fun setMarkers(markers: Collection<TagMarker>) {
    this.markers = markers
    repaint()
  }
  
  fun removeMarkers() {
    this.markers = emptyList()
  }
  
  override fun paint(g: Graphics) {
    if (!markers.isNullOrEmpty()) {
      super.paint(g)
    }
  }
  
  override fun paintChildren(g: Graphics) {
    super.paintChildren(g)
    
    val markers = markers ?: return
    
    (g as Graphics2D).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    
    val font = TagFont(editor)
    
    val cache = EditorOffsetCache.new()
    val viewRange = StandardBoundaries.VISIBLE_ON_SCREEN.getOffsetRange(editor, cache)
    val occupied = mutableListOf<Rectangle>()
    
    for (marker in markers) {
      if (marker.offset in viewRange) {
        marker.paint(g, editor, cache, font, occupied)
      }
    }
  }
}
