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
  
  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var jumpModeColor: Color = Color(0xFFFFFF),
  
  @OptionTag("tagForegroundRGB", converter = ColorConverter::class)
  var tagForegroundColor: Color = Color(0xFFFFFF),
  
  @OptionTag("searchHighlightRGB", converter = ColorConverter::class)
  var searchHighlightColor: Color = Color(0x008299),
)
