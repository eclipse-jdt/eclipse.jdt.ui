package p;

public class InnerDocumentedClass {
	/* This comment is really important
	 * because it will cause the 
	 * sourceRange to be different
	 */
	public class InnerClass {
		private int foo;
	}
}
