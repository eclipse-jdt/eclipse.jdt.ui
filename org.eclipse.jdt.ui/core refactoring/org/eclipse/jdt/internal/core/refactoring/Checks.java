/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;

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
		return checkName(name, JavaConventions.validatePackageName(name));
	}
	
	/**
	 * Checks if the given name is a valid compilation unit name.
	 *
	 * @param the compilation unit name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java package name.
	 */
	public static RefactoringStatus checkCompilationUnitName(String name) {
		return checkName(name, JavaConventions.validateCompilationUnitName(name));
	}
	
	/**
	 * Returns <code>null</code> if the new name is ok ie. no other file with that name exists.
	 * @param newName just a simple name - no extension.
	 */
	public static RefactoringStatus checkCompilationUnitNewName(ICompilationUnit cu, String newName) throws JavaModelException{
		IPath newPath= RenameResourceChange.renamedResourcePath(Refactoring.getResource(cu).getFullPath(), newName);
		if (resourceExists(newPath)){
			RefactoringStatus result= new RefactoringStatus();	
			result.addFatalError("Cannot rename a compilation unit to \"" + newName + ".java\" - this name is already used by another file in this directory");
			return result;
		} else
			return null;
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
	public static RefactoringStatus checkAffectedResourcesAvailability(List groupedResults) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		Iterator iter= groupedResults.iterator();
		while (iter.hasNext()){
			List searchResults= (List)iter.next();
			IResource resource= ((SearchResult)searchResults.get(0)).getResource();
			if (!resource.isAccessible())
				result.addFatalError("Affected resource:" + resource.getFullPath() + " is not accesible");
			if (resource.isReadOnly())
				result.addFatalError("Affected resource:" + resource.getFullPath() + " is read-only");	
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
			if (Flags.isNative(methods[i].getFlags()))
				result.addError("Method " + methods[i].getDeclaringType().getFullyQualifiedName() 
							+ "::" + methods[i].getElementName() 
							+ " is native. Running the modified program can cause UnsatisfiedLinkError on runtime.");
		}
		return result;
	}
	
	//---- New method name checking -------------------------------------------------------------
	
	/**
	 * Checks if the new method is already used in the given type.
	 */
	public static RefactoringStatus checkMethodInType(IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (Character.isUpperCase(name.charAt(0))) {
			result.addWarning(getFormattedString("methodName.discouraged", name));
		}
		IMethod match= findMethod(name, paramTypes.length, false, type.getMethods());
		if (match != null) {
			result.addError(getFormattedString("methodName.exists", PrettySignature.getUnqualifiedMethodSignature(match)));
		}
		return result;
	}
	
	/**
	 * Checks if the new method somehow conflicts with an already existing method in
	 * the hierarchy. The following checks are done:
	 * <ul>
	 *   <li> if the new method overrides a method defined in a super class or super 
	 *        interface of the given type. </li>
	 *   <li> if the new method overloads an already existing method in a way that
	 *        an ambiguity occurs. </li>
	 * </ul>
	 */
	public static RefactoringStatus checkMethodInHierarchy(IProgressMonitor pm, IType type, String methodName, String[] paramTypes, boolean isConstructor, int flags) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (Flags.isPrivate(flags))
			return result;
		
		ITypeHierarchy hierarchy= type.newTypeHierarchy(pm);
		checkSuperHierarchy(result, hierarchy, type, methodName, paramTypes, isConstructor);
		if (!result.isOK())
			return result;
		checkSubHierarchy(result, hierarchy, type, methodName, paramTypes, isConstructor);
		return result;
	}
	
	private static void checkSuperHierarchy(RefactoringStatus status, ITypeHierarchy hierarchy, IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		IType[] superTypes= hierarchy.getSupertypes(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod match= findMethod(name, paramTypes.length, false, superTypes[i].getMethods());
			if (match != null) {
				status.addError(makeHierarchyStatusMessage(match));
				return;
			}
			checkSuperHierarchy(status, hierarchy, superTypes[i], name, paramTypes, isConstructor);
			if (!status.isOK())
				return;
		}
	}
	
	private static void checkSubHierarchy(RefactoringStatus status, ITypeHierarchy hierarchy, IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		IType[] subTypes= hierarchy.getSubtypes(type);
		for (int i= 0; i < subTypes.length; i++) {
			IMethod match= findMethod(name,  paramTypes.length, isConstructor, subTypes[i].getMethods());
			if (match != null) {
				status.addError(makeHierarchyStatusMessage(match));
				return;
			}
			checkSubHierarchy(status, hierarchy, subTypes[i], name, paramTypes, isConstructor);
			if (!status.isOK())
				return;
		}		
	}
	
	private static String makeHierarchyStatusMessage(IMethod method) {
		return getFormattedString("methodInHierarchy.exists", PrettySignature.getMethodSignature(method));
	}
	
	//-------------- main method checks ------------------
		
	public static RefactoringStatus checkForMainMethod(IType type) throws JavaModelException{
		/*
		 * for simplicity we ignore type access modifiers and report all public static void methods
		 */
		RefactoringStatus result= new RefactoringStatus();
		if (JavaModelUtility.hasMainMethod(type))
			result.addWarning("Type " + type.getFullyQualifiedName() + " has a main method - refactoring might cause some applications (scripts etc.) to not work");
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
		if ("".equals(name)){
			result.addFatalError("Choose a name");
			return result;
		}	
		if (! status.isOK()) {
			switch (status.getSeverity()){
				case IStatus.ERROR: 
					result.addFatalError(status.getMessage());
					break;
				case IStatus.WARNING: 
					result.addWarning(status.getMessage());
					break;
				case IStatus.INFO:
					result.addInfo(status.getMessage());
					break;	
				default: //no nothing
					break;
			}
		}
		return result;		
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
	
	private static String getFormattedString(String key, String arg) {
		return Resources.getFormattedString("Checks." + key, arg);
	}
	
	private static String getFormattedString(String key, String[] args) {
		return Resources.getFormattedString("Checks." + key, args);
	}	
}