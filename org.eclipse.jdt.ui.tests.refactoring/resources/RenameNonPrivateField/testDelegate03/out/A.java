package p;

class A {
   
	public static final String g= "FOO";
	/**
	 * @deprecated Use {@link #g} instead
	 */
	public static final String f= g;

	public String getG() {
		return g;
	}

	/**
	 * @deprecated Use {@link #getG()} instead
	 */
	public String getF() {
		return getG();
	}

}
