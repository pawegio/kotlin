enum class E {
    open fun <caret>foo(n: Int): Int = n

    O
    A {
        override fun foo(n: Int): Int = n + 1

    }
    B {
        override fun foo(n: Int): Int = n + 2

    }
}

// REF: (in E.A).foo(Int)
// REF: (in E.B).foo(Int)