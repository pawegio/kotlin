/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text

import java.util.ArrayList


public enum class RegexOption(val value: String) {
    IGNORE_CASE : RegexOption("i")
    MULTILINE : RegexOption("m")
}


public data class MatchGroup(val value: String)


public class Regex(pattern: String, options: Set<RegexOption>) {

    public val pattern: String = pattern
    public val options: Set<RegexOption> = options.toSet()
    private val nativePattern: RegExp = RegExp(pattern, options.map { it.value }.joinToString() + "g")


    public fun matches(input: CharSequence): Boolean {
        nativePattern.reset()
        return nativePattern.test(input.toString())
    }

    public fun match(input: CharSequence): MatchResult? = nativePattern.findNext(input.toString(), 0)

    public fun matchAll(input: CharSequence): Sequence<MatchResult> = sequence({ match(input) }, { match -> match.next() })

    public fun replace(input: CharSequence, replacement: String): String = input.toString().nativeReplace(nativePattern, replacement)

    public inline fun replace(input: CharSequence, transform: (MatchResult) -> String): String {
        var match = match(input)
        if (match == null) return input.toString()

        var lastStart = 0
        val length = input.length()
        val sb = StringBuilder(length)
        do {
            val foundMatch = match!!
            sb.append(input, lastStart, foundMatch.range.start)
            sb.append(transform(foundMatch))
            lastStart = foundMatch.range.end + 1
            match = foundMatch.next()
        } while (lastStart < length && match != null)

        if (lastStart < length) {
            sb.append(input, lastStart, length)
        }

        return sb.toString()
    }

    public fun replaceFirst(input: CharSequence, replacement: String): String =
            input.toString().nativeReplace(RegExp(pattern, options.map { it.value }.joinToString()), replacement)

    public fun split(input: CharSequence, limit: Int = 0): List<String> {
        require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
        val matches = matchAll(input).let { if (limit == 0) it else it.take(limit - 1) }
        val result = ArrayList<String>()
        var lastStart = 0

        for (match in matches) {
            result.add(input.subSequence(lastStart, match.range.start).toString())
            lastStart = match.range.end + 1
        }
        result.add(input.subSequence(lastStart, input.length()).toString())
        return result
    }

    public override fun toString(): String = nativePattern.toString()

    companion object {
        public fun fromLiteral(literal: String): Regex = Regex(escape(literal))
        public fun escape(literal: String): String = literal.nativeReplace(patternEscape, "\\$&")
        public fun escapeReplacement(literal: String): String = literal.nativeReplace(replacementEscape, "$$$$")

        private val patternEscape = RegExp("""[-\\^$*+?.()|[\]{}]""", "g")
        private val replacementEscape = RegExp("""\$""", "g")
    }
}

public fun Regex(pattern: String, vararg options: RegexOption): Regex = Regex(pattern, options.toSet())


private fun RegExp.findNext(input: String, from: Int): MatchResult? {
    this.lastIndex = from
    val match = exec(input)
    if (match == null) return null
    val reMatch = match as RegExpMatch
    val range = reMatch.index..lastIndex-1

    return object : MatchResult {
        override val range: IntRange = range
        override val value: String
            get() = match[0]!!

        override val groups: MatchGroupCollection = object : MatchGroupCollection {
            override fun size(): Int = match.size()
            override fun isEmpty(): Boolean = size() == 0

            override fun contains(o: Any?): Boolean = this.any { it == o }
            override fun containsAll(c: Collection<Any?>): Boolean = c.all({contains(it)})

            override fun iterator(): Iterator<MatchGroup?> = indices.sequence().map { this[it] }.iterator()

            override fun get(index: Int): MatchGroup? = match[index]?.let { MatchGroup(it) }
        }

        override fun next(): MatchResult? = this@findNext.findNext(input, range.end + 1)
    }
}