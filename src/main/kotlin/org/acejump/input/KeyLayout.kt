package org.acejump.input

/**
 * Defines common keyboard layouts. Each layout has a key priority order, based on each key's distance from the home row and how
 * ergonomically difficult they are to press.
 */
@Suppress("unused", "SpellCheckingInspection")
enum class KeyLayout(internal val rows: Array<String>, priority: String, internal val characterRemapping: Map<Char, Char> = emptyMap()) {
  COLEMK(arrayOf("1234567890", "qwfpgjluy", "arstdhneio", "zxcvbkm"), priority = "tndhseriaovkcmbxzgjplfuwyq5849673210"),
  WORKMN(arrayOf("1234567890", "qdrwbjfup", "ashtgyneoi", "zxmcvkl"), priority = "tnhegysoaiclvkmxzwfrubjdpq5849673210"),
  DVORAK(arrayOf("1234567890", "pyfgcrl", "aoeuidhtns", "qjkxbmwvz"), priority = "uhetidonasxkbjmqwvzgfycprl5849673210"),
  QWERTY(arrayOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"), priority = "fjghdkslavncmbxzrutyeiwoqp5849673210"),
  QWERTZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210"),
  QWERTZ_CZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210", characterRemapping =  mapOf(
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
  internal val allPriorities = priority.mapIndexed { index, char -> char to index }.toMap()
  
  internal fun priority(): (Char) -> Int {
    return { allPriorities.getOrDefault(it, Int.MAX_VALUE) }
  }
}
