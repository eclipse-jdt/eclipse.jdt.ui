/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameVirtualMethodRefactoring extends RenameMethodRefactoring {
	
	public RenameVirtualMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
	}
	
	//------------ Conditions -------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
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
		result.merge(analyzeCompilationUnits(new SubProgressMonitor(pm, 3)));	
		pm.done();
		return result;
	}
		
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
	
	private RefactoringStatus analyzeCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		List grouped= getOccurrences(pm);
		if (grouped.isEmpty())
			return null;
			
		RefactoringStatus result= Checks.excludeCompilationUnits(grouped, getUnsavedFileList());
		if (result.hasFatalError())
			return result;
			
		RenameMethodASTAnalyzer analyzer= new RenameMethodASTAnalyzer(getNewName(), getMethod());
		for (Iterator iter= grouped.iterator(); iter.hasNext(); ){	
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			List searchResults= (List)iter.next();
			ICompilationUnit cu= (ICompilationUnit)JavaCore.create(((SearchResult)searchResults.get(0)).getResource());
			pm.subTask(RefactoringCoreMessages.getFormattedString("RenameVirtualMethodRefactoring.analyzing", cu.getElementName())); //$NON-NLS-1$
			result.merge(analyzer.analyze(searchResults, cu));
		}
		return result;
	}
	
	private boolean isDeclaredInInterface(IProgressMonitor pm) throws JavaModelException {
		ITypeHierarchy hier= getMethod().getDeclaringType().newTypeHierarchy(pm);
		IType[] classes= hier.getAllClasses();
		for (int i= 0; i < classes.length; i++) {
			IType[] superinterfaces= hier.getAllSuperInterfaces(classes[i]);
			for (int j= 0; j < superinterfaces.length; j++) {
				if (findMethod(getMethod(), superinterfaces[j]) != null)
						return true;
			}
		}
		return false;
	}
	
	private boolean overridesAnotherMethod(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
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
		IMethod found= findInHierarchy(getMethod().getDeclaringType().newSupertypeHierarchy(pm), getMethod());
		return (found != null && (! Flags.isStatic(found.getFlags())) && (! Flags.isPrivate(found.getFlags())));	
	}
	
	private boolean hierarchyDeclaresSimilarNativeMethod(IProgressMonitor pm) throws JavaModelException{
		IType[] classes= getMethod().getDeclaringType().newTypeHierarchy(pm).getAllSubtypes(getMethod().getDeclaringType());
		return classesDeclareOverridingNativeMethod(classes);
	}

	
	//---------------- Changes ----------------
	
	/**
	 * RenameMethodRefactoring#methodsToRename
	 */
	/*package*/ HashSet getMethodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException {
		HashSet methods= new HashSet();
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

}