package org.acejump.config

import com.intellij.util.xmlb.annotations.OptionTag
import org.acejump.input.KeyLayout
import org.acejump.input.KeyLayout.QWERTY
import java.awt.Color

data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.allChars,
  var prefixChars: String = ";",
  var minQueryLength: Int = 1,
  var editorFadeOpacity: Int = 70,
  
  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var jumpModeColor: Color = Color(0xFFFFFF),
  
  @OptionTag("tagForegroundRGB", converter = ColorConverter::class)
  var tagForegroundColor1: Color = Color(0xFFFFFF),
  
  @OptionTag("tagForeground2RGB", converter = ColorConverter::class)
  var tagForegroundColor2: Color = Color(0xFFFFFF),
  
  @OptionTag("searchHighlightRGB", converter = ColorConverter::class)
  var searchHighlightColor: Color = Color(0x008299),
)
