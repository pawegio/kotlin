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

package kotlin.js

public class Pattern private(private val regex: String) { // TODO: Flags

    public fun pattern(): String = regex
    override public fun toString(): String = regex

    public fun matcher(input: String): Matcher = Matcher(this, input)

    companion object {

        public fun compile(regex: String): Pattern = Pattern(regex)

    }
}

public class Matcher(val pattern: Pattern, val input: String) {

}