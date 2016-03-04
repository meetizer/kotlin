abstract class Base {
    override fun equals(other: Any?) = other is Base
}

class Derived1 : Base() {
    fun foo() {}
}

class Derived2 : Base()

fun check(x1: Derived1, x: Base) {
    if (x1 == x) {
        // Smart cast here will provoke CCA
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}