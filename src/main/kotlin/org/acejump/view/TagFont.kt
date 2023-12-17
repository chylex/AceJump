package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import org.acejump.config.AceConfig
import java.awt.Font

/**
 * Stores font metrics for aligning and rendering [TagMarker]s.
 */
internal class TagFont(editor: Editor) {
  val tagFont: Font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
  val tagCharWidth = editor.component.getFontMetrics(tagFont).charWidth('W')
  
  val foregroundColor1 = AceConfig.tagForegroundColor1
  val foregroundColor2 = AceConfig.tagForegroundColor2
  
  val lineHeight = editor.lineHeight
  val baselineDistance = editor.ascent
}
