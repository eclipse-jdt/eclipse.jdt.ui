package org.eclipse.jdt.internal.corext.refactoring.base;

public class RefactoringStatusCodes {

	private RefactoringStatusCodes() {
	}

	public static final int NONE= 0;
	public static final int OVERRIDES_ANOTHER_METHOD= 1;
	public static final int METHOD_DECLARED_IN_INTERFACE= 2;
	
	public static final int EXPRESSION_NOT_RVALUE= 64;
	public static final int EXPRESSION_NOT_RVALUE_VOID= 65;
	public static final int EXTRANEOUS_TEXT= 66;
}
