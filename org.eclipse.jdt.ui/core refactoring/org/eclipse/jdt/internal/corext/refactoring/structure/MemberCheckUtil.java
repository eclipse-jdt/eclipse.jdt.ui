package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class MemberCheckUtil {
	
	private MemberCheckUtil(){
	}
	
	public static RefactoringStatus checkMembersInDestinationType(IMember[] members, IType destinationType) throws JavaModelException {	
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < members.length; i++) {
			if (members[i].getElementType() == IJavaElement.METHOD)
				checkMethodInType(destinationType, result, (IMethod)members[i]);
			else 
			if (members[i].getElementType() == IJavaElement.FIELD)
				checkFieldInType(destinationType, result, (IField)members[i]);
		}
		return result;	
	}

	private static void checkMethodInType(IType destinationType, RefactoringStatus result, IMethod method) throws JavaModelException {
		IMethod[] destinationTypeMethods= destinationType.getMethods();
		IMethod found= findMethod(method, destinationTypeMethods);
		if (found != null){
			Context context= JavaSourceContext.create(destinationType.getCompilationUnit(), found.getSourceRange());
			String message= RefactoringCoreMessages.getFormattedString("MemberChecksUtil.signature_exists", //$NON-NLS-1$
					new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
			result.addError(message, context);
		} else {
			IMethod similar= Checks.findMethod(method, destinationType);
			if (similar != null){
				String message= RefactoringCoreMessages.getFormattedString("MemberChecksUtil.same_param_count",//$NON-NLS-1$
						 new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
				Context context= JavaSourceContext.create(destinationType.getCompilationUnit(), similar.getSourceRange());
				result.addWarning(message, context);
			}										
		}	
	}
	
	private static void checkFieldInType(IType destinationType, RefactoringStatus result, IField field) throws JavaModelException {
		IField destinationTypeField= destinationType.getField(field.getElementName());	
		if (! destinationTypeField.exists())
			return;
		String message= RefactoringCoreMessages.getFormattedString("MemberChecksUtil.field_exists", //$NON-NLS-1$
				new String[]{field.getElementName(), JavaModelUtil.getFullyQualifiedName(destinationType)});
		Context context= JavaSourceContext.create(destinationType.getCompilationUnit(), destinationTypeField.getSourceRange());
		result.addError(message, context);
	}
	
	/**
	 * Finds a method in a list of methods.
	 * @return The found method or <code>null</code>, if nothing found
	 */
	public static IMethod findMethod(IMethod method, IMethod[] allMethods) throws JavaModelException {
		String name= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		boolean isConstructor= method.isConstructor();

		for (int i= 0; i < allMethods.length; i++) {
			if (JavaModelUtil.isSameMethodSignature(name, paramTypes, isConstructor, allMethods[i]))
				return allMethods[i];
		}
		return null;
	}
}
