/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;

/**
 * Central registry for JUnit test runs.
 */
public final class JUnitModel {
	
	private final class JUnitLaunchListener implements ILaunchListener {

		/**
		 * Used to track new launches. We need to do this
		 * so that we only attach a TestRunner once to a launch.
		 * Once a test runner is connected, it is removed from the set.
		 */
		private HashSet fTrackedLaunches= new HashSet(20);

		/*
		 * @see ILaunchListener#launchAdded(ILaunch)
		 */
		public void launchAdded(ILaunch launch) {
			fTrackedLaunches.add(launch);
		}

		/*
		 * @see ILaunchListener#launchRemoved(ILaunch)
		 */
		public void launchRemoved(final ILaunch launch) {
			fTrackedLaunches.remove(launch);
			//TODO: story for removing old test runs?
//			getDisplay().asyncExec(new Runnable() {
//				public void run() {
//					TestRunnerViewPart testRunnerViewPart= findTestRunnerViewPartInActivePage();
//					if (testRunnerViewPart != null && testRunnerViewPart.isCreated() && launch.equals(testRunnerViewPart.getLastLaunch()))
//						testRunnerViewPart.reset();
//				}
//			});
		}

		/*
		 * @see ILaunchListener#launchChanged(ILaunch)
		 */
		public void launchChanged(final ILaunch launch) {
			if (!fTrackedLaunches.contains(launch))
				return;
		
			ILaunchConfiguration config= launch.getLaunchConfiguration();
			if (config == null)
				return;
			
			// test whether the launch defines the JUnit attributes
			String portStr= launch.getAttribute(JUnitBaseLaunchConfiguration.PORT_ATTR);
			String typeStr= launch.getAttribute(JUnitBaseLaunchConfiguration.TESTTYPE_ATTR);
			if (portStr == null || typeStr == null)
				return;
			
			IJavaElement element= JavaCore.create(typeStr);
			if (! (element instanceof IType))
				return;
			
			final int port= Integer.parseInt(portStr);
			final IType launchedType= (IType) element;
			fTrackedLaunches.remove(launch);
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					connectTestRunner(launch, launchedType, port);
				}
			});
		}

		private void connectTestRunner(ILaunch launch, IType launchedType, int port) {
			showTestRunnerViewPartInActivePage(findTestRunnerViewPartInActivePage());
			
			//TODO: Do notifications have to be sent in UI thread? 
			// Check concurrent access to fTestRunSessions (no problem inside asyncExec())
			int count= fTestRunSessions.size();
			int maxCount= JUnitPlugin.getDefault().getPreferenceStore().getInt(JUnitPreferencesConstants.MAX_TEST_RUNS);
			for (int i= count - 1; i >= maxCount - 1; i--) {
				TestRunSession session= (TestRunSession) fTestRunSessions.remove(i);
				notifyTestRunSessionRemoved(session);
			}
			
			TestRunSession testRunSession= new TestRunSession(launchedType, port, launch);
			fTestRunSessions.addFirst(testRunSession);
			notifyTestRunSessionAdded(testRunSession);
			
		}

		private TestRunnerViewPart showTestRunnerViewPartInActivePage(TestRunnerViewPart testRunner) {
			IWorkbenchPart activePart= null;
			IWorkbenchPage page= null;
			try {
				// TODO: have to force the creation of view part contents 
				// otherwise the UI will not be updated
				if (testRunner != null && testRunner.isCreated())
					return testRunner;
				page= JUnitPlugin.getActivePage();
				if (page == null)
					return null;
				activePart= page.getActivePart();
				//	show the result view if it isn't shown yet
				return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);
			} catch (PartInitException pie) {
				JUnitPlugin.log(pie);
				return null;
			} finally{
				//restore focus stolen by the creation of the result view
				if (page != null && activePart != null)
					page.activate(activePart);
			}
		}

		private TestRunnerViewPart findTestRunnerViewPartInActivePage() {
			IWorkbenchPage page= JUnitPlugin.getActivePage();
			if (page == null)
				return null;
			return (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
		}

		private Display getDisplay() {
//			Shell shell= getActiveWorkbenchShell();
//			if (shell != null) {
//				return shell.getDisplay();
//			}
			Display display= Display.getCurrent();
			if (display == null) {
				display= Display.getDefault();
			}
			return display;
		}
	}

	private final ListenerList fTestRunSessionListeners= new ListenerList();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList/*<TestRunSession>*/ fTestRunSessions= new LinkedList();
	private final ILaunchListener fLaunchListener= new JUnitLaunchListener();

	/**
	 * Starts the model (called by the {@link JUnitPlugin} on startup).
	 */
	public void start() {
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(fLaunchListener);
	}

	/**
	 * Stops the model (called by the {@link JUnitPlugin} on shutdown).
	 */
	public void stop() {
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(fLaunchListener);
	}
	
	
	public void addTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}
	
	public void removeTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}
	
	
	/**
	 * @return a list of active {@link TestRunSession}s. The list is a copy of
	 *         the internal data structure and modifications do not affect the
	 *         global list of active sessions. The list is sorted by age, youngest first.  
	 */
	public List getTestRunSessions() {
		return new ArrayList(fTestRunSessions);
	}
	
	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 * 
	 * @param testRunSession the session to remove
	 */
	public void removeTestRunSession(TestRunSession testRunSession) {
		boolean existed= fTestRunSessions.remove(testRunSession);
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
	}
	
	private void notifyTestRunSessionRemoved(TestRunSession testRunSession) {
		 Object[] listeners = fTestRunSessionListeners.getListeners();
		 for (int i = 0; i < listeners.length; ++i) {
		 	((ITestRunSessionListener) listeners[i]).sessionRemoved(testRunSession);
		 }
	}
	
	private void notifyTestRunSessionAdded(TestRunSession testRunSession) {
		 Object[] listeners = fTestRunSessionListeners.getListeners();
		 for (int i = 0; i < listeners.length; ++i) {
		 	((ITestRunSessionListener) listeners[i]).sessionAdded(testRunSession);
		 }
	}

}
