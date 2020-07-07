/*
 * The MIT License
 *
 * Copyright (c) 2013-2016 Workable SA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package isdigital.errorhandler

/**
 * Container to ease passing around a tuple of two objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
class ActionEntry
/**
 * Constructor for an ActionEntry.
 *
 * @param matcher the matcher object in the ActionEntry
 * @param action  the action object in the ActionEntry
 */(
    val matcher: Matcher,
    val action: Action
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ActionEntry
        return if (matcher != that.matcher) false else action == that.action
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the ActionEntry
     */
    override fun hashCode(): Int {
        return matcher.hashCode() xor action.hashCode()
    }

    companion object {
        /**
         * Convenience method for creating an ActionEntry
         *
         * @param matcher the matcher object in the ActionEntry
         * @param action  the action object in the ActionEntry
         * @return a new ActionEntry
         */
        fun from(
            matcher: Matcher,
            action: Action
        ): ActionEntry {
            return ActionEntry(matcher, action)
        }
    }
}
