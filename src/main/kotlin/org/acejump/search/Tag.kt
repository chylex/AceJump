package org.acejump.search

import com.intellij.openapi.editor.Editor

data class Tag(val editor: Editor, val offset: Int) {
  override fun equals(other: Any?): Boolean {
    return other is Tag && other.offset == offset && other.editor === editor
  }
  
  override fun hashCode(): Int {
    return (offset * 31) + editor.hashCode()
  }
}
