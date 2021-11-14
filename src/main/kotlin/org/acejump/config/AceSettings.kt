package org.acejump.config

import com.intellij.util.xmlb.annotations.OptionTag
import org.acejump.input.KeyLayout
import org.acejump.input.KeyLayout.QWERTY
import java.awt.Color

data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.allChars,
  var minQueryLength: Int = 1,
  
  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var jumpModeColor: Color = Color(0xFFFFFF),
  
  @OptionTag("textHighlightRGB", converter = ColorConverter::class)
  var textHighlightColor: Color = Color(0x394B58),
  
  @OptionTag("tagForegroundRGB", converter = ColorConverter::class)
  var tagForegroundColor: Color = Color(0xFFFFFF),
  
  @OptionTag("tagBackgroundRGB", converter = ColorConverter::class)
  var tagBackgroundColor: Color = Color(0x008299),
  
  @OptionTag("acceptedTagRGB", converter = ColorConverter::class)
  var acceptedTagColor: Color = Color(0x394B58)
)
