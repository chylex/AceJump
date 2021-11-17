
import org.acejump.test.util.BaseTest

/**
 * Functional test cases and end-to-end performance tests.
 *
 * TODO: Add more structure to test cases, use test resources to define files.
 */

class AceTest : BaseTest() {
  fun `test that scanner finds all occurrences of single character`() =
    assertEquals("test test test".search("t"), setOf(0, 3, 5, 8, 10, 13))

  fun `test empty results for an absent query`() =
    assertEmpty("test test test".search("best"))

  fun `test sticky results on a query with extra characters`() =
    assertEquals("test test test".search("testz"), setOf(0, 5, 10))

  fun `test a query inside text with some variations`() =
    assertEquals("abcd dabc cdab".search("cd"), setOf(2, 10))

  fun `test a query containing a space character`() =
    assertEquals("abcd dabc cdab".search("cd "), setOf(2))

  fun `test a query containing a { character`() =
    assertEquals("abcd{dabc cdab".search("cd{"), setOf(2))

  fun `test tag selection`() {
    "<caret>testing 1234".search("g")

    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("testin<caret>g 1234")
  }
}
