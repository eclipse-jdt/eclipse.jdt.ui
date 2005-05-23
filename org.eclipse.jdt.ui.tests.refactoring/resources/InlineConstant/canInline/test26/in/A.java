//5, 42, 5, 42
package p;

class A {
	private static final StringBuffer logger_= getLogger(A.class);

	public static void handleException(final Throwable t) {
		System.out.println(logger_);
	}

	private static StringBuffer getLogger(Class name) {
		return null;
	}
}
