/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.base.FileContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.util.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * This class defines a set of reusable static checks methods.
 */
public class Checks {
	
	/*
	 * no instances
	 */
	private Checks(){
	}

	/**
	 * Checks if method will have a constructor name after renaming.
	 * 
	 * @return <code>RefactoringStatus</code> with <code>WARNING</code> severity if 
	 * the give method will have a constructor name after renaming
	 * <code>null</code> otherwise.
	 */
	public static RefactoringStatus checkIfConstructorName(IMethod method, String newMethodName, String newTypeName){
		if (! newMethodName.equals(newTypeName))
			return null;
		else
			return RefactoringStatus.createWarningStatus("If you proceed, then the method " 
									+ method.getElementName()
									+ " in " 
									+ method.getDeclaringType().getFullyQualifiedName()
									+ " will have a constructor name.");	
	}
		
	/**
	 * Checks if the given name is a valid Java field name.
	 *
	 * @param the java field name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java field name.
	 */
	public static RefactoringStatus checkFieldName(String name) {
		return checkName(name, JavaConventions.validateFieldName(name));
	}
	
	/**
	 * Checks if the given name is a valid Java method name.
	 *
	 * @param the java method name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java method name.
	 */
	public static RefactoringStatus checkMethodName(String name) {
		return checkName(name, JavaConventions.validateMethodName(name));
	}
		
	/**
	 * Checks if the given name is a valid Java type name.
	 *
	 * @param the java method name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java type name.
	 */
	public static RefactoringStatus checkTypeName(String name) {
		//fix for: 1GF5Z0Z: ITPJUI:WINNT - assertion failed after renameType refactoring
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("Checks.no_dot"));//$NON-NLS-1$
		else	
			return checkName(name, JavaConventions.validateJavaTypeName(name));
	}
	
	/**
	 * Checks if the given name is a valid Java package name.
	 *
	 * @param the java package name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java package name.
	 */
	public static RefactoringStatus checkPackageName(String name) {
		RefactoringStatus result= checkName(name, JavaConventions.validatePackageName(name));
		if (! result.isOK())
			return result;
		//if name == null or "", then JavaConventions.validatePackageName reports an error anyway
		//so this call is safe
		if (! Character.isLowerCase(name.charAt(0))) 
			result.addWarning(RefactoringCoreMessages.getString("Checks.should_start_lowercase")); //$NON-NLS-1$
		return result;	
	}
	
	/**
	 * Checks if the given name is a valid compilation unit name.
	 *
	 * @param the compilation unit name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java package name.
	 */
	public static RefactoringStatus checkCompilationUnitName(String name) {
		if (hasTwoDots(name)) //$NON-NLS-2$ //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("Checks.no_two_dots"));//$NON-NLS-1$
		else	
			return checkName(name, JavaConventions.validateCompilationUnitName(name));
	}

	private static boolean hasTwoDots(String name) {
		return name.indexOf(".") != name.lastIndexOf(".");
	}
	
	/**
	 * Returns ok status if the new name is ok ie. no other file with that name exists.
	 * @param newName just a simple name - no extension.
	 */
	public static RefactoringStatus checkCompilationUnitNewName(ICompilationUnit cu, String newName) throws JavaModelException{
		if (resourceExists(RenameResourceChange.renamedResourcePath(Refactoring.getResource(cu).getFullPath(), newName)))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("Checks.cu_name_used", newName));//$NON-NLS-1$
		else
			return new RefactoringStatus();
	}
		
	public static boolean startsWithLowerCase(String s){
		if (s == null)
			return false;
		else if ("".equals(s)) //$NON-NLS-1$
			return false;
		else
			return (Character.isLowerCase(s.charAt(0)));		
	}

	public static boolean resourceExists(IPath resourcePath){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) != null;
	}
	
	/**
	 * Returns <code>true</code> if the parameter is a top-level type, <code>false</code> otherwise.
	 * 
	 * @param type
	 * @return <code>true</code> if the parameter is a top-level type, <code>false</code> otherwise.
	 */
	public static boolean isTopLevel(IType type){
		return type.getDeclaringType() == null;
	}
	
	/**
	 * Analyzes resources for availability: i.e. existence and being not read-only
	 * @param List groupedResults list of lists of <code>SearchResults</code> (grouped by resource 
	 * - as returned by <code>RefactoringSearchEngine#search</code>)
	 */
	public static RefactoringStatus checkAffectedResourcesAvailability(SearchResultGroup[] groupedResults) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < groupedResults.length; i++){
			IResource resource= groupedResults[i].getResource();
			if (!resource.isAccessible())
				result.addFatalError(RefactoringCoreMessages.getFormattedString("Checks.resource_not_accessible", resource.getFullPath())); //$NON-NLS-1$
			if (resource.isReadOnly())
				result.addFatalError(RefactoringCoreMessages.getFormattedString("Checks.resource_read_only", resource.getFullPath()), FileContext.create(resource, null)); //$NON-NLS-1$
		}
		return result;
	}	

	public static boolean isAlreadyNamed(IJavaElement element, String name){
		return name.equals(element.getElementName());
	}

	//-------------- native method checks ------------------
	public static RefactoringStatus checkForNativeMethods(IType type) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkForNativeMethods(type.getMethods()));
		result.merge(checkForNativeMethods(type.getTypes()));
		return result;
	}
	
	/* non java-doc
	 * checks all hierarchy of nested types
	 */
	public static RefactoringStatus checkForNativeMethods(IType[] types) throws JavaModelException {
		if (types == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < types.length; i++)
			result.merge(checkForNativeMethods(types[i]));
		return result;
	}
	
	public static RefactoringStatus checkForNativeMethods(ICompilationUnit cu) throws JavaModelException {
		return checkForNativeMethods(cu.getTypes());
	}
	
	private static RefactoringStatus checkForNativeMethods(IMethod[] methods) throws JavaModelException {
		if (methods == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < methods.length; i++) {
			if (Flags.isNative(methods[i].getFlags())){
				String msg= RefactoringCoreMessages.getFormattedString("Checks.method_native",  //$NON-NLS-1$
								new String[]{methods[i].getDeclaringType().getFullyQualifiedName(), methods[i].getElementName()})
								+ " UnsatisfiedLinkError."; //$NON-NLS-1$
				result.addError(msg, JavaSourceContext.create(methods[i])); 
			}				
		}
		return result;
	}
	
	//---- New method name checking -------------------------------------------------------------
	
	/**
	 * Checks if the new method is already used in the given type.
	 */
	public static RefactoringStatus checkMethodInType(ReferenceBinding type, String methodName, TypeBinding[] parameters, IJavaProject scope) {
		RefactoringStatus result= new RefactoringStatus();
		if (methodName.equals(new String(type.sourceName())))
			result.addWarning("New method name has constructor name");
		MethodBinding method= Bindings.findMethodInType(type, methodName.toCharArray(), parameters);
		if (method != null) 
			result.addError(
				"Method " + methodName + " already exists in " + new String(type.sourceName), 
				JavaSourceContext.create(method, scope));
		return result;
	}
	
	/**
	 * Checks if the new method somehow conflicts with an already existing method in
	 * the hierarchy. The following checks are done:
	 * <ul>
	 *   <li> if the new method overrides a method defined in the given type or in one of its
	 * 		super classes. </li>
	 * </ul>
	 */
	public static RefactoringStatus checkMethodInHierarchy(ReferenceBinding type, String methodName, TypeBinding[] parameters, IJavaProject scope) {
		RefactoringStatus result= new RefactoringStatus();
		MethodBinding method= Bindings.findMethodInHierarchy(type, methodName.toCharArray(), parameters);
		if (method != null) {
			ReferenceBinding dc= method.declaringClass;
			result.addError(
				"New method " + methodName + " overrides existing method in " + new String(dc.sourceName),
				JavaSourceContext.create(method, scope));
		}
		return result;
	}
	
	//-------------- main method checks ------------------
		
	public static RefactoringStatus checkForMainMethod(IType type) throws JavaModelException{
		/*
		 * for simplicity we ignore type access modifiers and report all public static void methods
		 */
		RefactoringStatus result= new RefactoringStatus();
		if (JavaModelUtil.hasMainMethod(type))
			result.addWarning(RefactoringCoreMessages.getFormattedString("Checks.has_main", type.getFullyQualifiedName())); //$NON-NLS-1$
		result.merge(checkForMainMethods(type.getTypes()));	
		return result;
	}
	
	public static RefactoringStatus checkForMainMethods(IType[] types) throws JavaModelException{
		if (types == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < types.length; i++) 
			result.merge(checkForMainMethod(types[i]));
		return result;
	}
	
	public static RefactoringStatus checkForMainMethods(ICompilationUnit cu) throws JavaModelException{
		return checkForMainMethods(cu.getTypes());
	}
	
	//---- Private helpers ----------------------------------------------------------------------
	
	private static RefactoringStatus checkName(String name, IStatus status) {
		RefactoringStatus result= new RefactoringStatus();
		if ("".equals(name)) //$NON-NLS-1$
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getString("Checks.Choose_name")); //$NON-NLS-1$

		if (status.isOK())
			return result;
		
		switch (status.getSeverity()){
			case IStatus.ERROR: 
				return RefactoringStatus.createFatalErrorStatus(status.getMessage());
			case IStatus.WARNING: 
				return RefactoringStatus.createWarningStatus(status.getMessage());
			case IStatus.INFO:
				return RefactoringStatus.createInfoStatus(status.getMessage());
			default: //no nothing
				return new RefactoringStatus();
		}
	}
	
	/**
	 * Finds a method in a type
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	public static IMethod findMethod(String name, int parameterCount, boolean isConstructor, IType type) throws JavaModelException {
		return findMethod(name, parameterCount, isConstructor, type.getMethods());
	}
	
	/**
	 * Finds a method in a type
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	public static IMethod findMethod(IMethod method, IType type) throws JavaModelException {
		return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), type.getMethods());
	}
	
	/**
	 * Finds a method in an array of methods
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	public static IMethod findMethod(IMethod method, IMethod[] methods) throws JavaModelException {
		return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), methods);
	}
	
	public static IMethod findMethod(String name, int parameters, boolean isConstructor, IMethod[] methods) throws JavaModelException {	
		for (int i= methods.length-1; i >= 0; i--) {
			IMethod curr= methods[i];
			if (name.equals(curr.getElementName())) {
				if (isConstructor == curr.isConstructor()) {
					if (parameters == curr.getParameterTypes().length) {
						return curr;
					}
				}
			}
		}
		return null;
	}
	
	/*
	 * Compare two parameter signatures
	 */
	public static boolean compareParamTypes(String[] paramTypes1, String[] paramTypes2) {
		if (paramTypes1.length == paramTypes2.length) {
			int i= 0;
			while (i < paramTypes1.length) {
				String t1= Signature.getSimpleName(Signature.toString(paramTypes1[i]));
				String t2= Signature.getSimpleName(Signature.toString(paramTypes2[i]));
				if (!t1.equals(t2)) {
					return false;
				}
				i++;
			}
			return true;
		}
		return false;
	}
	
	//---------------------
	
	public static RefactoringStatus checkIfCuBroken(IMember member) throws JavaModelException{
		ICompilationUnit cu= (ICompilationUnit)JavaCore.create(Refactoring.getResource(member));
		if (cu == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("Checks.cu_not_created"));	 //$NON-NLS-1$
		else if (! cu.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("Checks.cu_not_parsed"));	 //$NON-NLS-1$
		return new RefactoringStatus();
	}
	
	/**
	 * From SearchResultGroup[] passed as the parameter
	 * this method removes all those that correspond to a non-parsable ICompilationUnit
	 * and returns it as a result.
	 * Status object collect the result of checking.
	 */	
	public static SearchResultGroup[] excludeCompilationUnits(SearchResultGroup[] grouped, RefactoringStatus status) throws JavaModelException{
		List result= new ArrayList();
		boolean wasEmpty= grouped.length == 0;
		for (int i= 0; i < grouped.length; i++){	
			IResource resource= grouped[i].getResource();
			IJavaElement element= JavaCore.create(resource);
			if (! (element instanceof ICompilationUnit))
				continue;
			//XXX this is a workaround 	for a jcore feature that shows errors in cus only when you get the original element
			ICompilationUnit cu= (ICompilationUnit)JavaCore.create(resource);
			if (! cu.isStructureKnown()){
				String path= AbstractRefactoringASTAnalyzer.getFullPath(cu);
				status.addError(RefactoringCoreMessages.getFormattedString("Checks.cannot_be_parsed", path)); //$NON-NLS-1$
				continue; //removed, go to the next one
			}
			result.add(grouped[i]);	
		}
		
		if ((!wasEmpty) && result.isEmpty())
			status.addFatalError(RefactoringCoreMessages.getString("Checks.all_excluded")); //$NON-NLS-1$
		
		return (SearchResultGroup[])result.toArray(new SearchResultGroup[result.size()]);
	}
	
	public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] grouped) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < grouped.length; i++){
			IResource resource= grouped[i].getResource();
			if (hasCompileErrors(resource))
				result.addWarning("Affected resource:" + resource.getFullPath() + " has compile errors, which can affect accuracy of the code modification..");
		}
		return result;
	}
	
	private static boolean hasCompileErrors(IResource resource) throws JavaModelException {
		try {
			IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			for (int i= 0; i < problemMarkers.length; i++) {
				if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
					return true;
			}
			return false;
		
		} catch (CoreException e){
			throw new JavaModelException(e);
		}
	}
	
	//------
	public static boolean isReadOnly(Object element) throws JavaModelException{
		if (element instanceof IResource)
			return isReadOnly((IResource)element);
		
		if (element instanceof IJavaElement) {
			if ((element instanceof IPackageFragmentRoot) && isClasspathDelete((IPackageFragmentRoot)element)) 
				return false;
			return isReadOnly(((IJavaElement)element).getCorrespondingResource());
		}
		
		Assert.isTrue(false, "not expected to get here");	
		return false;
	}
	
	public static boolean isReadOnly(IResource res) throws JavaModelException{
		if (res.isReadOnly()) 
			return true;
		
		if (! (res instanceof IContainer))	
			return false;
		
		IContainer container= (IContainer)res;
		try {
			IResource[] children= container.members();
			for (int i= 0; i < children.length; i++) {
				if (isReadOnly(children[i]))
					return true;
			}
			return false;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	public static boolean isClasspathDelete(IPackageFragmentRoot pkgRoot) throws JavaModelException {
		IResource res= pkgRoot.getUnderlyingResource();
		if (res == null)
			return true;
		IProject definingProject= res.getProject();
		IProject occurringProject= pkgRoot.getJavaProject().getProject();
		return !definingProject.equals(occurringProject);
	}
	
	public static  RefactoringStatus checkCompileErrors(AST ast, ICompilationUnit cu) {
		IProblem[] problems= ast.getProblems();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (! problem.isWarning()){
				String message= "Compilation error in line: " + problem.getSourceLineNumber() 
										+ " : " + problem.getMessage();
				int start= problem.getSourceStart();
				int length= problem.getSourceEnd() - start + 1;
				Context context= JavaSourceContext.create(cu, new SourceRange(start, length));
				result.addFatalError(message, context);
			}		
		}
		return result;
	}
	
}