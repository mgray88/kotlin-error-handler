package isdigital.errorhandler.matchers.retrofit

import isdigital.errorhandler.matchers.retrofit.Range.Companion.of
import org.junit.Assert
import org.junit.Test

class RangeTest {
    @Test
    fun test_check_down_bound() {
        val range =
            of(400, 500)
        Assert.assertTrue(range.contains(400))
        Assert.assertFalse(range.contains(399))
    }

    @Test
    fun test_check_upper_bound() {
        val range =
            of(400, 500)
        Assert.assertTrue(range.contains(500))
        Assert.assertFalse(range.contains(501))
    }

    @Test
    fun test_is_in_range() {
        val range =
            of(400, 500)
        Assert.assertTrue(range.contains(450))
    }

    @Test
    fun test_same_range() {
        val range =
            of(400, 400)
        Assert.assertTrue(range.contains(400))
        Assert.assertFalse(range.contains(500))
    }

    @Test
    fun test_range_equality() {
        val range =
            of(400, 500)
        Assert.assertTrue(
            range.equals(
                of(
                    400,
                    500
                )
            )
        )
        Assert.assertFalse(
            range.equals(
                of(
                    401,
                    500
                )
            )
        )
    }
}
