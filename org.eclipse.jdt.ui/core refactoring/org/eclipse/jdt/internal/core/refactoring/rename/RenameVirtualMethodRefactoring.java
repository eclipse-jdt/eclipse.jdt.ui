/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

class RenameVirtualMethodRefactoring extends RenameMethodRefactoring {
	
	RenameVirtualMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
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

			if (overridesAnotherMethod(new SubProgressMonitor(pm, 2)))
				result.addError(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.overrides_another")); //$NON-NLS-1$

			pm.subTask(RefactoringCoreMessages.getString("RenameVirtualMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$

			if (isDeclaredInInterface(new SubProgressMonitor(pm, 2)))
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
	
	private boolean isDeclaredInInterface(IProgressMonitor pm) throws JavaModelException {
		try{	
			pm.beginTask("", 4);
			ITypeHierarchy hier= getMethod().getDeclaringType().newTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] classes= hier.getAllClasses();
			IProgressMonitor subPm= new SubProgressMonitor(pm, 3);
			subPm.beginTask("", classes.length);
			for (int i= 0; i < classes.length; i++) {
				ITypeHierarchy superTypes= classes[i].newSupertypeHierarchy(new SubProgressMonitor(subPm, 1));
				IType[] superinterfaces= superTypes.getAllSuperInterfaces(classes[i]);
				for (int j= 0; j < superinterfaces.length; j++) {
					if (findMethod(getMethod(), superinterfaces[j]) != null)
						return true;
				}
				subPm.worked(1);
			}
			return false;
		} finally{
			pm.done();
		}
	}
	
	
	private boolean hierarchyDeclaresSimilarNativeMethod(IProgressMonitor pm) throws JavaModelException{
		IType[] classes= getMethod().getDeclaringType().newTypeHierarchy(pm).getAllSubtypes(getMethod().getDeclaringType());
		return classesDeclareOverridingNativeMethod(classes);
	}
		
	boolean overridesAnotherMethod(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		//XXX: use the commented code once this is fixed: 1GCZZS1: ITPJCORE:WINNT - inconsistent search for method declarations
		//XXX: and delete findMethod
		
//		IType declaringType= getMethod().getDeclaringType();	
//		ITypeHierarchy superTypes= declaringType.newSupertypeHierarchy(new SubProgressMonitor(pm, 1));
//		pm.worked(1);
//		IJavaSearchScope scope= SearchEngine.createHierarchyScope(declaringType);
//		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.DECLARATIONS);
//		SearchResultCollector collector= new SearchResultCollector(new SubProgressMonitor(pm, 1));
//		new SearchEngine().search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);
//		pm.done();
//		for (Iterator iter= collector.getResults().iterator(); iter.hasNext(); ){
//			SearchResult result= (SearchResult)iter.next();
//			IType t= ((IMethod)result.getEnclosingElement()).getDeclaringType();
//			if ((!t.equals(declaringType)) && superTypes.contains(t))
//				return true;
//		}
//		return false;
		IMethod found= findInHierarchy(getMethod().getDeclaringType().newSupertypeHierarchy(new SubProgressMonitor(pm, 1)), getMethod());
		return (found != null && (! Flags.isStatic(found.getFlags())) && (! Flags.isPrivate(found.getFlags())));	
	}
	
	/**
	 * Finds a method in a type's hierarchy
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * The input type of the hierarchy is not searched for the method
	 * @return The first found method or null, if nothing found
	 */	
	private static final IMethod findInHierarchy(ITypeHierarchy hierarchy, IMethod method) throws JavaModelException {
		IType curr= hierarchy.getSuperclass(hierarchy.getType());
		while (curr != null) {
			IMethod found= findMethod(method, curr);
			if (found != null) {
				return found;
			}
			curr= hierarchy.getSuperclass(curr);
		}
		return null;
	}
	
	
	/* non java-doc
	 * declared in RenameMethodRefactoring#getMethodsToRename
	 */
	Set getMethodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException {
		Set methods= new HashSet();
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(pm);
		IType[] subtypes= hier.getAllSubtypes(type);
		for (int i= 0; i < subtypes.length; i++){
			IMethod subMethod= findMethod(method, subtypes[i]);
			if (subMethod != null){
				methods.add(subMethod);
			}
		}
		return methods;
	}
		
	private boolean classesDeclareOverridingNativeMethod(IType[] classes) throws JavaModelException{
		for (int i= 0; i < classes.length; i++){
			IMethod[] methods= classes[i].getMethods();
			for (int j= 0; j < methods.length; j++){
				if ((!methods[j].equals(getMethod()))
					&& (Flags.isNative(methods[j].getFlags()))
					&& (null != findMethod(getMethod(), new IMethod[]{methods[j]})))
						return true;
			}
		}
		return false;
	}
}