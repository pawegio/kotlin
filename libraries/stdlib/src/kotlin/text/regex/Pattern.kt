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
import java.util.regex.Pattern as NativePattern
import java.util.regex.Matcher
import kotlin.properties.Delegates

private val TODO: Nothing get() = throw UnsupportedOperationException()

public trait FlagEnum {
    public val value: Int
    public val mask: Int
}

public enum class PatternOptions(override val value: Int, override val mask: Int = value) : FlagEnum {
    IGNORE_CASE : PatternOptions(0x01)
    MULTILINE : PatternOptions(0x02)
    // ... other options


}


public trait MatchResult : MatchGroup {
    public val groups: List<MatchGroup?>
    public fun next(): MatchResult?
}


public trait MatchGroup {
    public val range: IntRange
    public val value: String
}



public class Pattern /* internal */ ( /* internal */ val nativePattern: NativePattern) {

    public constructor(pattern: String, options: Set<PatternOptions>): this(NativePattern.compile(pattern))
    public constructor(pattern: String, vararg options: PatternOptions) : this(pattern, options.toSet())

    public val pattern: String
        get() = nativePattern.pattern()

    public val options: Set<PatternOptions>
        get() = TODO


    public fun matches(string: CharSequence): Boolean = nativePattern.matcher(string).matches()

    public fun match(string: CharSequence): MatchResult? {
        val matcher = nativePattern.matcher(string)
        return if (matcher.matches())
            matcher.findNext()
        else
            null
    }

    public fun matchAll(string: CharSequence): Sequence<MatchResult> = sequence({ match(string) }, { match -> match.next() })

    public fun replace(string: CharSequence, replacement: String): String = nativePattern.matcher(string).replaceAll(replacement)
    public fun replace(string: CharSequence, evaluator: (MatchResult) -> String): String = TODO

    public fun split(string: CharSequence, limit: Int = 0): List<String> = nativePattern.split(string, limit).asList()

    companion object {}
}


public fun Pattern.Companion.escape(literal: String): String = NativePattern.quote(literal)
public fun Pattern.Companion.escapeReplacement(literal: String): String = Matcher.quoteReplacement(literal)




private fun Matcher.findNext(): MatchResult? {
    if (!find())
        return null

    var matchResult = this as java.util.regex.MatchResult

    return object: MatchResult {
        override val range: IntRange
            get() = matchResult.start()..matchResult.end()-1
        override val value: String
            get() = matchResult.group()

        override val groups: List<MatchGroup?> by Delegates.lazy {
            // TODO:  wrap
            val groups = ArrayList<MatchGroup?>(matchResult.groupCount())
            for (groupIndex in 1..groupCount()) {
                val range = matchResult.start(groupIndex)..matchResult.end(groupIndex)-1
                if (range.start >= 0)
                    groups.add(object: MatchGroup {
                        override val range: IntRange = range
                        override val value: String
                            get() = matchResult.group(groupIndex)
                    })
                else
                    groups.add(null)
            }
            groups
        }

        override fun next(): MatchResult? {
            matchResult = this@findNext.toMatchResult()
            return this@findNext.findNext() // TODO: advance next
        }
    }
}