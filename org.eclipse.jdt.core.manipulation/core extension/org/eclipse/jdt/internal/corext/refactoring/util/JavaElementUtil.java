/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


/**
 * Utility methods for the Java Model.
 *
 * @see JavaModelUtil (a better place for new methods)
 * see JDTUIHelperClasses
 */
public class JavaElementUtil {

	//no instances
	private JavaElementUtil(){
	}

	public static String createMethodSignature(IMethod method){
		try {
			return BasicElementLabels.getJavaElementName(Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), false, ! method.isConstructor()));
		} catch(JavaModelException e) {
			return BasicElementLabels.getJavaElementName(method.getElementName()); //fallback
		}
	}

	public static String createFieldSignature(IField field){
		return BasicElementLabels.getJavaElementName(field.getDeclaringType().getFullyQualifiedName('.') + "." + field.getElementName()); //$NON-NLS-1$
	}

	public static String createSignature(IMember member){
		switch (member.getElementType()){
			case IJavaElement.FIELD:
				return createFieldSignature((IField)member);
			case IJavaElement.TYPE:
				return BasicElementLabels.getJavaElementName(((IType)member).getFullyQualifiedName('.'));
			case IJavaElement.INITIALIZER:
				return RefactoringCoreMessages.JavaElementUtil_initializer;
			case IJavaElement.METHOD:
				return createMethodSignature((IMethod)member);
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	public static IJavaElement[] getElementsOfType(IJavaElement[] elements, int type){
		Set<IJavaElement> result= new HashSet<>(elements.length);
		for (IJavaElement element : elements) {
			if (element.getElementType() == type)
				result.add(element);
		}
		return result.toArray(new IJavaElement[result.size()]);
	}

	public static IType getMainType(ICompilationUnit cu) throws JavaModelException{
		for (IType type : cu.getTypes()) {
			if (isMainType(type)) {
				return type;
			}
		}
		return null;
	}

	public static boolean isMainType(IType type) throws JavaModelException{
		if (! type.exists())
			return false;

		if (type.isBinary())
			return false;

		if (type.getCompilationUnit() == null)
			return false;

		if (type.getDeclaringType() != null)
			return false;

		return isPrimaryType(type) || isCuOnlyType(type);
	}


	private static boolean isPrimaryType(IType type){
		return type.equals(type.getCompilationUnit().findPrimaryType());
	}


	private static boolean isCuOnlyType(IType type) throws JavaModelException{
		return type.getCompilationUnit().getTypes().length == 1;
	}

	/* @see org.eclipse.jdt.internal.core.JavaElement#isAncestorOf(org.eclipse.jdt.core.IJavaElement) */
	public static boolean isAncestorOf(IJavaElement ancestor, IJavaElement child) {
		IJavaElement parent= child.getParent();
		while (parent != null && !parent.equals(ancestor)) {
			parent= parent.getParent();
		}
		return parent != null;
	}

	public static IMethod[] getAllConstructors(IType type) throws JavaModelException {
		if (JavaModelUtil.isInterfaceOrAnnotation(type))
			return new IMethod[0];
		List<IMethod> result= new ArrayList<>();
		for (IMethod iMethod : type.getMethods()) {
			if (iMethod.isConstructor())
				result.add(iMethod);
		}
		return result.toArray(new IMethod[result.size()]);
	}

	/**
	 * @param root the package fragment root
	 * @return array of projects that have the specified root on their classpath
	 * @throws JavaModelException if getting the raw classpath or all Java projects fails
	 */
	public static IJavaProject[] getReferencingProjects(IPackageFragmentRoot root) throws JavaModelException {
		IClasspathEntry cpe= root.getRawClasspathEntry();
		if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
			cpe= root.getResolvedClasspathEntry();
		IJavaProject[] allJavaProjects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
		List<IJavaProject> result= new ArrayList<>(allJavaProjects.length);
		for (IJavaProject project : allJavaProjects) {
			IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(cpe);
			if (roots.length > 0)
				result.add(project);
		}
		return result.toArray(new IJavaProject[result.size()]);
	}

	public static IMember[] merge(IMember[] a1, IMember[] a2) {
		// Don't use hash sets since ordering is important for some refactorings.
		List<IMember> result= new ArrayList<>(a1.length + a2.length);
		for (IMember member : a1) {
			if (!result.contains(member))
				result.add(member);
		}
		for (IMember member : a2) {
			if (!result.contains(member))
				result.add(member);
		}
		return result.toArray(new IMember[result.size()]);
	}

	public static boolean isDefaultPackage(Object element) {
		return (element instanceof IPackageFragment) && ((IPackageFragment)element).isDefaultPackage();
	}

	/**
	 * @param pack a package fragment
	 * @return an array containing the given package and all subpackages
	 * @throws JavaModelException if getting the all sibling packages fails
	 */
	public static IPackageFragment[] getPackageAndSubpackages(IPackageFragment pack) throws JavaModelException {
		if (pack.isDefaultPackage())
			return new IPackageFragment[] { pack };

		IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getParent();
		ArrayList<IPackageFragment> subpackages= new ArrayList<>();
		subpackages.add(pack);
		String prefix= pack.getElementName() + '.';
		for (IJavaElement packageFragment : root.getChildren()) {
			IPackageFragment currentPackage = (IPackageFragment) packageFragment;
			if (currentPackage.getElementName().startsWith(prefix))
				subpackages.add(currentPackage);
		}
		return subpackages.toArray(new IPackageFragment[subpackages.size()]);
	}

	/**
	 * @param pack the package fragment; may not be null
	 * @return the parent package fragment, or null if the given package fragment is the default package or a top level package
	 */
	public static IPackageFragment getParentSubpackage(IPackageFragment pack) {
		if (pack.isDefaultPackage())
			return null;

		final int index= pack.getElementName().lastIndexOf('.');
		if (index == -1)
			return null;

		final IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getParent();
		final String newPackageName= pack.getElementName().substring(0, index);
		final IPackageFragment parent= root.getPackageFragment(newPackageName);
		if (parent.exists())
			return parent;
		else
			return null;
	}

	public static IMember[] sortByOffset(IMember[] members){
		Comparator<IMember> comparator= (o1, o2) -> {
			try{
				return o1.getNameRange().getOffset() - o2.getNameRange().getOffset();
			} catch (JavaModelException e){
				return 0;
			}
		};
		Arrays.sort(members, comparator);
		return members;
	}

	public static boolean isSourceAvailable(ISourceReference sourceReference) {
		try {
			return SourceRange.isAvailable(sourceReference.getSourceRange());
		} catch (JavaModelException e) {
			return false;
		}
	}
}
