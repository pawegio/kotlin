// "Change type from 'String' to '(LinkedHashSet<Int>) -> HashSet<Int>'" "true"

import java.util.HashSet
import java.util.LinkedHashSet

fun foo(f: ((java.util.LinkedHashSet<Int>) -> java.util.HashSet<Int>) -> String) {
    foo {
        (f: (LinkedHashSet<Int>) -> HashSet<Int>) -> "42"
    }
}