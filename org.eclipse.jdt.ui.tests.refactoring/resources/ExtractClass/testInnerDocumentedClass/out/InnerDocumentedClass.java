package p;

public class InnerDocumentedClass {
	/* This comment is really important
	 * because it will cause the 
	 * sourceRange to be different
	 */
	public class InnerClass {
		private InnerClassParameter parameterObject = new InnerClassParameter();
	}
}
