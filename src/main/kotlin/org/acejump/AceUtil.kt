package org.acejump

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.util.IncorrectOperationException
import it.unimi.dsi.fastutil.ints.IntArrayList

/**
 * Returns an immutable version of the currently edited document.
 */
val Editor.immutableText
  get() = this.document.immutableCharSequence

/**
 * Returns all open editors in the project.
 */
val Project.openEditors: List<Editor>
  get() {
    return try {
      FileEditorManagerEx.getInstanceEx(this)
        .splitters
        .getSelectedEditors()
        .mapNotNull { (it as? TextEditor)?.editor }
    } catch (e: IncorrectOperationException) {
      emptyList()
    }
  }

/**
 * Returns true if [this] contains [otherText] at the specified offset.
 */
fun CharSequence.matchesAt(selfOffset: Int, otherText: String, ignoreCase: Boolean): Boolean {
  return this.regionMatches(selfOffset, otherText, 0, otherText.length, ignoreCase)
}

/**
 * Calculates the length of a common prefix in [this] starting at index [selfOffset], and [otherText] starting at index 0.
 */
fun CharSequence.countMatchingCharacters(selfOffset: Int, otherText: String): Int {
  var i = 0
  var o = selfOffset + i
  
  while (i < otherText.length && o < this.length && otherText[i].equals(this[o], ignoreCase = true)) {
    i++
    o++
  }
  
  return i
}

fun MutableMap<Editor, IntArrayList>.clone(): MutableMap<Editor, IntArrayList> {
  val clone = HashMap<Editor, IntArrayList>(size)
  
  for ((editor, offsets) in this) {
    clone[editor] = offsets.clone()
  }
  
  return clone
}
