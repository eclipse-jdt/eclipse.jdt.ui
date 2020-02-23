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
package org.eclipse.jdt.internal.ui.search;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

public class SearchResultUpdater implements IElementChangedListener, IQueryListener {

	JavaSearchResult fResult;
	private static final int REMOVED_FLAGS= IJavaElementDelta.F_MOVED_TO |
									IJavaElementDelta.F_REMOVED_FROM_CLASSPATH |
									IJavaElementDelta.F_CLOSED |
									IJavaElementDelta.F_CONTENT;

	public SearchResultUpdater(JavaSearchResult result) {
		fResult= result;
		NewSearchUI.addQueryListener(this);
		JavaCore.addElementChangedListener(this);
		// TODO make this work with resources
	}

	@Override
	public void elementChanged(ElementChangedEvent event) {
		//long t0= System.currentTimeMillis();
		IJavaElementDelta delta= event.getDelta();
		Set<IAdaptable> removedElements= new HashSet<>();
		Set<IAdaptable> potentiallyRemovedElements= new HashSet<>();
		collectRemoved(potentiallyRemovedElements, removedElements, delta);
		if (removedElements.size() > 0)
			handleRemoved(removedElements);
		if (potentiallyRemovedElements.size() > 0)
			handleRemoved(potentiallyRemovedElements);
		//System.out.println(this+"handled delta in: "+(System.currentTimeMillis()-t0));
	}

	private void handleRemoved(Set<IAdaptable> removedElements) {
		for (Object element : fResult.getElements()) {
			if (isContainedInRemoved(removedElements, element)) {
				if (element instanceof IJavaElement) {
					IJavaElement je= (IJavaElement) element;
					if (!je.exists()) {
						//System.out.println("removing: "+je+" in "+fResult.getUserData());
						for (Match match : fResult.getMatches(element)) {
							fResult.removeMatch(match);
						}
					}
				} else if (element instanceof IResource) {
					IResource resource= (IResource) element;
					if (!resource.exists()) {
						//System.out.println("removing: "+resource+" in "+fResult.getUserData());
						for (Match match : fResult.getMatches(element)) {
							fResult.removeMatch(match);
						}
					}

				}
			}
		}
	}

	private boolean isContainedInRemoved(Set<IAdaptable> removedElements, Object object) {
		for (IAdaptable iAdaptable : removedElements) {
			if (isParentOf(iAdaptable, object))
				return true;
		}
		return false;
	}

	private boolean isParentOf(Object ancestor, Object descendant) {
		while (descendant != null && !ancestor.equals(descendant))
			descendant= getParent(descendant);
		return descendant != null;
	}

	private Object getParent(Object object) {
		if (object instanceof IJavaElement)
			return ((IJavaElement)object).getParent();
		else if (object instanceof IResource)
			return ((IResource)object).getParent();
		return null;
	}

	private void collectRemoved(Set<IAdaptable> potentiallyRemovedSet, Set<IAdaptable> removedElements, IJavaElementDelta delta) {
		if (delta.getKind() == IJavaElementDelta.REMOVED)
			removedElements.add(delta.getElement());
		else if (delta.getKind() == IJavaElementDelta.CHANGED) {
			int flags= delta.getFlags();
			if ((flags & REMOVED_FLAGS) != 0) {
				potentiallyRemovedSet.add(delta.getElement());
			} else {
				for (IJavaElementDelta childDelta : delta.getAffectedChildren()) {
					collectRemoved(potentiallyRemovedSet, removedElements, childDelta);
				}
			}
		}
		IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (IResourceDelta resourceDelta : resourceDeltas) {
				collectRemovals(removedElements, resourceDelta);
			}
		}
	}

	@Override
	public void queryAdded(ISearchQuery query) {
		// don't care
	}

	@Override
	public void queryRemoved(ISearchQuery query) {
		if (fResult.equals(query.getSearchResult())) {
			JavaCore.removeElementChangedListener(this);
			NewSearchUI.removeQueryListener(this);
		}
	}

	private void collectRemovals(Set<IAdaptable> removals, IResourceDelta delta) {
		if (delta.getKind() == IResourceDelta.REMOVED)
			removals.add(delta.getResource());
		else {
			for (IResourceDelta child : delta.getAffectedChildren()) {
				collectRemovals(removals, child);
			}
		}
	}

	@Override
	public void queryStarting(ISearchQuery query) {
		// not interested
	}

	@Override
	public void queryFinished(ISearchQuery query) {
		// not interested
	}

}
