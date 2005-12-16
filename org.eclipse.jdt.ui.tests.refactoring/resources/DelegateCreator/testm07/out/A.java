package p;

public class A {

	static void /*abstract*/ foo(String/*aaa*/a,/*x*/String/*bar*/ b/*foo*/) /*foo*/{

	}//bar

	/**
	 * @deprecated Use {@link #bar(String,String)} instead
	 */
	static void /*abstract*/ foo(String/*aaa*/a,/*x*/String/*bar*/ b/*foo*/) /*foo*/{
		bar(a, b);
	}//bar
}
