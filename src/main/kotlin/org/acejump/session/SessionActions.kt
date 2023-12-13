package org.acejump.session

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.search.SearchQuery
import org.acejump.view.TagMarker

internal interface SessionActions {
  fun showHighlights(results: Map<Editor, IntList>, query: SearchQuery)
  fun hideHighlights()
  
  fun setTagMarkers(markers: Map<Editor, Collection<TagMarker>>)
}
