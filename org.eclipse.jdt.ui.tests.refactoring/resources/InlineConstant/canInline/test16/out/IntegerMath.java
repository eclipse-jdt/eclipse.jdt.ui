//8, 23 -> 8, 23  replaceAll == true, removeDeclaration == true
package p;

class IntegerMath<E> {
	/**
	 * This uses {@link #PI}
	 */
	int getCircumference(int radius) {
		return 2 * radius * 3/*.14159265*/;
	}
}

/**
 * @see IntegerMath#PI
 */
class Test {
	int c= new IntegerMath<String>().getCircumference(3);
}