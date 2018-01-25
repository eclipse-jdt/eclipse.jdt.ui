/*******************************************************************************
 * Copyright (c) 2017 Till Brychcy and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class DynamicSourcesWorkingSetUpdater implements IWorkingSetUpdater {

	private class ResourceChangeListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta= event.getDelta();
			IResourceDelta[] affectedChildren= delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.REMOVED, IResource.PROJECT);
			if (affectedChildren.length > 0) {
				triggerUpdate();
			} else {
				affectedChildren= delta.getAffectedChildren(IResourceDelta.CHANGED, IResource.PROJECT);
				for (int i= 0; i < affectedChildren.length; i++) {
					IResourceDelta projectDelta= affectedChildren[i];
					if ((projectDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
						triggerUpdate();
						// one is enough
						return;
					}
				}
			}
		}
	}

	private IResourceChangeListener fResourceChangeListener;

	private class JavaElementChangeListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			processJavaDelta(event.getDelta());
		}

		private boolean processJavaDelta(IJavaElementDelta delta) {
			IJavaElement jElement= delta.getElement();
			int type= jElement.getElementType();
			if (type == IJavaElement.JAVA_PROJECT) {
				triggerUpdate();
				return true;
			}
			if (type == IJavaElement.JAVA_MODEL) {
				IJavaElementDelta[] children= delta.getAffectedChildren();
				for (int i= 0; i < children.length; i++) {
					if (processJavaDelta(children[i]))
						return true;
				}
			}
			return false;
		}
	}

	private IElementChangedListener fJavaElementChangeListener;

	private Set<IWorkingSet> fWorkingSets= new HashSet<>();

	private Job fUpdateJob;

	public static final String TEST_NAME= "test"; //$NON-NLS-1$

	public static final String MAIN_NAME= "main"; //$NON-NLS-1$

	@Override
	public void add(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			fWorkingSets.add(workingSet);
		}
		triggerUpdate();
	}

	@Override
	public boolean remove(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			return fWorkingSets.remove(workingSet);
		}
	}

	@Override
	public boolean contains(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			return fWorkingSets.contains(workingSet);
		}
	}

	public DynamicSourcesWorkingSetUpdater() {
		fResourceChangeListener= new ResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		fJavaElementChangeListener= new JavaElementChangeListener();
		JavaCore.addElementChangedListener(fJavaElementChangeListener, ElementChangedEvent.POST_CHANGE);
	}

	@Override
	public void dispose() {
		if (fResourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceChangeListener);
			fResourceChangeListener= null;
		}
		if (fJavaElementChangeListener != null)
			JavaCore.removeElementChangedListener(fJavaElementChangeListener);
	}

	public void triggerUpdate() {
		synchronized (this) {
			if (fUpdateJob != null)
				fUpdateJob.cancel();

			fUpdateJob= new Job(WorkingSetMessages.JavaSourcesWorkingSets_updating) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					return updateElements(monitor);
				}
			};
			fUpdateJob.setSystem(true);
			fUpdateJob.schedule(1000L);
		}
	}

	public IStatus updateElements(IProgressMonitor monitor) {
		IWorkingSet[] array;
		synchronized (fWorkingSets) {
			array= fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
		}
		return updateElements(array, monitor);
	}

	private IStatus updateElements(IWorkingSet[] workingSets, IProgressMonitor monitor) {
		// final boolean isTestSourcesWorkingSet= TEST_NAME.equals(workingSet.getName());
		IWorkspaceRoot root;
		try {
			root= ResourcesPlugin.getWorkspace().getRoot();
		} catch (IllegalStateException e1) {
			// can happen during shutdown
			return Status.CANCEL_STATUS;
		}
		IJavaModel model= JavaCore.create(root);
		List<IAdaptable> testResult= new ArrayList<>();
		List<IAdaptable> mainResult= new ArrayList<>();
		try {
			IJavaProject[] jProjects= model.getJavaProjects();
			for (int i= 0; i < jProjects.length; i++) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;

				final IJavaProject project= jProjects[i];
				if (project.getProject().isOpen()) {
					for (IPackageFragmentRoot iPackageFragmentRoot : project.getPackageFragmentRoots()) {
						IClasspathEntry classpathEntry= iPackageFragmentRoot.getRawClasspathEntry();
						if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							if (classpathEntry.isTest()) {
								testResult.add(iPackageFragmentRoot);
							} else {
								mainResult.add(iPackageFragmentRoot);
							}
						}
					}
				}
			}
			IAdaptable[] testArray= testResult.toArray(new IAdaptable[testResult.size()]);
			IAdaptable[] productionArray= mainResult.toArray(new IAdaptable[mainResult.size()]);
			if (PlatformUI.isWorkbenchRunning()) {
				final Display display= PlatformUI.getWorkbench().getDisplay();
				if (!display.isDisposed()) {
					display.asyncExec(() -> {
						for (IWorkingSet w : workingSets) {
							if (MAIN_NAME.equals(w.getName())) {
								w.setElements(productionArray);
							} else if (TEST_NAME.equals(w.getName())) {
								w.setElements(testArray);
							}
						}
					});
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return Status.OK_STATUS;
	}
}
