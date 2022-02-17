/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.viewsupport;


import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

import org.eclipse.jface.util.Throttler;

import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitAnnotationModelEvent;

/**
 * Listens to resource deltas and filters for marker changes of type IMarker.PROBLEM
 * Viewers showing error ticks should register as listener to
 * this type.
 */
public class ProblemMarkerManager implements IResourceChangeListener, IAnnotationModelListener , IAnnotationModelListenerExtension {

	/**
	 * Visitors used to look if the element change delta contains a marker change.
	 */
	private static class ProjectErrorVisitor implements IResourceDeltaVisitor {

		private HashSet<IResource> fChangedElements;

		public ProjectErrorVisitor(HashSet<IResource> changedElements) {
			fChangedElements= changedElements;
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res= delta.getResource();
			if (res instanceof IProject && delta.getKind() == IResourceDelta.CHANGED) {
				IProject project= (IProject) res;
				if (!project.isAccessible()) {
					// only track open Java projects
					return false;
				}
			}
			checkInvalidate(delta, res);
			return true;
		}

		private void checkInvalidate(IResourceDelta delta, IResource resource) {
			int kind= delta.getKind();
			if (kind == IResourceDelta.REMOVED || kind == IResourceDelta.ADDED || (kind == IResourceDelta.CHANGED && isErrorDelta(delta))) {
				// invalidate the resource and all parents
				while (resource.getType() != IResource.ROOT && fChangedElements.add(resource)) {
					resource= resource.getParent();
				}
			}
		}

		private boolean isErrorDelta(IResourceDelta delta) {
			if ((delta.getFlags() & IResourceDelta.MARKERS) != 0) {
				for (IMarkerDelta markerDelta : delta.getMarkerDeltas()) {
					if (markerDelta.isSubtypeOf(IMarker.PROBLEM)) {
						int kind= markerDelta.getKind();
						if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED)
							return true;
						int severity= markerDelta.getAttribute(IMarker.SEVERITY, -1);
						int newSeverity= markerDelta.getMarker().getAttribute(IMarker.SEVERITY, -1);
						if (newSeverity != severity)
							return true;
					}
				}
			}
			return false;
		}
	}

	private final ListenerList<IProblemChangedListener> fListeners= new ListenerList<>();

	private final Set<IResource> fResourcesWithMarkerChanges= ConcurrentHashMap.newKeySet();
	private final Set<IResource> fResourcesWithAnnotationChanges= ConcurrentHashMap.newKeySet();

	private final Throttler throttledUpdates= new Throttler(PlatformUI.getWorkbench().getDisplay(), Duration.ofMillis(250), this::runPendingUpdates);

	public ProblemMarkerManager() {
	}

	/*
	 * @see IResourceChangeListener#resourceChanged
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		HashSet<IResource> changedElements= new HashSet<>();

		try {
			IResourceDelta delta= event.getDelta();
			if (delta != null)
				delta.accept(new ProjectErrorVisitor(changedElements));
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
		}

		if (fResourcesWithMarkerChanges.addAll(changedElements)) {
			fireChanges();
		}
	}

	@Override
	public void modelChanged(IAnnotationModel model) {
		// no action
	}

	@Override
	public void modelChanged(AnnotationModelEvent event) {
		if (event instanceof CompilationUnitAnnotationModelEvent) {
			CompilationUnitAnnotationModelEvent cuEvent= (CompilationUnitAnnotationModelEvent) event;
			if (cuEvent.includesProblemMarkerAnnotationChanges()) {
				IResource changedResource= cuEvent.getUnderlyingResource();
				if (fResourcesWithAnnotationChanges.add(changedResource)) {
					fireChanges();
				}
			}
		}
	}


	/**
	 * Adds a listener for problem marker changes.
	 * @param listener the listener to add
	 */
	public void addListener(IProblemChangedListener listener) {
		if (fListeners.isEmpty()) {
			JavaPlugin.getWorkspace().addResourceChangeListener(this);
			JavaPlugin.getDefault().getCompilationUnitDocumentProvider().addGlobalAnnotationModelListener(this);
		}
		fListeners.add(listener);
	}

	/**
	 * Removes a <code>IProblemChangedListener</code>.
	 * @param listener the listener to remove
	 */
	public void removeListener(IProblemChangedListener listener) {
		fListeners.remove(listener);
		if (fListeners.isEmpty()) {
			JavaPlugin.getWorkspace().removeResourceChangeListener(this);
			JavaPlugin.getDefault().getCompilationUnitDocumentProvider().removeGlobalAnnotationModelListener(this);
		}
	}

	private void fireChanges() {
		throttledUpdates.throttledExec();
	}

	/**
	 * Notify all IProblemChangedListener. Must be called in the display thread.
	 */
	private void runPendingUpdates() {
		ArrayList<IResource> resourcesWithMarkerChanges= new ArrayList<>();
		ArrayList<IResource> resourcesWithAnnotationChanges= new ArrayList<>();
		fResourcesWithMarkerChanges.removeIf(e -> resourcesWithMarkerChanges.add(e));
		fResourcesWithAnnotationChanges.removeIf(e -> resourcesWithAnnotationChanges.add(e));
		IResource[] markerResources= resourcesWithMarkerChanges.toArray(IResource[]::new);
		IResource[] annotationResources= resourcesWithAnnotationChanges.toArray(IResource[]::new);
		for (IProblemChangedListener curr : fListeners) {
			if (markerResources.length != 0) {
				curr.problemsChanged(markerResources, true);
			}
			if (annotationResources.length != 0) {
				curr.problemsChanged(annotationResources, false);
			}
		}
	}

}
