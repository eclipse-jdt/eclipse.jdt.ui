//8, 23 -> 8, 23  replaceAll == true, removeDeclaration == true
package p;

class IntegerMath<E> {
	/**
	 * This is {@link #PI}
	 */
	static final int PI= 3;
	/**
	 * This uses {@link #PI}
	 */
	int getCircumference(int radius) {
		return 2 * radius * PI/*.14159265*/;
	}
}

/**
 * @see IntegerMath#PI
 */
class Test {
	int c= new IntegerMath<String>().getCircumference(IntegerMath.PI);
}