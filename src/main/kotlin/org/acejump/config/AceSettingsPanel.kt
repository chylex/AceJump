package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBSlider
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.acejump.input.KeyLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.util.Hashtable
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.text.JTextComponent
import kotlin.reflect.KProperty

/**
 * Settings view located in File | Settings | Tools | AceJump.
 */
@Suppress("UsePropertyAccessSyntax")
internal class AceSettingsPanel {
  private val tagAllowedCharsField = JBTextField()
  private val keyboardLayoutCombo = ComboBox<KeyLayout>()
  private val keyboardLayoutArea = JBTextArea().apply { isEditable = false }
  private val minQueryLengthField = JBTextField()
  private val editorFadeOpacitySlider = JBSlider(0, 10).apply {
    labelTable = Hashtable((0..10).associateWith { JLabel("${it * 10}") })
    paintTrack = true
    paintLabels = true
    paintTicks = true
    minorTickSpacing = 1
    majorTickSpacing = 1
    minimumSize = Dimension(275, minimumSize.height)
  }
  private val jumpModeColorWheel = ColorPanel()
  private val tagForeground1ColorWheel = ColorPanel()
  private val tagForeground2ColorWheel = ColorPanel()
  private val searchHighlightColorWheel = ColorPanel()
  
  init {
    tagAllowedCharsField.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutArea.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutCombo.setupEnumItems { keyChars = it.rows.joinToString("\n") }
  }
  
  internal val rootPanel: JPanel = panel {
    group("Characters and Layout") {
      row("Allowed characters in tags:") { cell(tagAllowedCharsField).columns(COLUMNS_LARGE) }
      row("Keyboard layout:") { cell(keyboardLayoutCombo).columns(COLUMNS_SHORT) }
      row("Keyboard design:") { cell(keyboardLayoutArea).columns(COLUMNS_SHORT) }
    }
    
    group("Behavior") {
      row("Minimum typed characters (1-10):") { cell(minQueryLengthField).columns(COLUMNS_SHORT) }
    }
    
    group("Colors") {
      row("Caret background:") {
        cell(jumpModeColorWheel)
      }
      row("Tag foreground:") {
        cell(tagForeground1ColorWheel)
        cell(tagForeground2ColorWheel)
      }
      row("Search highlight:") {
        cell(searchHighlightColorWheel)
      }
      row("Editor fade opacity (%):") {
        cell(editorFadeOpacitySlider)
      }
    }
  }
  
  // Property-to-property delegation: https://stackoverflow.com/q/45074596/1772342
  internal var allowedChars by tagAllowedCharsField
  internal var keyboardLayout by keyboardLayoutCombo
  internal var keyChars by keyboardLayoutArea
  internal var minQueryLength by minQueryLengthField
  internal var editorFadeOpacity by editorFadeOpacitySlider
  internal var jumpModeColor by jumpModeColorWheel
  internal var tagForegroundColor1 by tagForeground1ColorWheel
  internal var tagForegroundColor2 by tagForeground2ColorWheel
  internal var searchHighlightColor by searchHighlightColorWheel
  
  internal var minQueryLengthInt
    get() = minQueryLength.toIntOrNull()?.coerceIn(1, 10)
    set(value) { minQueryLength = value.toString() }
  
  internal var editorFadeOpacityPercent
    get() = editorFadeOpacity * 10
    set(value) { editorFadeOpacity = value / 10 }
  
  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    minQueryLength = settings.minQueryLength.toString()
    editorFadeOpacityPercent = settings.editorFadeOpacity
    jumpModeColor = settings.jumpModeColor
    tagForegroundColor1 = settings.tagForegroundColor1
    tagForegroundColor2 = settings.tagForegroundColor2
    searchHighlightColor = settings.searchHighlightColor
  }
  
  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  
  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.lowercase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)
  
  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)
  
  private operator fun JCheckBox.getValue(a: AceSettingsPanel, p: KProperty<*>) = isSelected
  private operator fun JCheckBox.setValue(a: AceSettingsPanel, p: KProperty<*>, selected: Boolean) = setSelected(selected)
  
  private operator fun JSlider.getValue(a: AceSettingsPanel, p: KProperty<*>) = value
  private operator fun JSlider.setValue(a: AceSettingsPanel, p: KProperty<*>, value: Int) = setValue(value)
  
  private operator fun <T> ComboBox<T>.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedItem as T
  private operator fun <T> ComboBox<T>.setValue(a: AceSettingsPanel, p: KProperty<*>, item: T) = setSelectedItem(item)
  
  private inline fun <reified T : Enum<T>> ComboBox<T>.setupEnumItems(crossinline onChanged: (T) -> Unit) {
    T::class.java.enumConstants.forEach(this::addItem)
    addActionListener { onChanged(selectedItem as T) }
  }
}
