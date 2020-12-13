package org.acejump.config

import com.intellij.util.xmlb.annotations.OptionTag
import org.acejump.input.KeyLayout
import org.acejump.input.KeyLayout.QWERTY
import java.awt.Color

data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.allChars,
  
  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var singleCaretModeColor: Color = Color.BLUE,
  
  @OptionTag("multiCaretModeRGB", converter = ColorConverter::class)
  var multiCaretModeColor: Color = Color.ORANGE,
  
  @OptionTag("textHighlightRGB", converter = ColorConverter::class)
  var textHighlightColor: Color = Color.GREEN,
  
  @OptionTag("tagForegroundRGB", converter = ColorConverter::class)
  var tagForegroundColor: Color = Color.BLACK,
  
  @OptionTag("tagBackgroundRGB", converter = ColorConverter::class)
  var tagBackgroundColor: Color = Color.YELLOW,
  
  @OptionTag("acceptedTagRGB", converter = ColorConverter::class)
  var acceptedTagColor: Color = Color.CYAN,
  
  var roundedTagCorners: Boolean = true,
  var searchWholeFile: Boolean = true
)
