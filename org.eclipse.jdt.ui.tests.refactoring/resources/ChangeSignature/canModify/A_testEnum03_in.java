package p;

/**
 * @see #A
 * @see #A()
 * @see A#A
 * @see A#A()
 */
enum A {
    A(1), B(2) { }, C, D(), E { }, F() { }
   	;
   	A() {}
    A(int i) { }
}
