/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;


public class ParentChecker {
	private IResource[] fResources;
	private IJavaElement[] fJavaElements;
	private IJarEntryResource[] fJarResources;

	public ParentChecker(IResource[] resources, IJavaElement[] javaElements) {
		this(resources, javaElements, new IJarEntryResource[0]);
	}

	public ParentChecker(IResource[] resources, IJavaElement[] javaElements, IJarEntryResource[] jarResources) {
		Assert.isNotNull(resources);
		Assert.isNotNull(javaElements);
		Assert.isNotNull(jarResources);
		fResources= resources;
		fJavaElements= javaElements;
		fJarResources= jarResources;
	}

	public boolean haveCommonParent() {
		return getCommonParent() != null;
	}

	public Object getCommonParent(){
		if (fJavaElements.length == 0 && fResources.length == 0 && fJarResources.length == 0)
			return null;
		if (! resourcesHaveCommonParent() || ! javaElementsHaveCommonParent() || ! jarResourcesHaveCommonParent())
			return null;
		if (fJavaElements.length == 0 && fResources.length == 0)
			return getCommonJarResourceParent();
		if (fJavaElements.length == 0 && fJarResources.length == 0) {
			IResource commonResourceParent= getCommonResourceParent();
			Assert.isNotNull(commonResourceParent);
			IJavaElement convertedToJava= JavaCore.create(commonResourceParent);
			if (convertedToJava != null && convertedToJava.exists())
				return convertedToJava;
			else
				return commonResourceParent;
		}
		if (fResources.length == 0 && fJarResources.length == 0)
			return getCommonJavaElementParent();

		IJavaElement convertedToJava= null;
		IJavaElement commonJavaElementParent= null;
		Object commonJarResourcesParent= null;
		if (fResources.length != 0) {
			IResource commonResourceParent= getCommonResourceParent();
			Assert.isNotNull(commonResourceParent);
			convertedToJava= JavaCore.create(commonResourceParent);
			if (convertedToJava == null || !convertedToJava.exists())
				return null;
		}
		if (fJavaElements.length != 0) {
			commonJavaElementParent= getCommonJavaElementParent();
			Assert.isNotNull(commonJavaElementParent);
			if (convertedToJava != null && !commonJavaElementParent.equals(convertedToJava))
				return null;
		}
		Object commonParent= convertedToJava == null ? commonJavaElementParent : convertedToJava;
		if (fJarResources.length != 0) {
			commonJarResourcesParent= getCommonJarResourceParent();
			Assert.isNotNull(commonJarResourcesParent);
			if (!commonJarResourcesParent.equals(commonParent))
				return null;
		}
		return commonParent;
	}

	/**
	 * Return the common parent for the jar resources.
	 *
	 * @return the common parent for the jar resources
	 * @since 3.6
	 */
	private Object getCommonJarResourceParent() {
		Assert.isNotNull(fJarResources);
		Assert.isTrue(fJarResources.length > 0);//safe - checked before
		return fJarResources[0].getParent();
	}

	private IJavaElement getCommonJavaElementParent() {
		Assert.isNotNull(fJavaElements);
		Assert.isTrue(fJavaElements.length > 0);//safe - checked before
		return fJavaElements[0].getParent();
	}

	private IResource getCommonResourceParent() {
		Assert.isNotNull(fResources);
		Assert.isTrue(fResources.length > 0);//safe - checked before
		return fResources[0].getParent();
	}

	private boolean javaElementsHaveCommonParent() {
		if (fJavaElements.length == 0)
			return true;
		IJavaElement firstParent= fJavaElements[0].getParent();
		Assert.isNotNull(firstParent); //this should never happen
		for (int i= 1; i < fJavaElements.length; i++) {
			if (! firstParent.equals(fJavaElements[i].getParent()))
				return false;
		}
		return true;
	}

	private boolean resourcesHaveCommonParent() {
		if (fResources.length == 0)
			return true;
		IResource firstParent= fResources[0].getParent();
		Assert.isNotNull(firstParent);
		for (int i= 1; i < fResources.length; i++) {
			if (! firstParent.equals(fResources[i].getParent()))
				return false;
		}
		return true;
	}

	/**
	 * Checks whether the jar resources have the same parent.
	 *
	 * @return <code>true</code> if the jar resources have the same parent, <code>false</code>
	 *         otherwise
	 * @since 3.6
	 */
	private boolean jarResourcesHaveCommonParent() {
		if (fJarResources.length == 0)
			return true;
		Object firstParent= fJarResources[0].getParent();
		Assert.isNotNull(firstParent);
		for (int i= 1; i < fJarResources.length; i++) {
			if (! firstParent.equals(fJarResources[i].getParent()))
				return false;
		}
		return true;
	}

	public IResource[] getResources(){
		return fResources;
	}

	public IJavaElement[] getJavaElements(){
		return fJavaElements;
	}

	public void removeElementsWithAncestorsOnList(boolean removeOnlyJavaElements) {
		if (! removeOnlyJavaElements){
			removeResourcesDescendantsOfResources();
			removeResourcesDescendantsOfJavaElements();
		}
		removeJavaElementsDescendantsOfJavaElements();
//		removeJavaElementsChildrenOfResources(); //this case is covered by removeUnconfirmedArchives
	}

	private void removeResourcesDescendantsOfJavaElements() {
		Set<IResource> resourcesWithoutDescendants= filterDescendants(fResources, fJavaElements);
		fResources= resourcesWithoutDescendants.toArray(new IResource[resourcesWithoutDescendants.size()]);
	}

	private static Set<IResource> filterDescendants(IResource[] resourcesToFilter, IJavaElement[] javaElements) {
		List<IResource> descendants= new ArrayList<>();
		Set<IJavaElement> elements= new HashSet<>(Arrays.asList(javaElements));
		for (IResource currentResource : resourcesToFilter) {
			if (hasAncestor(currentResource, elements)) {
				descendants.add(currentResource);
			}
		}
		Set<IResource> filteredResources = new LinkedHashSet<>(Arrays.asList(resourcesToFilter));
		filteredResources.removeAll(descendants);
		return filteredResources;
	}

	private void removeJavaElementsDescendantsOfJavaElements() {
		Set<IJavaElement> javaElementsWithoutDescendants= filterDescendants(fJavaElements);
		fJavaElements= javaElementsWithoutDescendants.toArray(new IJavaElement[javaElementsWithoutDescendants.size()]);
	}

	private static Set<IJavaElement> filterDescendants(IJavaElement[] elementsToFilter) {
		List<IJavaElement> descendants= new ArrayList<>();
		Set<IJavaElement> elements= new LinkedHashSet<>(Arrays.asList(elementsToFilter));
		for (IJavaElement currentElement : elementsToFilter) {
			if (hasAncestor(currentElement, elements)) {
				descendants.add(currentElement);
			}
		}
		elements.removeAll(descendants);
		return elements;
	}

	private void removeResourcesDescendantsOfResources() {
		List<IResource> subResources= new ArrayList<>(3);
		for (IResource subResource : fResources) {
			for (IResource superResource : fResources) {
				if (isDescendantOf(subResource, superResource))
					subResources.add(subResource);
			}
		}
		removeFromSetToDelete(subResources.toArray(new IResource[subResources.size()]));
	}

	private static boolean hasAncestor(IResource resource, Set<IJavaElement> elements) {
		for (IResource parent= resource.getParent(); parent != null; parent= parent.getParent()) {
			IJavaElement parentElement= JavaCore.create(parent);
			if (parentElement != null && parentElement.exists() && elements.contains(parentElement)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasAncestor(IJavaElement element, Set<IJavaElement> elements) {
		for (IJavaElement parent= element.getParent(); parent != null; parent= parent.getParent()) {
			if (elements.contains(parent)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDescendantOf(IResource subResource, IResource superResource) {
		return ! subResource.equals(superResource) && superResource.getFullPath().isPrefixOf(subResource.getFullPath());
	}

	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils.setMinus(fResources, resourcesToNotDelete);
	}

	public static boolean isDescendantOf(IResource subResource, IJavaElement superElement) {
		return hasAncestor(subResource, Collections.singleton(superElement));
	}

	public static boolean isDescendantOf(IJavaElement subElement, IJavaElement superElement) {
		return hasAncestor(subElement, Collections.singleton(superElement));
	}

}
