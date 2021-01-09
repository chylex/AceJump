package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.GrowPolicy.MEDIUM_TEXT
import com.intellij.ui.layout.GrowPolicy.SHORT_TEXT
import com.intellij.ui.layout.panel
import org.acejump.input.KeyLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.text.JTextComponent
import kotlin.reflect.KProperty

/**
 * Settings view located in File | Settings | Tools | AceJump.
 */
@Suppress("UsePropertyAccessSyntax")
internal class AceSettingsPanel {
  private val tagCharsField = JBTextField()
  private val keyboardLayoutCombo = ComboBox<KeyLayout>()
  private val keyboardLayoutArea = JBTextArea().apply { isEditable = false }
  private val minQueryLengthField = JBTextField()
  private val jumpModeColorWheel = ColorPanel()
  private val fromCaretModeColorWheel = ColorPanel()
  private val betweenPointsModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()
  private val acceptedTagColorWheel = ColorPanel()
  
  init {
    tagCharsField.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutArea.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutCombo.setupEnumItems { keyChars = it.rows.joinToString("\n") }
  }
  
  internal val rootPanel: JPanel = panel {
    fun Cell.short(component: JComponent) = component(growPolicy = SHORT_TEXT)
    fun Cell.medium(component: JComponent) = component(growPolicy = MEDIUM_TEXT)
    
    titledRow("Characters and Layout") {
      row("Allowed characters in tags:") { medium(tagCharsField) }
      row("Keyboard layout:") { short(keyboardLayoutCombo) }
      row("Keyboard design:") { short(keyboardLayoutArea) }
    }
    
    titledRow("Behavior") {
      row("Minimum typed characters (1-10):") { short(minQueryLengthField) }
    }
    
    titledRow("Colors") {
      row("Jump mode caret background:") { short(jumpModeColorWheel) }
      row("From Caret mode caret background:") { short(fromCaretModeColorWheel) }
      row("Between Points mode caret background:") { short(betweenPointsModeColorWheel) }
      row("Searched text background:") { short(textHighlightColorWheel) }
      row("Tag foreground:") { short(tagForegroundColorWheel) }
      row("Tag background:") { short(tagBackgroundColorWheel) }
      row("Accepted tag position background:") { short(acceptedTagColorWheel) }
    }
  }
  
  // Property-to-property delegation: https://stackoverflow.com/q/45074596/1772342
  internal var allowedChars by tagCharsField
  internal var keyboardLayout by keyboardLayoutCombo
  internal var keyChars by keyboardLayoutArea
  internal var minQueryLength by minQueryLengthField
  internal var jumpModeColor by jumpModeColorWheel
  internal var fromCaretModeColor by fromCaretModeColorWheel
  internal var betweenPointsModeColor by betweenPointsModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel
  internal var acceptedTagColor by acceptedTagColorWheel
  
  internal var minQueryLengthInt
    get() = minQueryLength.toIntOrNull()?.coerceIn(1, 10)
    set(value) { minQueryLength = value.toString() }
  
  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    minQueryLength = settings.minQueryLength.toString()
    jumpModeColor = settings.jumpModeColor
    fromCaretModeColor = settings.fromCaretModeColor
    betweenPointsModeColor = settings.betweenPointsModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
    acceptedTagColor = settings.acceptedTagColor
  }
  
  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  
  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.toLowerCase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)
  
  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)
  
  private operator fun JCheckBox.getValue(a: AceSettingsPanel, p: KProperty<*>) = isSelected
  private operator fun JCheckBox.setValue(a: AceSettingsPanel, p: KProperty<*>, selected: Boolean) = setSelected(selected)
  
  private operator fun <T> ComboBox<T>.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedItem as T
  private operator fun <T> ComboBox<T>.setValue(a: AceSettingsPanel, p: KProperty<*>, item: T) = setSelectedItem(item)
  
  private inline fun <reified T : Enum<T>> ComboBox<T>.setupEnumItems(crossinline onChanged: (T) -> Unit) {
    T::class.java.enumConstants.forEach(this::addItem)
    addActionListener { onChanged(selectedItem as T) }
  }
}
