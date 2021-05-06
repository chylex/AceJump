package org.acejump.search

import com.intellij.openapi.editor.Editor
import org.acejump.view.TagMarker

internal sealed class TaggingResult {
  class Accept(val tag: Tag) : TaggingResult()
  class Mark(val markers: MutableMap<Editor, Collection<TagMarker>>) : TaggingResult()
}
