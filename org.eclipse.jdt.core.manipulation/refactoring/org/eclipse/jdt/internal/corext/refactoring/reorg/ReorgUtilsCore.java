/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Moved most logic from org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;


public class ReorgUtilsCore {

	//workaround for bug 18311
	private static final ISourceRange fgUnknownRange= new SourceRange(-1, 0);

	protected ReorgUtilsCore() {
	}

	public static boolean isArchiveOrExternalMember(IJavaElement[] elements) {
		for (IJavaElement element : elements) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null && (root.isArchive() || root.isExternal()))
				return true;
		}
		return false;
	}

	public static boolean containsOnlyProjects(List<?> elements){
		if (elements.isEmpty())
			return false;
		for (Object name : elements) {
			if (! isProject(name))
				return false;
		}
		return true;
	}

	public static boolean isProject(Object element){
		return (element instanceof IJavaProject) || (element instanceof IProject);
	}


	public static boolean isInsideCompilationUnit(IJavaElement element) {
		return 	!(element instanceof ICompilationUnit) &&
				hasAncestorOfType(element, IJavaElement.COMPILATION_UNIT);
	}

	public static boolean isInsideClassFile(IJavaElement element) {
		return 	!(element instanceof IClassFile) &&
				hasAncestorOfType(element, IJavaElement.CLASS_FILE);
	}

	public static boolean hasAncestorOfType(IJavaElement element, int type){
		return element.getAncestor(type) != null;
	}

	/*
	 * May be <code>null</code>.
	 */
	public static ICompilationUnit getCompilationUnit(IJavaElement javaElement){
		if (javaElement instanceof ICompilationUnit)
			return (ICompilationUnit) javaElement;
		return (ICompilationUnit) javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
	}

	/*
	 * some of the returned elements may be <code>null</code>.
	 */
	public static ICompilationUnit[] getCompilationUnits(IJavaElement[] javaElements){
		ICompilationUnit[] result= new ICompilationUnit[javaElements.length];
		for (int i= 0; i < javaElements.length; i++) {
			result[i]= getCompilationUnit(javaElements[i]);
		}
		return result;
	}

	public static IResource getResource(IJavaElement element){
		if (element instanceof ICompilationUnit)
			return ((ICompilationUnit)element).getPrimary().getResource();
		else
			return element.getResource();
	}

	public static IResource[] getResources(IJavaElement[] elements) {
		IResource[] result= new IResource[elements.length];
		for (int i= 0; i < elements.length; i++) {
			result[i]= ReorgUtilsCore.getResource(elements[i]);
		}
		return result;
	}

	public static String getName(IResource resource) {
		String resourceLabel= BasicElementLabels.getResourceName(resource);
		switch (resource.getType()){
			case IResource.FILE:
				return Messages.format(RefactoringCoreMessages.ReorgUtils_0, resourceLabel);
			case IResource.FOLDER:
				return Messages.format(RefactoringCoreMessages.ReorgUtils_1, resourceLabel);
			case IResource.PROJECT:
				return Messages.format(RefactoringCoreMessages.ReorgUtils_2, resourceLabel);
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	public static String getName(IJavaElement element) throws JavaModelException {
		String pattern= createNamePattern(element);
		String arg= JavaElementLabelsCore.getElementLabel(element, JavaElementLabelsCore.ALL_DEFAULT);
		return Messages.format(pattern, arg);
	}

	private static String createNamePattern(IJavaElement element) throws JavaModelException {
		switch(element.getElementType()){
			case IJavaElement.CLASS_FILE:
				return RefactoringCoreMessages.ReorgUtils_3;
			case IJavaElement.COMPILATION_UNIT:
				return RefactoringCoreMessages.ReorgUtils_4;
			case IJavaElement.FIELD:
				return RefactoringCoreMessages.ReorgUtils_5;
			case IJavaElement.IMPORT_CONTAINER:
				return RefactoringCoreMessages.ReorgUtils_6;
			case IJavaElement.IMPORT_DECLARATION:
				return RefactoringCoreMessages.ReorgUtils_7;
			case IJavaElement.INITIALIZER:
				return RefactoringCoreMessages.ReorgUtils_8;
			case IJavaElement.JAVA_PROJECT:
				return RefactoringCoreMessages.ReorgUtils_9;
			case IJavaElement.METHOD:
				if (((IMethod)element).isConstructor())
					return RefactoringCoreMessages.ReorgUtils_10;
				else
					return RefactoringCoreMessages.ReorgUtils_11;
			case IJavaElement.PACKAGE_DECLARATION:
				return RefactoringCoreMessages.ReorgUtils_12;
			case IJavaElement.PACKAGE_FRAGMENT:
				if (JavaElementUtil.isDefaultPackage(element))
					return RefactoringCoreMessages.ReorgUtils_13;
				else
					return RefactoringCoreMessages.ReorgUtils_14;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				if (((IPackageFragmentRoot) element).isArchive())
					return RefactoringCoreMessages.ReorgUtils_21;
				if (isSourceFolder(element))
					return RefactoringCoreMessages.ReorgUtils_15;
				if (isClassFolder(element))
					return RefactoringCoreMessages.ReorgUtils_16;
				return RefactoringCoreMessages.ReorgUtils_17;
			case IJavaElement.TYPE:
				IType type= (IType)element;
				if (type.isAnonymous())
					return RefactoringCoreMessages.ReorgUtils_20;
				return RefactoringCoreMessages.ReorgUtils_18;
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	public static IResource[] getResources(List<?> elements) {
		List<IResource> resources= new ArrayList<>(elements.size());
		for (Object element : elements) {
			if (element instanceof IResource)
				resources.add((IResource) element);
		}
		return resources.toArray(new IResource[resources.size()]);
	}

	public static IJavaElement[] getJavaElements(List<?> elements) {
		List<IJavaElement> resources= new ArrayList<>(elements.size());
		for (Object element : elements) {
			if (element instanceof IJavaElement)
				resources.add((IJavaElement) element);
		}
		return resources.toArray(new IJavaElement[resources.size()]);
	}

	/**
	 * Returns the jar entry resources from the list of elements.
	 *
	 * @param elements the list of elements
	 * @return the array of jar entry resources
	 * @since 3.6
	 */
	public static IJarEntryResource[] getJarEntryResources(List<?> elements) {
		List<IJarEntryResource> resources= new ArrayList<>(elements.size());
		for (Object element : elements) {
			if (element instanceof IJarEntryResource)
				resources.add((IJarEntryResource) element);
		}
		return resources.toArray(new IJarEntryResource[resources.size()]);
	}

	public static boolean hasSourceAvailable(IMember member) throws JavaModelException{
		return ! member.isBinary() ||
				(member.getSourceRange() != null && ! fgUnknownRange.equals(member.getSourceRange()));
	}

	public static IResource[] setMinus(IResource[] setToRemoveFrom, IResource[] elementsToRemove) {
		Set<IResource> setMinus= new HashSet<>(setToRemoveFrom.length - setToRemoveFrom.length);
		setMinus.addAll(Arrays.asList(setToRemoveFrom));
		setMinus.removeAll(Arrays.asList(elementsToRemove));
		return setMinus.toArray(new IResource[setMinus.size()]);
	}

	public static IJavaElement[] setMinus(IJavaElement[] setToRemoveFrom, IJavaElement[] elementsToRemove) {
		Set<IJavaElement> setMinus= new HashSet<>(setToRemoveFrom.length - setToRemoveFrom.length);
		setMinus.addAll(Arrays.asList(setToRemoveFrom));
		setMinus.removeAll(Arrays.asList(elementsToRemove));
		return setMinus.toArray(new IJavaElement[setMinus.size()]);
	}

	public static IJavaElement[] union(IJavaElement[] set1, IJavaElement[] set2) {
		//use linked set to keep element order
		Set<IJavaElement> union= new LinkedHashSet<>(set1.length + set2.length);
		union.addAll(Arrays.asList(set1));
		union.addAll(Arrays.asList(set2));
		return union.toArray(new IJavaElement[union.size()]);
	}

	public static IResource[] union(IResource[] set1, IResource[] set2) {
		Stream<IResource> nonNullResources= Stream.concat(Arrays.stream(set1), Arrays.stream(set2)).filter(Objects::nonNull);
		//use linked set to keep element order
		Set<IResource> union= new LinkedHashSet<>(set1.length + set2.length);
		nonNullResources.forEach(x -> union.add(x));
		return union.toArray(new IResource[union.size()]);
	}

	public static IType[] getMainTypes(IJavaElement[] javaElements) throws JavaModelException {
		List<IJavaElement> result= new ArrayList<>();
		for (IJavaElement element : javaElements) {
			if (element instanceof IType && JavaElementUtil.isMainType((IType)element))
				result.add(element);
		}
		return result.toArray(new IType[result.size()]);
	}

	public static IFolder[] getFolders(IResource[] resources) {
		Set<IResource> result= getResourcesOfType(resources, IResource.FOLDER);
		return result.toArray(new IFolder[result.size()]);
	}

	public static IFile[] getFiles(IResource[] resources) {
		Set<IResource> result= getResourcesOfType(resources, IResource.FILE);
		return result.toArray(new IFile[result.size()]);
	}

	//the result can be cast down to the requested type array
	public static Set<IResource> getResourcesOfType(IResource[] resources, int typeMask){
		Set<IResource> result= new HashSet<>(resources.length);
		for (IResource resource : resources) {
			if (isOfType(resource, typeMask)) {
				result.add(resource);
			}
		}
		return result;
	}

	//the result can be cast down to the requested type array
	//type is _not_ a mask
	public static List<?> getElementsOfType(IJavaElement[] javaElements, int type){
		List<IJavaElement> result= new ArrayList<>(javaElements.length);
		for (IJavaElement javaElement : javaElements) {
			if (isOfType(javaElement, type)) {
				result.add(javaElement);
			}
		}
		return result;
	}

	public static boolean hasElementsNotOfType(IResource[] resources, int typeMask) {
		for (IResource resource : resources) {
			if (resource != null && ! isOfType(resource, typeMask))
				return true;
		}
		return false;
	}

	//type is _not_ a mask
	public static boolean hasElementsNotOfType(IJavaElement[] javaElements, int type) {
		for (IJavaElement element : javaElements) {
			if (element != null && ! isOfType(element, type))
				return true;
		}
		return false;
	}

	//type is _not_ a mask
	public static boolean hasElementsOfType(IJavaElement[] javaElements, int type) {
		for (IJavaElement element : javaElements) {
			if (element != null && isOfType(element, type))
				return true;
		}
		return false;
	}

	public static boolean hasElementsOfType(IJavaElement[] javaElements, int[] types) {
		for (int type : types) {
			if (hasElementsOfType(javaElements, type)) return true;
		}
		return false;
	}


	public static boolean hasOnlyElementsOfType(IJavaElement[] javaElements, int[] types) {
		for (IJavaElement element : javaElements) {
			boolean found= false;
			for (int j= 0; j < types.length && !found; j++) {
				if (isOfType(element, types[j]))
					found= true;
			}
			if (!found)
				return false;
		}

		return true;
	}

	public static boolean hasElementsOfType(IResource[] resources, int typeMask) {
		for (IResource resource : resources) {
			if (resource != null && isOfType(resource, typeMask))
				return true;
		}
		return false;
	}

	private static boolean isOfType(IJavaElement element, int type) {
		return element.getElementType() == type;//this is _not_ a mask
	}

	private static boolean isOfType(IResource resource, int type) {
		return resource != null && isFlagSet(resource.getType(), type);
	}

	private static boolean isFlagSet(int flags, int flag){
		return (flags & flag) != 0;
	}

	public static boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException {
		return (javaElement instanceof IPackageFragmentRoot) &&
				((IPackageFragmentRoot)javaElement).getKind() == IPackageFragmentRoot.K_SOURCE;
	}

	public static boolean isClassFolder(IJavaElement javaElement) throws JavaModelException {
		return (javaElement instanceof IPackageFragmentRoot) &&
				((IPackageFragmentRoot)javaElement).getKind() == IPackageFragmentRoot.K_BINARY;
	}

	public static boolean isPackageFragmentRoot(IJavaProject javaProject) throws JavaModelException{
		return getCorrespondingPackageFragmentRoot(javaProject) != null;
	}

	private static boolean isPackageFragmentRootCorrespondingToProject(IPackageFragmentRoot root) {
		return root.getResource() instanceof IProject;
	}

	public static IPackageFragmentRoot getCorrespondingPackageFragmentRoot(IJavaProject p) throws JavaModelException {
		for (IPackageFragmentRoot root : p.getPackageFragmentRoots()) {
			if (isPackageFragmentRootCorrespondingToProject(root)) {
				return root;
			}
		}
		return null;
	}

	public static boolean containsLinkedResources(IResource[] resources){
		for (IResource resource : resources) {
			if (resource != null && resource.isLinked()) {
				return true;
			}
		}
		return false;
	}

	public static boolean containsLinkedResources(IJavaElement[] javaElements){
		for (IJavaElement javaElement : javaElements) {
			IResource res= getResource(javaElement);
			if (res != null && res.isLinked()) return true;
		}
		return false;
	}

	public static boolean canBeDestinationForLinkedResources(IResource resource) {
		return resource.isAccessible() && resource instanceof IProject;
	}

	public static boolean canBeDestinationForLinkedResources(IJavaElement javaElement) {
		if (javaElement instanceof IPackageFragmentRoot){
			return isPackageFragmentRootCorrespondingToProject((IPackageFragmentRoot)javaElement);
		} else if (javaElement instanceof IJavaProject){
			return true;//XXX ???
		} else return false;
	}

	public static boolean isParentInWorkspaceOrOnDisk(IPackageFragment pack, IPackageFragmentRoot root){
		if (pack == null)
			return false;
		IJavaElement packParent= pack.getParent();
		if (packParent == null)
			return false;
		if (packParent.equals(root))
			return true;
		IResource packageResource= ResourceUtil.getResource(pack);
		IResource packageRootResource= ResourceUtil.getResource(root);
		return isParentInWorkspaceOrOnDisk(packageResource, packageRootResource);
	}

	public static boolean isParentInWorkspaceOrOnDisk(IPackageFragmentRoot root, IJavaProject javaProject){
		if (root == null)
			return false;
		IJavaElement rootParent= root.getParent();
		if (rootParent == null)
			return false;
		if (rootParent.equals(root))
			return true;
		IResource packageResource= ResourceUtil.getResource(root);
		IResource packageRootResource= ResourceUtil.getResource(javaProject);
		return isParentInWorkspaceOrOnDisk(packageResource, packageRootResource);
	}

	public static boolean isParentInWorkspaceOrOnDisk(ICompilationUnit cu, IPackageFragment dest){
		if (cu == null)
			return false;
		IJavaElement cuParent= cu.getParent();
		if (cuParent == null)
			return false;
		if (cuParent.equals(dest))
			return true;
		IResource cuResource= cu.getResource();
		IResource packageResource= ResourceUtil.getResource(dest);
		return isParentInWorkspaceOrOnDisk(cuResource, packageResource);
	}

	public static boolean isParentInWorkspaceOrOnDisk(IResource res, IResource maybeParent){
		if (res == null)
			return false;
		return areEqualInWorkspaceOrOnDisk(res.getParent(), maybeParent);
	}

	public static boolean areEqualInWorkspaceOrOnDisk(IResource r1, IResource r2){
		if (r1 == null || r2 == null)
			return false;
		if (r1.equals(r2))
			return true;
		URI r1Location= r1.getLocationURI();
		URI r2Location= r2.getLocationURI();
		if (r1Location == null || r2Location == null)
			return false;
		return r1Location.equals(r2Location);
	}

	public static IResource[] getNotNulls(IResource[] resources) {
		Set<IResource> result= new LinkedHashSet<>(resources.length);
		for (IResource resource : resources) {
			if (resource != null)
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}

	public static IResource[] getNotLinked(IResource[] resources) {
		Collection<IResource> result= new LinkedHashSet<>(resources.length);
		for (IResource resource : resources) {
			if (resource != null && ! result.contains(resource) && ! resource.isLinked())
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}

	/* List<IJavaElement> javaElements
	 * return ICompilationUnit -> List<IJavaElement>
	 */
	public static Map<ICompilationUnit, List<IJavaElement>> groupByCompilationUnit(List<IJavaElement> javaElements){
		Map<ICompilationUnit, List<IJavaElement>> result= new HashMap<>();
		for (IJavaElement element : javaElements) {
			ICompilationUnit cu= ReorgUtilsCore.getCompilationUnit(element);
			if (cu != null){
				if (! result.containsKey(cu))
					result.put(cu, new ArrayList<>(1));
				result.get(cu).add(element);
			}
		}
		return result;
	}

	public static void splitIntoJavaElementsAndResources(Object[] elements, List<? super IJavaElement> javaElementResult, List<? super IResource> resourceResult) {
		for (Object element : elements) {
			if (element instanceof IJavaElement) {
				javaElementResult.add((IJavaElement) element);
			} else if (element instanceof IResource) {
				IResource resource= (IResource)element;
				IJavaElement jElement= JavaCore.create(resource);
				if (jElement != null && jElement.exists())
					javaElementResult.add(jElement);
				else
					resourceResult.add(resource);
			}
		}
	}

	public static boolean containsElementOrParent(Set<IAdaptable> elements, IJavaElement element) {
		IJavaElement curr= element;
		do {
			if (elements.contains(curr))
				return true;
			curr= curr.getParent();
		} while (curr != null);
		return false;
	}

	public static boolean containsElementOrParent(Set<IAdaptable> elements, IResource element) {
		IResource curr= element;
		do {
			if (elements.contains(curr))
				return true;
			IJavaElement jElement= JavaCore.create(curr);
			if (jElement != null && jElement.exists()) {
				return containsElementOrParent(elements, jElement);
			}
			curr= curr.getParent();
		} while (curr != null);
		return false;
	}
}
