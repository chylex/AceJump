package org.acejump.input

/**
 * Defines common keyboard layouts. Each layout has a key priority order, based on each key's distance from the home row and how
 * ergonomically difficult they are to press.
 */
@Suppress("unused", "SpellCheckingInspection")
enum class KeyLayout(
  internal val rows: Array<String>,
  priority: String,
  private val characterSides: Pair<Set<Char>, Set<Char>> = Pair(emptySet(), emptySet()),
  internal val characterRemapping: Map<Char, Char> = emptyMap(),
) {
  COLEMK(arrayOf("1234567890", "qwfpgjluy", "arstdhneio", "zxcvbkm"), priority = "tndhseriaovkcmbxzgjplfuwyq5849673210"),
  WORKMN(arrayOf("1234567890", "qdrwbjfup", "ashtgyneoi", "zxmcvkl"), priority = "tnhegysoaiclvkmxzwfrubjdpq5849673210"),
  DVORAK(arrayOf("1234567890", "pyfgcrl", "aoeuidhtns", "qjkxbmwvz"), priority = "uhetidonasxkbjmqwvzgfycprl5849673210"),
  QWERTY(arrayOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"), priority = "fjghdkslavncmbxzrutyeiwoqp5849673210", characterSides = sides(listOf("123456", "qwert", "asdfg", "zxcvb"), listOf("7890", "yuiop", "hjkl", "nm"))),
  QWERTZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210", characterSides = sides(listOf("123456", "qwert", "asdfg", "yxcvb"), listOf("7890", "zuiop", "hjkl", "nm"))),
  QWERTZ_CZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210", characterSides = sides(listOf("123456", "qwert", "asdfg", "yxcvb"), listOf("7890", "zuiop", "hjkl", "nm")), characterRemapping = mapOf(
    '+' to '1',
    'ě' to '2',
    'š' to '3',
    'č' to '4',
    'ř' to '5',
    'ž' to '6',
    'ý' to '7',
    'á' to '8',
    'í' to '9',
    'é' to '0'
  )),
  QGMLWY(arrayOf("1234567890", "qgmlwyfub", "dstnriaeoh", "zxcvjkp"), priority = "naterisodhvkcpjxzlfmuwygbq5849673210"),
  QGMLWB(arrayOf("1234567890", "qgmlwbyuv", "dstnriaeoh", "zxcfjkp"), priority = "naterisodhfkcpjxzlymuwbgvq5849673210"),
  NORMAN(arrayOf("1234567890", "qwdfkjurl", "asetgynioh", "zxcvbpm"), priority = "tneigysoahbvpcmxzjkufrdlwq5849673210");
  
  internal val allChars = rows.joinToString("").toCharArray().apply(CharArray::sort).joinToString("")
  private val allPriorities = priority.mapIndexed { index, char -> char to index }.toMap()
  
  fun priority(char: Char): Int {
    return allPriorities[char] ?: allChars.length
  }
  
  fun areOnSameSide(c1: Char, c2: Char): Boolean {
    return (c1 in characterSides.first && c2 in characterSides.first) || (c1 in characterSides.second && c2 in characterSides.second)
  }
}

private fun sides(left: List<String>, right: List<String>): Pair<Set<Char>, Set<Char>> {
  return Pair(
    left.flatMapTo(mutableSetOf()) { it.toCharArray().toSet() },
    right.flatMapTo(mutableSetOf()) { it.toCharArray().toSet() }
  )
}
