/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

class RenameMethodInInterfaceRefactoring extends RenameMethodRefactoring {

	RenameMethodInInterfaceRefactoring(IMethod method){
		super(method);
	}
		
	//---- preconditions ---------------------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		if (! getMethod().getDeclaringType().isInterface())
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.no_class_method")); //$NON-NLS-1$
		return result;
	}
	
	/*
	 * non java-doc
	 * @see Refactoring#checkActivation
	 */		
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkActivation(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;			
			
			result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			return result;
		} finally {
			pm.done();
		}
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 11); //$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 6)));
			if (result.hasFatalError())
				return result;
			if (isSpecialCase())
				result.addError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.special_case")); //$NON-NLS-1$
			pm.worked(1);
			IMethod relatedMethod= relatedTypeDeclaresMethodName(new SubProgressMonitor(pm, 3), getMethod(), getNewName());
			if (relatedMethod != null){
				Context context= JavaSourceContext.create(relatedMethod);
				result.addError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.already_defined"), context); //$NON-NLS-1$
			}	
			return result;
		} finally {
			pm.done();
		}
	}
		
	private IMethod relatedTypeDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			Set types= getRelatedTypes(new SubProgressMonitor(pm, 1));
			for (Iterator iter= types.iterator(); iter.hasNext(); ) {
				IMethod m= Checks.findMethod(method, (IType)iter.next());
				IMethod hierarchyMethod= hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), m, newName);
				if (hierarchyMethod != null)
					return hierarchyMethod;
			}
			return null;
		} finally {
			pm.done();
		}	
	}

	/*
	 * special cases are mentioned in the java lang spec 2nd edition (9.2)
	 */
	private boolean isSpecialCase() throws JavaModelException {
		String[] noParams= new String[0];
		String[] specialNames= new String[]{"toString", "toString", "toString", "toString", "equals", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"equals", "getClass", "getClass", "hashCode", "notify", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"notifyAll", "wait", "wait", "wait"}; //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		String[][] specialParamTypes= new String[][]{noParams, noParams, noParams, noParams,
													 {"QObject;"}, {"Qjava.lang.Object;"}, noParams, noParams, //$NON-NLS-2$ //$NON-NLS-1$
													 noParams, noParams, noParams, {Signature.SIG_LONG, Signature.SIG_INT},
													 {Signature.SIG_LONG}, noParams};
		String[] specialReturnTypes= new String[]{"QString;", "QString;", "Qjava.lang.String;", "Qjava.lang.String;", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
												   Signature.SIG_BOOLEAN, Signature.SIG_BOOLEAN, "QClass;", "Qjava.lang.Class;", //$NON-NLS-2$ //$NON-NLS-1$
												   Signature.SIG_INT, Signature.SIG_VOID, Signature.SIG_VOID, Signature.SIG_VOID,
												   Signature.SIG_VOID, Signature.SIG_VOID};
		Assert.isTrue((specialNames.length == specialParamTypes.length) && (specialParamTypes.length == specialReturnTypes.length));
		for (int i= 0; i < specialNames.length; i++){
			if (specialNames[i].equals(getNewName()) 
				&& Checks.compareParamTypes(getMethod().getParameterTypes(), specialParamTypes[i]) 
				&& !specialReturnTypes[i].equals(getMethod().getReturnType())){
					return true;
			}
		}
		return false;		
	}
	
	private Set getRelatedTypes(IProgressMonitor pm) throws JavaModelException {
		Set methods= getMethodsToRename(getMethod(), pm, null);
		Set result= new HashSet(methods.size());
		for (Iterator iter= methods.iterator(); iter.hasNext(); ){
			result.add(((IMethod)iter.next()).getDeclaringType());
		}
		return result;
	}
}