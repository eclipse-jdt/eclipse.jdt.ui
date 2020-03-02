/*******************************************************************************
 * Copyright (c) 2018 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater2;
import org.eclipse.ui.progress.WorkbenchJob;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class DynamicSourcesWorkingSetUpdater implements IWorkingSetUpdater2 {

	private class JavaElementChangeListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			processJavaDelta(event.getDelta());
		}

		private boolean processJavaDelta(IJavaElementDelta delta) {
			IJavaElement jElement= delta.getElement();
			int type= jElement.getElementType();
			if (type == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				int kind= delta.getKind();
				if (kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED) {
					// this can happen without "classpath changed" event, if the directory corresponding to an optional source folder is created.
					triggerUpdate();
					return true;
				}
				// do not traverse into children
			} else if (type == IJavaElement.JAVA_PROJECT) {
				int kind= delta.getKind();
				int flags= delta.getFlags();
				if (kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED
						|| (flags & (IJavaElementDelta.F_OPENED | IJavaElementDelta.F_CLOSED | IJavaElementDelta.F_CLASSPATH_CHANGED)) != 0) {
					triggerUpdate();
					return true;
				}
				for (IJavaElementDelta element : delta.getAffectedChildren()) {
					if (processJavaDelta(element))
						return true;
				}
			} else if (type == IJavaElement.JAVA_MODEL) {
				for (IJavaElementDelta element : delta.getAffectedChildren()) {
					if (processJavaDelta(element))
						return true;
				}
			}
			return false;
		}
	}

	private class UpdateUIJob extends WorkbenchJob {

		volatile Runnable task;

		public UpdateUIJob() {
			super(WorkingSetMessages.JavaSourcesWorkingSets_updating);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Runnable r = task;
			if(r != null && !monitor.isCanceled() && !isDisposed.get()) {
				r.run();
			}
			return Status.OK_STATUS;
		}

		void setTask(Runnable r){
			cancel();
			task = r;
			if(r != null){
				schedule();
			}
		}

		Runnable getTask() {
			return task;
		}
	}

	private IElementChangedListener fJavaElementChangeListener;

	private Set<IWorkingSet> fWorkingSets= new HashSet<>();

	private Map<String, IAdaptable[]> fInitialContents;

	private static final IAdaptable[] NOT_INIALIZED = new IAdaptable[0];

	private Job fUpdateJob;

	private UpdateUIJob fUpdateUIJob;

	private AtomicBoolean isDisposed= new AtomicBoolean();

	public static final String TEST_OLD_NAME= "test"; //$NON-NLS-1$

	public static final String MAIN_OLD_NAME= "main"; //$NON-NLS-1$

	public static final String TEST_NAME= "Java Test Sources"; //$NON-NLS-1$

	public static final String MAIN_NAME= "Java Main Sources"; //$NON-NLS-1$

	@Override
	public void add(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			fWorkingSets.add(workingSet);
		}
	}

	@Override
	public boolean remove(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			boolean removed= fWorkingSets.remove(workingSet);
			if(fWorkingSets.isEmpty()) {
				fUpdateJob.cancel();
				fUpdateUIJob.setTask(null);
				if (fJavaElementChangeListener != null) {
					JavaCore.removeElementChangedListener(fJavaElementChangeListener);
				}
			}
			return removed;
		}
	}

	@Override
	public boolean contains(IWorkingSet workingSet) {
		synchronized (fWorkingSets) {
			return fWorkingSets.contains(workingSet);
		}
	}

	public DynamicSourcesWorkingSetUpdater() {
		fInitialContents = new ConcurrentHashMap<>();
		fInitialContents.put(MAIN_NAME, NOT_INIALIZED);
		fInitialContents.put(TEST_NAME, NOT_INIALIZED);
		fUpdateJob= new Job(WorkingSetMessages.JavaSourcesWorkingSets_updating) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return updateElements(monitor);
			}
		};
		fUpdateUIJob = new UpdateUIJob();
		fUpdateJob.setSystem(true);
		fJavaElementChangeListener= new JavaElementChangeListener();
	}

	@Override
	public void dispose() {
		isDisposed.set(true);
		if (fJavaElementChangeListener != null) {
			JavaCore.removeElementChangedListener(fJavaElementChangeListener);
		}
		fUpdateJob.cancel();
		fUpdateUIJob.setTask(null);
	}

	public void triggerUpdate() {
		if(isDisposed.get()) {
			return;
		}
		fUpdateJob.cancel();
		fUpdateJob.schedule(1000L);
	}

	public IStatus updateElements(IProgressMonitor monitor) {
		IWorkingSet[] array;
		synchronized (fWorkingSets) {
			array= fWorkingSets.toArray(new IWorkingSet[fWorkingSets.size()]);
		}
		return updateElements(array, monitor);
	}

	private IStatus updateElements(IWorkingSet[] workingSets, IProgressMonitor monitor) {
		try {
			if(isDisposed.get() || monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			Map<String, IAdaptable[]> data= collectData(monitor);
			Runnable update = () -> updateWorkingSets(workingSets, data);
			if(Display.getCurrent() != null) {
				update.run();
			} else {
				fUpdateUIJob.setTask(new Runnable() {
					@Override
					public void run() {
						// check if the next task is already in queue
						if (this != fUpdateUIJob.getTask()) {
							update.run();
						}
					}
				});
			}
		} catch (Exception e) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private static void updateWorkingSets(IWorkingSet[] workingSets, Map<String, IAdaptable[]> data) {
		for (IWorkingSet w : workingSets) {
			if (MAIN_NAME.equals(w.getName())) {
				w.setElements(data.get(MAIN_NAME));
			} else if (TEST_NAME.equals(w.getName())) {
				w.setElements(data.get(TEST_NAME));
			}
		}
	}

	private Map<String, IAdaptable[]> collectData(IProgressMonitor monitor) throws CoreException {
		Map<String, IAdaptable[]> data = new HashMap<>();
		data.put(MAIN_NAME, new IAdaptable[0]);
		data.put(TEST_NAME, new IAdaptable[0]);
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel model= JavaCore.create(root);
		List<IAdaptable> testResult= new ArrayList<>();
		List<IAdaptable> mainResult= new ArrayList<>();
		for (IJavaProject project : model.getJavaProjects()) {
			if (monitor.isCanceled() || isDisposed.get()) {
				return data;
			}
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
		data.put(MAIN_NAME, productionArray);
		data.put(TEST_NAME, testArray);
		if(NOT_INIALIZED.equals(fInitialContents.get(MAIN_NAME))) {
			fInitialContents.put(MAIN_NAME, data.get(MAIN_NAME));
		}
		if(NOT_INIALIZED.equals(fInitialContents.get(TEST_NAME))) {
			fInitialContents.put(TEST_NAME, data.get(TEST_NAME));
		}
		return data;
	}

	@Override
	public boolean isManagingPersistenceOf(IWorkingSet set) {
		return true;
	}

	@Override
	public IAdaptable[] restore(IWorkingSet set) {
		String name= set.getName();
		if(!MAIN_NAME.equals(name) && !TEST_NAME.equals(name)) {
			return NOT_INIALIZED;
		}
		IAdaptable[] data= fInitialContents.get(name);
		if(data == null || NOT_INIALIZED.equals(data)) {
			try {
				data = collectData(new NullProgressMonitor()).get(name);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
			JavaCore.addElementChangedListener(fJavaElementChangeListener, ElementChangedEvent.POST_CHANGE);
		}
		fInitialContents.remove(name);
		return data;
	}
}
