/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *     Carsten Hammer - reload imported test runs
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jdt.junit.TestRunListener;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.util.XmlProcessorFactoryJdtJunit;

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
		private HashSet<ILaunch> fTrackedLaunches= new HashSet<>(20);

		/*
		 * @see ILaunchListener#launchAdded(ILaunch)
		 */
		@Override
		public void launchAdded(ILaunch launch) {
			fTrackedLaunches.add(launch);
		}

		/*
		 * @see ILaunchListener#launchRemoved(ILaunch)
		 */
		@Override
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
		@Override
		public void launchChanged(final ILaunch launch) {
			if (!fTrackedLaunches.contains(launch))
				return;

			ILaunchConfiguration config= launch.getLaunchConfiguration();
			if (config == null)
				return;

			final IJavaProject javaProject= JUnitLaunchConfigurationConstants.getJavaProject(config);
			if (javaProject == null)
				return;

			// test whether the launch defines the JUnit attributes
			String portStr= launch.getAttribute(JUnitLaunchConfigurationConstants.ATTR_PORT);
			if (portStr == null)
				return;
			try {
				final int port= Integer.parseInt(portStr);
				fTrackedLaunches.remove(launch);
				connectTestRunner(launch, javaProject, port);
			} catch (NumberFormatException e) {
				return;
			}
		}

		private void connectTestRunner(ILaunch launch, IJavaProject javaProject, int port) {
			TestRunSession testRunSession= new TestRunSession(launch, javaProject, port);
			addTestRunSession(testRunSession);

			for (TestRunListener listener : JUnitCorePlugin.getDefault().getNewTestRunListeners()) {
				listener.sessionLaunched(testRunSession);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static final class LegacyTestRunSessionListener implements ITestRunSessionListener {
		private TestRunSession fActiveTestRunSession;
		private ITestSessionListener fTestSessionListener;

		@Override
		public void sessionAdded(TestRunSession testRunSession) {
			// Only serve one legacy ITestRunListener at a time, since they cannot distinguish between different concurrent test sessions:
			if (fActiveTestRunSession != null)
				return;

			fActiveTestRunSession= testRunSession;

			fTestSessionListener= new ITestSessionListener() {
				@Override
				public void testAdded(TestElement testElement) {
				}

				@Override
				public void sessionStarted() {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testRunStarted(fActiveTestRunSession.getTotalCount());
					}
				}
				@Override
				public void sessionTerminated() {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testRunTerminated();
					}
					sessionRemoved(fActiveTestRunSession);
				}
				@Override
				public void sessionStopped(long elapsedTime) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testRunStopped(elapsedTime);
					}
					sessionRemoved(fActiveTestRunSession);
				}
				@Override
				public void sessionEnded(long elapsedTime) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testRunEnded(elapsedTime);
					}
					sessionRemoved(fActiveTestRunSession);
				}
				@Override
				public void runningBegins() {
					// ignore
				}
				@Override
				public void testStarted(TestCaseElement testCaseElement) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testStarted(testCaseElement.getId(), testCaseElement.getTestName());
					}
				}

				@Override
				public void testFailed(TestElement testElement, Status status, String trace, String expected, String actual) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testFailed(status.getOldCode(), testElement.getId(), testElement.getTestName(), trace);
					}
				}

				@Override
				public void testEnded(TestCaseElement testCaseElement) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testEnded(testCaseElement.getId(), testCaseElement.getTestName());
					}
				}

				@Override
				public void testReran(TestCaseElement testCaseElement, Status status, String trace, String expectedResult, String actualResult) {
					org.eclipse.jdt.junit.ITestRunListener[] testRunListeners= JUnitCorePlugin.getDefault().getTestRunListeners();
					for (ITestRunListener testRunListener : testRunListeners) {
						testRunListener.testReran(testCaseElement.getId(), testCaseElement.getClassName(), testCaseElement.getTestMethodName(), status.getOldCode(), trace);
					}
				}

				@Override
				public boolean acceptsSwapToDisk() {
					return true;
				}
			};
			fActiveTestRunSession.addTestSessionListener(fTestSessionListener);
		}

		@Override
		public void sessionRemoved(TestRunSession testRunSession) {
			if (fActiveTestRunSession == testRunSession) {
				fActiveTestRunSession.removeTestSessionListener(fTestSessionListener);
				fTestSessionListener= null;
				fActiveTestRunSession= null;
			}
		}
	}

	private final ListenerList<ITestRunSessionListener> fTestRunSessionListeners= new ListenerList<>();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList<TestRunSession> fTestRunSessions= new LinkedList<>();
	private final Map<TestRunSession, File> fImportedTestRunSources= new IdentityHashMap<>();
	private final ILaunchListener fLaunchListener= new JUnitLaunchListener();

	/**
	 * Starts the model (called by the {@link JUnitCorePlugin} on startup).
	 */
	public void start() {
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(fLaunchListener);

		List<TestRunSession> restoredSessions= TestRunSessionHistory.load(JUnitCorePlugin.getHistoryDirectory(), getMaxTestRunCount());
		for (int i= restoredSessions.size() - 1; i >= 0; i--) {
			addTestRunSession(restoredSessions.get(i));
		}

		addTestRunSessionListener(new LegacyTestRunSessionListener());
	}

	/**
	 * Stops the model (called by the {@link JUnitCorePlugin} on shutdown).
	 */
	public void stop() {
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(fLaunchListener);

		TestRunSessionHistory.store(getTestRunSessions(), JUnitCorePlugin.getHistoryDirectory(), getMaxTestRunCount());
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
	public synchronized List<TestRunSession> getTestRunSessions() {
		return new ArrayList<>(fTestRunSessions);
	}

	/**
	 * Returns the file from which the given session was imported.
	 *
	 * @param testRunSession the session
	 * @return the normalized absolute source file, or <code>null</code>
	 */
	public synchronized File getImportedTestRunSource(TestRunSession testRunSession) {
		return fImportedTestRunSources.get(testRunSession);
	}

	private static int getMaxTestRunCount() {
		return Math.max(0, Platform.getPreferencesService().getInt(JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.MAX_TEST_RUNS, 10, null));
	}

	/**
	 * Adds the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 *
	 * @param testRunSession the session to add
	 */
	public void addTestRunSession(TestRunSession testRunSession) {
		addTestRunSession(testRunSession, null);
	}

	private void addTestRunSession(TestRunSession testRunSession, File importedTestRunSource) {
		Assert.isNotNull(testRunSession);
		ArrayList<TestRunSession> toRemove= new ArrayList<>();

		synchronized (this) {
			Assert.isLegal(! fTestRunSessions.contains(testRunSession));
			fTestRunSessions.addFirst(testRunSession);
			if (importedTestRunSource != null) {
				fImportedTestRunSources.put(testRunSession, importedTestRunSource);
			}

			int maxCount= getMaxTestRunCount();
			int size= fTestRunSessions.size();
			if (size > maxCount) {
				List<TestRunSession> excess= fTestRunSessions.subList(maxCount, size);
				for (Iterator<TestRunSession> iter= excess.iterator(); iter.hasNext();) {
					TestRunSession oldSession= iter.next();
					if (!oldSession.isStarting() && !oldSession.isRunning() && !oldSession.isKeptAlive()) {
						toRemove.add(oldSession);
						iter.remove();
						fImportedTestRunSources.remove(oldSession);
					}
				}
			}
		}

		for (TestRunSession oldSession : toRemove) {
			notifyTestRunSessionRemoved(oldSession);
			oldSession.removeSwapFile();
		}
		notifyTestRunSessionAdded(testRunSession);
	}

	/**
	 * Imports a test run session from the given file.
	 *
	 * @param file a file containing a test run session transcript
	 * @return the imported test run session
	 * @throws CoreException if the import failed
	 */
	public static TestRunSession importTestRunSession(File file) throws CoreException {
		File sourceFile= file.toPath().toAbsolutePath().normalize().toFile();
		TestRunSession session= readTestRunSession(sourceFile);
		JUnitCorePlugin.getModel().addTestRunSession(session, sourceFile);
		return session;
	}

	/**
	 * Reloads a file-imported test run. The existing session is replaced only after
	 * the source file has been parsed successfully.
	 *
	 * @param testRunSession the imported session to replace
	 * @return the replacement session, or <code>null</code> if the original session is no longer
	 *         registered or was not imported from a file
	 * @throws CoreException if the source file cannot be read or parsed
	 */
	public static TestRunSession reloadTestRunSession(TestRunSession testRunSession) throws CoreException {
		Assert.isNotNull(testRunSession);
		JUnitModel model= JUnitCorePlugin.getModel();
		File sourceFile= model.getImportedTestRunSource(testRunSession);
		if (sourceFile == null) {
			return null;
		}

		TestRunSession replacementSession= readTestRunSession(sourceFile);
		if (!model.replaceTestRunSession(testRunSession, replacementSession, sourceFile)) {
			return null;
		}
		return replacementSession;
	}

	private static TestRunSession readTestRunSession(File file) throws CoreException {
		try {
			SAXParserFactory parserFactory= XmlProcessorFactoryJdtJunit.createSAXFactoryWithErrorOnDOCTYPE();
//			parserFactory.setValidating(true); // TODO: add DTD and debug flag
			SAXParser parser= parserFactory.newSAXParser();
			TestRunHandler handler= new TestRunHandler();
			parser.parse(file, handler);
			return handler.getTestRunSession();
		} catch (ParserConfigurationException | SAXException e) {
			throwImportError(file, e);
		} catch (IOException e) {
			throwImportError(file, e);
		} catch (IllegalArgumentException e) {
			// Bug in parser: can throw IAE even if file is not null
			throwImportError(file, e);
		}
		return null; // does not happen
	}

	/**
	 * Imports a test run session from the given URL.
	 *
	 * @param url an URL to a test run session transcript
	 * @param monitor a progress monitor for cancellation
	 * @return the imported test run session
	 * @throws InvocationTargetException wrapping a CoreException if the import failed
	 * @throws InterruptedException if the import was cancelled
	 * @since 3.6
	 */
	public static TestRunSession importTestRunSession(String url, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask(ModelMessages.JUnitModel_importing_from_url, IProgressMonitor.UNKNOWN);
		final String trimmedUrl= url.trim().replaceAll("\r\n?|\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
		final TestRunHandler handler= new TestRunHandler(monitor);

		final CoreException[] exception= { null };
		final TestRunSession[] session= { null };

		Thread importThread= new Thread("JUnit URL importer") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					SAXParserFactory parserFactory= XmlProcessorFactoryJdtJunit.createSAXFactoryWithErrorOnDOCTYPE();
//					parserFactory.setValidating(true); // TODO: add DTD and debug flag
					SAXParser parser= parserFactory.newSAXParser();
					parser.parse(trimmedUrl, handler);
					session[0]= handler.getTestRunSession();
				} catch (OperationCanceledException e) {
					// canceled
				} catch (ParserConfigurationException | SAXException e) {
					storeImportError(e);
				} catch (IOException e) {
					storeImportError(e);
				} catch (IllegalArgumentException e) {
					// Bug in parser: can throw IAE even if URL is not null
					storeImportError(e);
				}
			}
			private void storeImportError(Exception e) {
				exception[0]= new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
						JUnitCorePlugin.getPluginId(), ModelMessages.JUnitModel_could_not_import, e));
			}
		};
		importThread.start();

		while (session[0] == null && exception[0] == null && !monitor.isCanceled()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// that's OK
			}
		}
		if (session[0] == null) {
			if (exception[0] != null) {
				throw new InvocationTargetException(exception[0]);
			} else {
				importThread.interrupt(); // have to kill the thread since we don't control URLConnection and XML parsing
				throw new InterruptedException();
			}
		}

		JUnitCorePlugin.getModel().addTestRunSession(session[0]);
		monitor.done();
		return session[0];
	}

	public static void importIntoTestRunSession(File swapFile, TestRunSession testRunSession) throws CoreException {
		try {
			SAXParserFactory parserFactory= XmlProcessorFactoryJdtJunit.createSAXFactoryWithErrorOnDOCTYPE();
//			parserFactory.setValidating(true); // TODO: add DTD and debug flag
			SAXParser parser= parserFactory.newSAXParser();
			TestRunHandler handler= new TestRunHandler(testRunSession);
			parser.parse(swapFile, handler);
		} catch (ParserConfigurationException | SAXException e) {
			throwImportError(swapFile, e);
		} catch (IOException e) {
			throwImportError(swapFile, e);
		} catch (IllegalArgumentException e) {
			// Bug in parser: can throw IAE even if file is not null
			throwImportError(swapFile, e);
		}
	}

	/**
	 * Exports the given test run session.
	 *
	 * @param testRunSession the test run session
	 * @param file the destination
	 * @throws CoreException if an error occurred
	 */
	public static void exportTestRunSession(TestRunSession testRunSession, File file) throws CoreException {
		try (FileOutputStream out= new FileOutputStream(file)) {
			exportTestRunSession(testRunSession, out);
		} catch (IOException | TransformerConfigurationException e) {
			throwExportError(file, e);
		} catch (TransformerException e) {
			throwExportError(file, e);
		}
	}

	public static void exportTestRunSession(TestRunSession testRunSession, OutputStream out)
			throws TransformerFactoryConfigurationError, TransformerException {

		Transformer transformer= XmlProcessorFactoryJdtJunit.createTransformerFactoryWithErrorOnDOCTYPE().newTransformer();
		InputSource inputSource= new InputSource();
		SAXSource source= new SAXSource(new TestRunSessionSerializer(testRunSession), inputSource);
		StreamResult result= new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		/*
		 * Bug in Xalan: Only indents if proprietary property
		 * org.apache.xalan.templates.OutputProperties.S_KEY_INDENT_AMOUNT is set.
		 *
		 * Bug in Xalan as shipped with J2SE 5.0:
		 * Does not read the indent-amount property at all >:-(.
		 */
		try {
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalArgumentException e) {
			// no indentation today...
		}
		transformer.transform(source, result);
	}

	private static void throwExportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
				JUnitCorePlugin.getPluginId(),
				Messages.format(ModelMessages.JUnitModel_could_not_write, BasicElementLabels.getPathLabel(file)),
				e));
	}

	private static void throwImportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
				JUnitCorePlugin.getPluginId(),
				Messages.format(ModelMessages.JUnitModel_could_not_read, BasicElementLabels.getPathLabel(file)),
				e));
	}

	private boolean replaceTestRunSession(TestRunSession oldSession, TestRunSession newSession, File sourceFile) {
		Assert.isNotNull(oldSession);
		Assert.isNotNull(newSession);
		boolean replaced;
		synchronized (this) {
			int index= fTestRunSessions.indexOf(oldSession);
			replaced= index >= 0;
			if (replaced) {
				fTestRunSessions.set(index, newSession);
				fImportedTestRunSources.remove(oldSession);
				fImportedTestRunSources.put(newSession, sourceFile);
			}
		}
		if (replaced) {
			notifyTestRunSessionRemoved(oldSession);
			oldSession.removeSwapFile();
			notifyTestRunSessionAdded(newSession);
		}
		return replaced;
	}

	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 *
	 * @param testRunSession the session to remove
	 */
	public void removeTestRunSession(TestRunSession testRunSession) {
		boolean existed;
		synchronized (this) {
			existed= fTestRunSessions.remove(testRunSession);
			fImportedTestRunSources.remove(testRunSession);
		}
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
		testRunSession.removeSwapFile();
	}

	private void notifyTestRunSessionRemoved(TestRunSession testRunSession) {
		testRunSession.stopTestRun();
		ILaunch launch= testRunSession.getLaunch();
		if (launch != null) {
			ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunch(launch);
		}

		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(TestRunSession testRunSession) {
		for (ITestRunSessionListener listener : fTestRunSessionListeners) {
			listener.sessionAdded(testRunSession);
		}
	}

}
