/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;

class ParentChecker {
	private IResource[] fResources;
	private IJavaElement[] fJavaElements;

	public ParentChecker(IResource[] resources, IJavaElement[] javaElements) {
		Assert.isNotNull(resources);
		Assert.isNotNull(javaElements);
		fResources= resources;
		fJavaElements= javaElements;
	}
	
	public boolean haveCommonParent() {
		if (! resourcesHaveCommonParent() || ! javaElementsHaveCommonParent())
			return false;
		if (fJavaElements.length == 0 || fResources.length == 0)
			return true;
		IResource commonResourceParent= getCommonResourceParent();
		IJavaElement commonJavaElementParent= getCommonJavaElementParent();
		Assert.isNotNull(commonJavaElementParent);
		Assert.isNotNull(commonResourceParent);
		IJavaElement convertedToJava= JavaCore.create(commonResourceParent);
		return (convertedToJava != null && 
			convertedToJava.exists() && 
			commonJavaElementParent.equals(ReorgUtils2.toWorkingCopy(convertedToJava)));
	}

	private IJavaElement getCommonJavaElementParent() {
		return fJavaElements[0].getParent();//safe - checked before
	}

	private IResource getCommonResourceParent() {
		return fResources[0].getParent();//safe - checked before
	}

	private boolean javaElementsHaveCommonParent() {
		if (fJavaElements.length == 0)
			return true;
		IJavaElement[] javaElements= ReorgUtils2.toWorkingCopies(fJavaElements);
		IJavaElement firstParent= javaElements[0].getParent();
		Assert.isNotNull(firstParent);//this should never happen			
		for (int i= 1; i < javaElements.length; i++) {
			if (! firstParent.equals(javaElements[i].getParent()))
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
	
	public IResource[] getResources(){
		return fResources;
	}		
		
	public IJavaElement[] getJavaElements(){
		return fJavaElements;
	}

	public void removeElementsWithParentsOnList(boolean removeOnlyJavaElements) {
		if (! removeOnlyJavaElements){
			removeResourcesChildrenOfResources();
			removeResourcesChildrenOfJavaElements();
		}
		removeJavaElementsChildrenOfJavaElements();
//		removeJavaElementsChildrenOfResources(); //this case is covered by removeUnconfirmedArchives
	}
				
	private void removeResourcesChildrenOfJavaElements() {
		List subResources= new ArrayList(3);
		for (int i= 0; i < fResources.length; i++) {
			IResource subResource= fResources[i];
			for (int j= 0; j < fJavaElements.length; j++) {
				IJavaElement superElements= fJavaElements[j];
				if (isChildOf(subResource, superElements))
					subResources.add(subResource);
			}
		}
		removeFromSetToDelete((IResource[]) subResources.toArray(new IResource[subResources.size()]));
	}

	private void removeJavaElementsChildrenOfJavaElements() {
		List subElements= new ArrayList(3);
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement subElement= fJavaElements[i];
			for (int j= 0; j < fJavaElements.length; j++) {
				IJavaElement superElement= fJavaElements[j];
				if (isChildOf(subElement, superElement))
					subElements.add(subElement);
			}
		}
		removeFromSetToDelete((IJavaElement[]) subElements.toArray(new IJavaElement[subElements.size()]));
	}

	private void removeResourcesChildrenOfResources() {
		List subResources= new ArrayList(3);
		for (int i= 0; i < fResources.length; i++) {
			IResource subResource= fResources[i];
			for (int j= 0; j < fResources.length; j++) {
				IResource superResource= fResources[j];
				if (isChildOf(subResource, superResource))
					subResources.add(subResource);
			}
		}
		removeFromSetToDelete((IResource[]) subResources.toArray(new IResource[subResources.size()]));
	}

	private static boolean isChildOf(IResource subResource, IJavaElement superElement) {
		IResource parent= subResource.getParent();
		while(parent != null){
			IJavaElement el= JavaCore.create(parent);
			if (el != null && el.exists() && el.equals(superElement))
				return true;
			parent= parent.getParent();
		}
		return false;
	}

	private static boolean isChildOf(IJavaElement subElement, IJavaElement superElement) {
		if (subElement.equals(superElement))
			return false;
		IJavaElement parent= subElement.getParent();
		while(parent != null){
			if (parent.equals(superElement))
				return true;
			parent= parent.getParent();
		}
		return false;
	}

	private static boolean isChildOf(IResource subResource, IResource superResource) {
		return ! subResource.equals(superResource) && superResource.getFullPath().isPrefixOf(subResource.getFullPath());
	}

	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils2.setMinus(fResources, resourcesToNotDelete);
	}
	
	private void removeFromSetToDelete(IJavaElement[] elementsToNotDelete) {
		fJavaElements= ReorgUtils2.setMinus(fJavaElements, elementsToNotDelete);
	}
}
