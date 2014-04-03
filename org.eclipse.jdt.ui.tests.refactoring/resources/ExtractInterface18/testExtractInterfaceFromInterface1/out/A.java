package p;

interface A extends B {
	/* (non-Javadoc)
	 * @see p.B#defaultM1(java.lang.String)
	 */
	public default void defaultM1(String s) {
		System.out.println(s);
	}

	public static void statictM1(String s) {
		System.out.println(s);
	}
}