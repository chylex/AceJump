package org.acejump.search

import org.acejump.view.Tag

internal sealed class TaggingResult {
  class Accept(val offset: Int) : TaggingResult()
  class Mark(val tags: List<Tag>) : TaggingResult()
}
