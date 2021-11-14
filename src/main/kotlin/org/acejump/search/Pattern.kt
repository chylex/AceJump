package org.acejump.search

enum class Pattern(val regex: String) {
  LINE_STARTS("^.|^\\n"),
  LINE_ENDS("\\n|\\Z"),
  LINE_INDENTS("[^\\s].*|^\\n"),
  ALL_WORDS("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]"),
  VIM_LWORD("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]"),
  VIM_UWORD("(?<=\\s|\\A)[^\\s]"),
  VIM_LWORD_END("[a-zA-Z0-9_](?=[^a-zA-Z0-9_]|\\Z)"),
  VIM_UWORD_END("[^\\s](?=\\s|\\Z)")
}
