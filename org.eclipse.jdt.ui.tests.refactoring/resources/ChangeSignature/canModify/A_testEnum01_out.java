package p;

/**
 * @see #A(int, int)
 * @see A#A(int, int)
 */
enum A {
    A(1, 17), B(2, 17) { }
   	;
    private A(int i, int a) { }
}
