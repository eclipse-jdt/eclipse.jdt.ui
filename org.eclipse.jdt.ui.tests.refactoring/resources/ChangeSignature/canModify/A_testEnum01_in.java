package p;

/**
 * @see #A(int)
 * @see A#A(int)
 */
enum A {
    A(1), B(2) { }
   	;
    A(int i) { }
}
