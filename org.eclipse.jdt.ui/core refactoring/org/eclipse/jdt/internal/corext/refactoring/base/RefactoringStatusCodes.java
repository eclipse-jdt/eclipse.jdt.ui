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
	
	public static final int NOT_STATIC_FINAL_SELECTED= 128;
	public static final int SYNTAX_ERRORS= 129;
	public static final int DECLARED_IN_CLASSFILE= 130;
	public static final int CANNOT_INLINE_BLANK_FINAL= 131;
	public static final int LOCAL_AND_ANONYMOUS_NOT_SUPPORTED= 132;
	public static final int REFERENCE_IN_CLASSFILE= 133;
	public static final int WILL_NOT_REMOVE_DECLARATION= 134;
}
