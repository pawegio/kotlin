val Array<String>.firstElement: String get() = get(0)

fun box(): String {
    val p = Array<String>::firstElement
    return p.get(array("OK", "Fail"))
}
