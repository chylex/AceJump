package org.acejump.session

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import org.acejump.config.AceConfig

/**
 * Holds [Editor] caret settings. The settings are saved the moment a [Session] starts, modified to indicate AceJump states, and restored
 * once the [Session] ends.
 */
internal data class EditorSettings(private val isBlockCursor: Boolean, private val isBlinkCaret: Boolean, private val isReadOnly: Boolean) {
  companion object {
    fun setup(editor: Editor): EditorSettings {
      val settings = editor.settings
      val document = editor.document
      
      val original = EditorSettings(
        isBlockCursor = settings.isBlockCursor,
        isBlinkCaret = settings.isBlinkCaret,
        isReadOnly = !document.isWritable
      )
      
      settings.isBlockCursor = true
      settings.isBlinkCaret = false
      document.setReadOnly(true)
      editor.colorsScheme.setColor(EditorColors.CARET_COLOR, AceConfig.jumpModeColor)
      
      return original
    }
  }
  
  fun onTagAccepted(editor: Editor) = editor.let {
    it.settings.isBlockCursor = isBlockCursor
    it.colorsScheme.setColor(EditorColors.CARET_COLOR, AbstractColorsScheme.INHERITED_COLOR_MARKER)
  }
  
  fun onTagUnaccepted(editor: Editor) = editor.let {
    it.settings.isBlockCursor = true
    it.colorsScheme.setColor(EditorColors.CARET_COLOR, AceConfig.jumpModeColor)
  }
  
  fun restore(editor: Editor) {
    val settings = editor.settings
    val document = editor.document
    
    settings.isBlockCursor = isBlockCursor
    settings.isBlinkCaret = isBlinkCaret
    document.setReadOnly(isReadOnly)
    editor.colorsScheme.setColor(EditorColors.CARET_COLOR, AbstractColorsScheme.INHERITED_COLOR_MARKER)
  }
}
