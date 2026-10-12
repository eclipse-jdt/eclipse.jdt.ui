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
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


public class JavaWorkingSetUpdater implements IWorkingSetUpdater, IElementChangedListener {

	/**
	 * DO NOT REMOVE, used in a product, see https://bugs.eclipse.org/297529 .
	 * @deprecated As of 3.5, replaced by {@link IWorkingSetIDs#JAVA}
	 */
	@Deprecated
	public static final String ID= IWorkingSetIDs.JAVA;


	private List<IWorkingSet> fWorkingSets;

	private static class WorkingSetDelta {
		private IWorkingSet fWorkingSet;
		private List<IAdaptable> fElements;
		private boolean fChanged;
		public WorkingSetDelta(IWorkingSet workingSet) {
			fWorkingSet= workingSet;
			fElements= new ArrayList<>(Arrays.asList(workingSet.getElements()));
		}
		public int indexOf(Object element) {
			return fElements.indexOf(element);
		}
		public void set(int index, IAdaptable element) {
			fElements.set(index, element);
			fChanged= true;
		}
		public void remove(int index) {
			if (fElements.remove(index) != null) {
				fChanged= true;
			}
		}
		public void process() {
			if (fChanged) {
				fWorkingSet.setElements(fElements.toArray(new IAdaptable[fElements.size()]));
			}
		}
	}

	public JavaWorkingSetUpdater() {
		fWorkingSets= new ArrayList<>();
		JavaCore.addElementChangedListener(this);
	}

	@Override
	public void add(IWorkingSet workingSet) {
		checkElementExistence(workingSet);
		synchronized (fWorkingSets) {
			fWorkingSets.add(workingSet);
		}
	}

	@Override
	public boolean remove(IWorkingSet workingSet) {
		boolean result;
		synchronized(fWorkingSets) {
			result= fWorkingSets.remove(workingSet);
		}
		return result;
	}

	@Override
	public boolean contains(IWorkingSet workingSet) {
		synchronized(fWorkingSets) {
			return fWorkingSets.contains(workingSet);
		}
	}

	@Override
	public void dispose() {
		synchronized(fWorkingSets) {
			fWorkingSets.clear();
		}
		JavaCore.removeElementChangedListener(this);
	}

	@Override
	public void elementChanged(ElementChangedEvent event) {
		IWorkingSet[] workingSets;
		synchronized(fWorkingSets) {
			workingSets= fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
		}
		for (IWorkingSet workingSet : workingSets) {
			WorkingSetDelta workingSetDelta= new WorkingSetDelta(workingSet);
			processJavaDelta(workingSetDelta, event.getDelta());
			IResourceDelta[] resourceDeltas= event.getDelta().getResourceDeltas();
			if (resourceDeltas != null) {
				for (IResourceDelta resourceDelta : resourceDeltas) {
					processResourceDelta(workingSetDelta, resourceDelta);
				}
			}
			workingSetDelta.process();
		}
	}

	private void processJavaDelta(WorkingSetDelta result, IJavaElementDelta delta) {
		IJavaElement jElement= delta.getElement();
		int index= result.indexOf(jElement);
		int type= jElement.getElementType();
		int kind= delta.getKind();
		int flags= delta.getFlags();
		if (type == IJavaElement.JAVA_PROJECT && kind == IJavaElementDelta.CHANGED) {
			if (index != -1 && (flags & IJavaElementDelta.F_CLOSED) != 0) {
				result.set(index, ((IJavaProject)jElement).getProject());
			} else if ((flags & IJavaElementDelta.F_OPENED) != 0) {
				index= result.indexOf(((IJavaProject)jElement).getProject());
				if (index != -1)
					result.set(index, jElement);
			}
		}
		if (index != -1) {
			if (kind == IJavaElementDelta.REMOVED) {
				if ((flags & IJavaElementDelta.F_MOVED_TO) != 0) {
					result.set(index, delta.getMovedToElement());
				} else {
					result.remove(index);
				}
			}
		}
		IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (IResourceDelta resourceDelta : resourceDeltas) {
				processResourceDelta(result, resourceDelta);
			}
		}
		for (IJavaElementDelta child : delta.getAffectedChildren()) {
			processJavaDelta(result, child);
		}
	}

	private void processResourceDelta(WorkingSetDelta result, IResourceDelta delta) {
		IResource resource= delta.getResource();
		int type= resource.getType();
		int index= result.indexOf(resource);
		int kind= delta.getKind();
		int flags= delta.getFlags();
		if (kind == IResourceDelta.CHANGED && type == IResource.PROJECT && index != -1) {
			if ((flags & IResourceDelta.OPEN) != 0) {
				result.set(index, resource);
			}
		}
		if (index != -1 && kind == IResourceDelta.REMOVED) {
			if ((flags & IResourceDelta.MOVED_TO) != 0) {
				result.set(index,
					ResourcesPlugin.getWorkspace().getRoot().findMember(delta.getMovedToPath()));
			} else {
				result.remove(index);
			}
		}

		// Don't dive into closed or opened projects
		if (projectGotClosedOrOpened(resource, kind, flags))
			return;

		for (IResourceDelta child : delta.getAffectedChildren()) {
			processResourceDelta(result, child);
		}
	}

	private boolean projectGotClosedOrOpened(IResource resource, int kind, int flags) {
		return resource.getType() == IResource.PROJECT
			&& kind == IResourceDelta.CHANGED
			&& (flags & IResourceDelta.OPEN) != 0;
	}

	private void checkElementExistence(IWorkingSet workingSet) {
		List<IAdaptable> elements= new ArrayList<>(Arrays.asList(workingSet.getElements()));
		boolean changed= false;
		for (Iterator<IAdaptable> iter= elements.iterator(); iter.hasNext();) {
			IAdaptable element= iter.next();
			boolean remove= false;
			if (element instanceof IJavaElement) {
				IJavaElement jElement= (IJavaElement)element;
				// If we have directly a project then remove it when it
				// doesn't exist anymore. However if we have a sub element
				// under a project only remove the element if the parent
				// project is open. Otherwise we would remove all elements
				// in closed projects.
				if (jElement instanceof IJavaProject) {
					remove= !jElement.exists();
				} else {
					final IJavaProject javaProject= jElement.getJavaProject();
					final boolean isProjectOpen= javaProject != null ? javaProject.getProject().isOpen() : true;
					remove= isProjectOpen && !jElement.exists();
				}
			} else if (element instanceof IResource) {
				IResource resource= (IResource)element;
				// See comments above
				if (resource instanceof IProject) {
					remove= !resource.exists();
				} else {
					IProject project= resource.getProject();
					remove= (project != null ? project.isOpen() : true) && !resource.exists();
				}
			}
			if (remove) {
				iter.remove();
				changed= true;
			}
		}
		if (changed) {
			workingSet.setElements(elements.toArray(new IAdaptable[elements.size()]));
		}
	}
}
