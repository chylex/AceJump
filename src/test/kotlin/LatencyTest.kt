
import org.acejump.action.AceVimAction
import org.acejump.test.util.BaseTest
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class LatencyTest : BaseTest() {
  
  private fun `test tag latency`(editorText: String) {
    val chars = editorText.toCharArray().distinct().filter { !it.isWhitespace() }
    val avg = averageTimeWithWarmup(warmupRuns = 10, timedRuns = 10) {
      var time = 0L
      
      for (query in chars) {
        makeEditor(editorText)
        myFixture.testAction(AceVimAction.JumpAllEditors())
        time += measureTimeMillis { typeAndWaitForResults("$query") }
        // TODO assert(Tagger.markers.isNotEmpty()) { "Should be tagged: $query" }
        resetEditor()
      }
      
      time
    }
    
    println("Average time to tag results: ${String.format("%.1f", avg.toDouble() / chars.size)} ms")
  }
  
  fun `test random text latency`() = `test tag latency`(
    generateSequence {
      generateSequence {
        generateSequence {
          ('a'..'z').random(Random(0))
        }.take(5).joinToString("")
      }.take(20).joinToString(" ")
    }.take(100).joinToString("\n")
  )
  
  fun `test lorem ipsum latency`() = `test tag latency`(
    File(
      javaClass.classLoader.getResource("lipsum.txt")!!.file
    ).readText()
  )
}
