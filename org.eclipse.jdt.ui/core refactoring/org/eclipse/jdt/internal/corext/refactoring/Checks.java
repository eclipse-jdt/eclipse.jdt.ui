/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * This class defines a set of reusable static checks methods.
 */
public class Checks {
	
	/*
	 * no instances
	 */
	private Checks(){
	}
	
	/* Constants returned by checkExpressionIsRValue */
	public static final int IS_RVALUE= 0;
	public static final int NOT_RVALUE_MISC= 1;
	public static final int NOT_RVALUE_VOID= 2;

	/**
	 * Checks if method will have a constructor name after renaming.
	 * @param method
	 * @param newMethodName
	 * @param newTypeName 
	 * @return <code>RefactoringStatus</code> with <code>WARNING</code> severity if 
	 * the give method will have a constructor name after renaming
	 * <code>null</code> otherwise.
	 */
	public static RefactoringStatus checkIfConstructorName(IMethod method, String newMethodName, String newTypeName){
		if (! newMethodName.equals(newTypeName))
			return null;
		else
			return RefactoringStatus.createWarningStatus(
				RefactoringCoreMessages.getFormattedString("Checks.constructor_name",  //$NON-NLS-1$
				new Object[] {JavaElementUtil.createMethodSignature(method), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType()) } ));
	}
		
	/**
	 * Checks if the given name is a valid Java field name.
	 *
	 * @param name the java field name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java field name.
	 */
	public static RefactoringStatus checkFieldName(String name) {
		return checkName(name, JavaConventions.validateFieldName(name));
	}

	/**
	 * Checks if the given name is a valid Java type parameter name.
	 *
	 * @param name the java type parameter name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java type parameter name.
	 */
	public static RefactoringStatus checkTypeParameterName(String name) {
		// TODO use method from JavaConventions (see 73535)
		return checkName(name, JavaConventions.validateIdentifier(name));
	}

	/**
	 * Checks if the given name is a valid Java identifier.
	 *
	 * @param name the java identifier.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java identifier.
	 */
	public static RefactoringStatus checkIdentifier(String name) {
		return checkName(name, JavaConventions.validateIdentifier(name));
	}
	
	/**
	 * Checks if the given name is a valid Java method name.
	 *
	 * @param name the java method name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java method name.
	 */
	public static RefactoringStatus checkMethodName(String name) {
		RefactoringStatus status= checkName(name, JavaConventions.validateMethodName(name));
		if (status.isOK() && startsWithUpperCase(name))
			return RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getString("Checks.method_names_lowercase")); //$NON-NLS-1$
		else	
			return status;
	}
		
	/**
	 * Checks if the given name is a valid Java type name.
	 *
	 * @param name the java method name.
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
	 * @param name the java package name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid java package name.
	 */
	public static RefactoringStatus checkPackageName(String name) {
		return checkName(name, JavaConventions.validatePackageName(name));
	}
	
	/**
	 * Checks if the given name is a valid compilation unit name.
	 *
	 * @param name the compilation unit name.
	 * @return a refactoring status containing the error message if the
	 *  name is not a valid compilation unit name.
	 */
	public static RefactoringStatus checkCompilationUnitName(String name) {
		return checkName(name, JavaConventions.validateCompilationUnitName(name));
	}

	/**
	 * Returns ok status if the new name is ok. This is when no other file with that name exists.
	 * @param cu
	 * @param newName 
	 * @return the status
	 */
	public static RefactoringStatus checkCompilationUnitNewName(ICompilationUnit cu, String newName) {
		if (resourceExists(RenameResourceChange.renamedResourcePath(ResourceUtil.getResource(cu).getFullPath(), newName + ".java"))) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("Checks.cu_name_used", newName));//$NON-NLS-1$
		else
			return new RefactoringStatus();
	}
		
	private static boolean startsWithUpperCase(String s) {
		if (s == null)
			return false;
		else if ("".equals(s)) //$NON-NLS-1$
			return false;
		else
			//workaround for JDK bug (see 26529)
			return s.charAt(0) == Character.toUpperCase(s.charAt(0));
	}
		
	public static boolean startsWithLowerCase(String s){
		if (s == null)
			return false;
		else if ("".equals(s)) //$NON-NLS-1$
			return false;
		else
			//workaround for JDK bug (see 26529)
			return s.charAt(0) == Character.toLowerCase(s.charAt(0));
	}

	public static boolean resourceExists(IPath resourcePath){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath) != null;
	}
	
	public static boolean isTopLevel(IType type){
		return type.getDeclaringType() == null;
	}

	public static boolean isTopLevelType(IMember member){
		return  member.getElementType() == IJavaElement.TYPE && isTopLevel((IType) member);
	}
	
	public static boolean isInsideLocalType(IType type) throws JavaModelException {
		while (type != null) {
			if (type.isLocal())
				return true;
			type= type.getDeclaringType();
		}
		return false;
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
			if (JdtFlags.isNative(methods[i])){
				String msg= RefactoringCoreMessages.getFormattedString("Checks.method_native",  //$NON-NLS-1$
								new String[]{JavaModelUtil.getFullyQualifiedName(methods[i].getDeclaringType()), methods[i].getElementName(), "UnsatisfiedLinkError"});//$NON-NLS-1$
				result.addError(msg, JavaStatusContext.create(methods[i])); 
			}				
		}
		return result;
	}
	
	//---- New method name checking -------------------------------------------------------------
	
	/**
	 * Checks if the new method is already used in the given type.
	 * @param type
	 * @param methodName
	 * @param parameters
	 * @param scope
	 * @return the status
	 */
	public static RefactoringStatus checkMethodInType(ITypeBinding type, String methodName, ITypeBinding[] parameters, IJavaProject scope) {
		RefactoringStatus result= new RefactoringStatus();
		if (methodName.equals(type.getName()))
			result.addWarning(RefactoringCoreMessages.getString("Checks.methodName.constructor")); //$NON-NLS-1$
		IMethodBinding method= org.eclipse.jdt.internal.corext.dom.Bindings.findMethodInType(type, methodName, parameters);
		if (method != null) 
			result.addError(RefactoringCoreMessages.getFormattedString("Checks.methodName.exists",  //$NON-NLS-1$
				new Object[] {methodName, type.getName()}),
				JavaStatusContext.create(method, scope));
		return result;
	}
	
	/**
	 * Checks if the new method somehow conflicts with an already existing method in
	 * the hierarchy. The following checks are done:
	 * <ul>
	 *   <li> if the new method overrides a method defined in the given type or in one of its
	 * 		super classes. </li>
	 * </ul>
	 * @param type
	 * @param methodName
	 * @param returnType
	 * @param parameters
	 * @param scope
	 * @return the status
	 */
	public static RefactoringStatus checkMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding returnType, ITypeBinding[] parameters, IJavaProject scope) {
		RefactoringStatus result= new RefactoringStatus();
		IMethodBinding method= Bindings.findMethodInHierarchy(type, methodName, parameters);
		if (method != null) {
			boolean returnTypeClash= false;
			ITypeBinding methodReturnType= method.getReturnType();
			if (returnType != null && methodReturnType != null) {
				String returnTypeKey= returnType.getKey();
				String methodReturnTypeKey= methodReturnType.getKey();
				if (returnTypeKey == null && methodReturnTypeKey == null) {
					returnTypeClash= returnType != methodReturnType;	
				} else if (returnTypeKey != null && methodReturnTypeKey != null) {
					returnTypeClash= !returnTypeKey.equals(methodReturnTypeKey);
				}
			}
			ITypeBinding dc= method.getDeclaringClass();
			if (returnTypeClash) {
				result.addError(RefactoringCoreMessages.getFormattedString("Checks.methodName.returnTypeClash", //$NON-NLS-1$
					new Object[] {methodName, dc.getName()}),
					JavaStatusContext.create(method, scope));
			} else {
				result.addError(RefactoringCoreMessages.getFormattedString("Checks.methodName.overrides", //$NON-NLS-1$
					new Object[] {methodName, dc.getName()}),
					JavaStatusContext.create(method, scope));
			}
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
			result.addWarning(RefactoringCoreMessages.getFormattedString("Checks.has_main", JavaModelUtil.getFullyQualifiedName(type))); //$NON-NLS-1$
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
	
	//---- Selection checks --------------------------------------------------------------------
	
	public static boolean isExtractableExpression(ASTNode[] selectedNodes, ASTNode coveringNode) {
		ASTNode node= coveringNode;
		if (selectedNodes != null && selectedNodes.length == 1)
			node= selectedNodes[0];
		return isExtractableExpression(node);
	}
	
	public static boolean isExtractableExpression(ASTNode node) {
		if (! (node instanceof Expression))
			return false;
		if (node instanceof Name) {
			IBinding binding= ((Name)node).resolveBinding();
			return ! (binding instanceof ITypeBinding);
		}
		return true;
	}
	
	public static boolean isInsideJavadoc(ASTNode node) {
		do {
			if (node.getNodeType() == ASTNode.JAVADOC)
				return true;
			node= node.getParent();
		} while (node != null);
		return false;
	}

	//---- Private helpers ----------------------------------------------------------------------
	
	private static RefactoringStatus checkName(String name, IStatus status) {
		RefactoringStatus result= new RefactoringStatus();
		if ("".equals(name)) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("Checks.Choose_name")); //$NON-NLS-1$

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
	 * @param name
	 * @param parameterCount
	 * @param isConstructor
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaModelException
	 */
	public static IMethod findMethod(String name, int parameterCount, boolean isConstructor, IType type) throws JavaModelException {
		return findMethod(name, parameterCount, isConstructor, type.getMethods());
	}
	
	/**
	 * Finds a method in a type.
	 * Searches for a method with the same name and the same parameter count.
	 * Parameter types are <b>not</b> compared.
	 * @param method
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaModelException
	 */
	public static IMethod findMethod(IMethod method, IType type) throws JavaModelException {
		return findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), type.getMethods());
	}

	/**
	 * Finds a method in an array of methods.
	 * Searches for a method with the same name and the same parameter count.
	 * Parameter types are <b>not</b> compared.
	 * @param method
	 * @param methods
	 * @return The first found method or null, if nothing found
	 * @throws JavaModelException
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

	/**
	 * Finds a method in a type.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @param method
	 * @param type
	 * @return The first found method or null, if nothing found
	 * @throws JavaModelException
	 */
	public static IMethod findSimilarMethod(IMethod method, IType type) throws JavaModelException {
		return findSimilarMethod(method, type.getMethods());
	}

	/**
	 * Finds a method in an array of methods.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @param method
	 * @param methods
	 * @return The first found method or null, if nothing found
	 * @throws JavaModelException
	 */
	public static IMethod findSimilarMethod(IMethod method, IMethod[] methods) throws JavaModelException {
		boolean isConstructor= method.isConstructor();
		for (int i= 0; i < methods.length; i++) {
			IMethod otherMethod= methods[i];
			if (otherMethod.isConstructor() == isConstructor && method.isSimilar(otherMethod))
				return otherMethod;
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
		ICompilationUnit cu= (ICompilationUnit)JavaCore.create(ResourceUtil.getResource(member));
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
	 * @param grouped the array of search result groups from which non parsable compilation
	 *  units are to be removed.
	 * @param status a refactoring status to collect errors and problems
	 * @return the array of search result groups 
	 * @throws JavaModelException
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
				String path= Checks.getFullPath(cu);
				status.addError(RefactoringCoreMessages.getFormattedString("Checks.cannot_be_parsed", path)); //$NON-NLS-1$
				continue; //removed, go to the next one
			}
			result.add(grouped[i]);	
		}
		
		if ((!wasEmpty) && result.isEmpty())
			status.addFatalError(RefactoringCoreMessages.getString("Checks.all_excluded")); //$NON-NLS-1$
		
		return (SearchResultGroup[])result.toArray(new SearchResultGroup[result.size()]);
	}
	
	private static final String getFullPath(ICompilationUnit cu) {
		Assert.isTrue(cu.exists());
		return ResourceUtil.getResource(cu).getFullPath().toString();
	}
	
	
	public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] grouped) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < grouped.length; i++)
			checkCompileErrorsInAffectedFile(result, grouped[i].getResource());
		return result;
	}
	
	public static void checkCompileErrorsInAffectedFile(RefactoringStatus result, IResource resource) throws JavaModelException {
		if (hasCompileErrors(resource))
			result.addWarning(RefactoringCoreMessages.getFormattedString("Checks.cu_has_compile_errors", resource.getFullPath().makeRelative())); //$NON-NLS-1$
	}
	
	public static RefactoringStatus checkCompileErrorsInAffectedFiles(SearchResultGroup[] references, IResource declaring) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < references.length; i++){
			IResource resource= references[i].getResource();
			if (resource.equals(declaring))
				declaring= null;
			checkCompileErrorsInAffectedFile(result, resource);
		}
		if (declaring != null)
			checkCompileErrorsInAffectedFile(result, declaring);
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
		} catch (JavaModelException e){
			throw e;		
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
			return isReadOnly(((IJavaElement)element).getResource());
		}
		
		Assert.isTrue(false, "not expected to get here");	 //$NON-NLS-1$
		return false;
	}
	
	public static boolean isReadOnly(IResource res) throws JavaModelException {
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
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	public static boolean isClasspathDelete(IPackageFragmentRoot pkgRoot) {
		IResource res= pkgRoot.getResource();
		if (res == null)
			return true;
		IProject definingProject= res.getProject();
		if (res.getParent() != null && pkgRoot.isArchive() && ! res.getParent().equals(definingProject))
			return true;
		
		IProject occurringProject= pkgRoot.getJavaProject().getProject();
		return !definingProject.equals(occurringProject);
	}
	
	//-------- validateEdit checks ----
	
	public static RefactoringStatus validateModifiesFiles(IFile[] filesToModify, Object context) {
		RefactoringStatus result= new RefactoringStatus();
		IStatus status= Resources.checkInSync(filesToModify);
		if (!status.isOK())
			result.merge(RefactoringStatus.create(status));
		status= Resources.makeCommittable(filesToModify, context);
		if (!status.isOK()) {
			result.merge(RefactoringStatus.create(status));
			if (!result.hasFatalError()) {
				result.addFatalError(RefactoringCoreMessages.getString("Checks.validateEdit")); //$NON-NLS-1$
			}			
		}
		return result;
	}
	
	public static RefactoringStatus validateEdit(ICompilationUnit unit, Object context) {
		IResource resource= JavaModelUtil.toOriginal(unit).getResource();
		RefactoringStatus result= new RefactoringStatus();
		if (resource == null)
			return result;
		IStatus status= Resources.checkInSync(resource);
		if (!status.isOK())
			result.merge(RefactoringStatus.create(status));
		status= Resources.makeCommittable(resource, context);
		if (!status.isOK()) {
			result.merge(RefactoringStatus.create(status));
			if (!result.hasFatalError()) {
				result.addFatalError(RefactoringCoreMessages.getString("Checks.validateEdit")); //$NON-NLS-1$
			}			
		}
		return result;
	}	

	/**
	 * Checks whether it is possible to modify the given <code>IJavaElement</code>.
	 * The <code>IJavaElement</code> must exist and be non read-only to be modifiable.
	 * Moreover, if it is a <code>IMember</code> it must not be binary.
	 * The returned <code>RefactoringStatus</code> has <code>ERROR</code> severity if
	 * it is not possible to modify the element.
	 * @param javaElement
	 * @return the status
	 * @throws JavaModelException
	 *
	 * @see IJavaElement#exists
	 * @see IJavaElement#isReadOnly
	 * @see IMember#isBinary
	 * @see RefactoringStatus
	 */ 
	public static RefactoringStatus checkAvailability(IJavaElement javaElement) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! javaElement.exists())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.not_in_model", javaElement.getElementName())); //$NON-NLS-1$
		if (javaElement.isReadOnly())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.read_only", javaElement.getElementName()));	 //$NON-NLS-1$
		if (javaElement.exists() && !javaElement.isStructureKnown())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.unknown_structure", javaElement.getElementName()));	 //$NON-NLS-1$
		if (javaElement instanceof IMember && ((IMember)javaElement).isBinary())
			result.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.binary", javaElement.getElementName())); //$NON-NLS-1$
		return result;
	}
	
	public static boolean isAvailable(IJavaElement javaElement) throws JavaModelException {
		if (javaElement == null)
			return false;
		if (! javaElement.exists())
			return false;
		if (javaElement.isReadOnly())
			return false;
		// work around for https://bugs.eclipse.org/bugs/show_bug.cgi?id=48422
		// the Java project is now cheating regarding its children so we shouldn't
		// call isStructureKnown if the project isn't open.
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=52474
		if (!(javaElement instanceof IJavaProject) && !(javaElement instanceof ILocalVariable) && !javaElement.isStructureKnown())
			return false;
		if (javaElement instanceof IMember && ((IMember)javaElement).isBinary())
			return false;
		return true;
	}

	public static IType findTypeInPackage(IPackageFragment pack, String name) throws JavaModelException {
		Assert.isTrue(pack.exists());
		Assert.isTrue(!pack.isReadOnly());
		
		/* ICompilationUnit.getType expects simple name*/  
		if (name.indexOf(".") != -1) //$NON-NLS-1$
			name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
		ICompilationUnit[] cus= pack.getCompilationUnits();
		for (int i= 0; i < cus.length; i++){
			if (cus[i].getType(name).exists())
				return cus[i].getType(name);
		}
		return null;
	}

	public static RefactoringStatus checkTempName(String newName) {
		RefactoringStatus result= Checks.checkIdentifier(newName);
		if (result.hasFatalError())
			return result;
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning(RefactoringCoreMessages.getString("ExtractTempRefactoring.convention")); //$NON-NLS-1$
		return result;		
	}

	public static RefactoringStatus checkEnumConstantName(String newName) {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (result.hasFatalError())
			return result;
		for (int i= 0; i < newName.length(); i++) {
			char c= newName.charAt(i);
			if (Character.isLetter(c) && !Character.isUpperCase(c)) {
				result.addWarning(RefactoringCoreMessages.getString("RenameEnumConstRefactoring.convention")); //$NON-NLS-1$	
				break;
			}
		}
		return result;
	}

	public static RefactoringStatus checkConstantName(String newName) {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (result.hasFatalError())
			return result;
		for (int i= 0; i < newName.length(); i++) {
			char c= newName.charAt(i);
			if (Character.isLetter(c) && !Character.isUpperCase(c)) {
				result.addWarning(RefactoringCoreMessages.getString("ExtractConstantRefactoring.convention")); //$NON-NLS-1$	
				break;
			}
		}
		return result;
	}

	public static boolean isException(IType iType, IProgressMonitor pm) throws JavaModelException {
		try{
			if (iType.isInterface())
				return false;
			IType[] superTypes= iType.newSupertypeHierarchy(pm).getAllSupertypes(iType);
			for (int i= 0; i < superTypes.length; i++) {
				if ("java.lang.Throwable".equals(superTypes[i].getFullyQualifiedName())) //$NON-NLS-1$
					return true;
			}
			return false;
		} finally{
			pm.done();
		}	
	}
		
	/**
	 * @param e
	 * @return int
	 *          Checks.IS_RVALUE		if e is an rvalue
	 *          Checks.NOT_RVALUE_VOID  if e is not an rvalue because its type is void
	 *          Checks.NOT_RVALUE_MISC  if e is not an rvalue for some other reason
	 */
	public static int checkExpressionIsRValue(Expression e) {
		if(e instanceof Name) {
			if(!(((Name) e).resolveBinding() instanceof IVariableBinding)) {
				return NOT_RVALUE_MISC;
			}
		}
		
		ITypeBinding tb= e.resolveTypeBinding();
		if (tb == null)
			return NOT_RVALUE_MISC;
		else if (tb.getName().equals("void")) //$NON-NLS-1$
			return NOT_RVALUE_VOID;

		return IS_RVALUE;		
	}

	public static boolean isDeclaredIn(VariableDeclaration tempDeclaration, Class astNodeClass) {
		ASTNode initializer= ASTNodes.getParent(tempDeclaration, astNodeClass);
		if (initializer == null)
			return false;
		ASTNode anonymous= ASTNodes.getParent(tempDeclaration, AnonymousClassDeclaration.class);	
		if (anonymous == null)
			return true;
		// stupid code. Is to find out if the variable declaration isn't a field.
		if (ASTNodes.isParent(anonymous, initializer))
			return false;
		return true;	
	}
}
