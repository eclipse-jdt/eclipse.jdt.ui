package p;

/**
 * @see #A
 * @see #A(Object)
 * @see A#A
 * @see A#A(Object)
 */
enum A {
    A(1), B(2) { }, C(null), D(null), E(null) { }, F(null) { }
   	;
   	A(Object obj) {}
    A(int i) { }
}
