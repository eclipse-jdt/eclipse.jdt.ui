/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

class RenameVirtualMethodRefactoring extends RenameMethodRefactoring {
	
	RenameVirtualMethodRefactoring(IMethod method) {
		super(method);
	}
	
	//------------ preconditions -------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(checkAvailability(getMethod()));
					
		if (Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.no_private")); //$NON-NLS-1$
		if (Flags.isStatic(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.no_static"));	 //$NON-NLS-1$
		if (! getMethod().getDeclaringType().isClass())
			result.addFatalError(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.only_class_methods")); //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 12); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();

			result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));

			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.checking")); //$NON-NLS-1$

			if (MethodChecks.overridesAnotherMethod(getMethod(), new SubProgressMonitor(pm, 2)))
				result.addError(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.overrides_another")); //$NON-NLS-1$

			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$

			if (MethodChecks.isDeclaredInInterface(getMethod(), new SubProgressMonitor(pm, 2)))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameVirtualMethodRefactoring.from_interface", getMethod().getElementName( ))); //$NON-NLS-1$

			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$

			if (hierarchyDeclaresSimilarNativeMethod(new SubProgressMonitor(pm, 2)))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameVirtualMethodRefactoring.requieres_renaming_native",  //$NON-NLS-1$
																		 new String[]{getMethod().getElementName(), "UnsatisfiedLinkError"})); //$NON-NLS-1$

			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$

			if (hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 2), getMethod(), getNewName()))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameVirtualMethodRefactoring.hierarchy_declares1", getNewName())); //$NON-NLS-1$

			return result;
		} finally{
			pm.done();
		}
	}
	
	private boolean hierarchyDeclaresSimilarNativeMethod(IProgressMonitor pm) throws JavaModelException{
		IType[] classes= getMethod().getDeclaringType().newTypeHierarchy(pm).getAllSubtypes(getMethod().getDeclaringType());
		return classesDeclareOverridingNativeMethod(classes);
	}
		
	private boolean classesDeclareOverridingNativeMethod(IType[] classes) throws JavaModelException{
		for (int i= 0; i < classes.length; i++){
			IMethod[] methods= classes[i].getMethods();
			for (int j= 0; j < methods.length; j++){
				if ((!methods[j].equals(getMethod()))
					&& (Flags.isNative(methods[j].getFlags()))
					&& (null != Checks.findMethod(getMethod(), new IMethod[]{methods[j]})))
						return true;
			}
		}
		return false;
	}
}