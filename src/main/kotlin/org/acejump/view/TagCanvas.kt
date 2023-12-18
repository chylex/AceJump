package org.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Editor
import com.intellij.ui.ColorUtil
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Holds all active tag markers and renders them on top of the editor.
 */
internal class TagCanvas(private val editor: Editor) : JComponent() {
  private var markers: Collection<TagMarker> = emptyList()
  private var visible = false
  
  init {
    val contentComponent = editor.contentComponent
    contentComponent.add(this)
    setBounds(0, 0, contentComponent.width, contentComponent.height)
    
    if (ApplicationInfo.getInstance().build.components.first() < 173) {
      SwingUtilities.convertPoint(this, location, editor.component.rootPane).let { setLocation(-it.x, -it.y) }
    }
  }
  
  fun unbind() {
    editor.contentComponent.remove(this)
    editor.contentComponent.repaint()
  }
  
  fun setMarkers(markers: Collection<TagMarker>) {
    this.markers = markers
    this.visible = true
    repaint()
  }
  
  fun removeMarkers() {
    this.markers = emptyList()
    this.visible = false
    repaint()
  }
  
  override fun paintChildren(g: Graphics) {
    super.paintChildren(g)
    
    if (!visible) {
      return
    }
    
    g.color = ColorUtil.withAlpha(editor.colorsScheme.defaultBackground, (AceConfig.editorFadeOpacity * 0.01).coerceIn(0.0, 1.0))
    g.fillRect(0, 0, width - 1, height - 1)
    
    if (markers.isEmpty()) {
      return
    }
    
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
