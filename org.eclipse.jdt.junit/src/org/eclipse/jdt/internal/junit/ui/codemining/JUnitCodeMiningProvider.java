/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui.codemining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.internal.ui.javaeditor.JavaCodeMiningReconciler;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Code mining provider which shows, above JUnit test classes and test methods, the result of their
 * last run and the Run and Debug actions. Each of them is enabled by its own preference, see
 * {@link PreferenceConstants#EDITOR_JAVA_CODEMINING_SHOW_JUNIT_STATUS},
 * {@link PreferenceConstants#EDITOR_JAVA_CODEMINING_SHOW_JUNIT_RUN} and
 * {@link PreferenceConstants#EDITOR_JAVA_CODEMINING_SHOW_JUNIT_DEBUG}.
 * <p>
 * A class gets code minings if the test kind of its project says it is a test class (which
 * excludes abstract classes, interfaces and helper classes), a method if it is a test method of
 * this kind. Member types, such as the JUnit 5 <code>@Nested</code> classes, are visited and
 * handled like top level types. Local and anonymous types are not visited.
 * </p>
 */
public class JUnitCodeMiningProvider extends AbstractCodeMiningProvider {

	private final JUnitStatusRegistry fRegistry= JUnitPlugin.getDefault().getStatusRegistry();

	private final Set<ITextViewer> fViewers= Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private final Runnable fRefresh= this::refresh;

	private volatile boolean fTimeoutLogged;

	public JUnitCodeMiningProvider() {
		if (fRegistry != null) {
			fRegistry.addFinishedListener(fRefresh);
		}
	}

	private static boolean isEnabled(String preferenceName) {
		return PreferenceConstants.getPreferenceStore().getBoolean(preferenceName);
	}

	private void refresh() {
		if (!isEnabled(PreferenceConstants.EDITOR_CODEMINING_ENABLED) || !isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_STATUS)) {
			return;
		}
		Display display= Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		try {
			display.asyncExec(() -> {
				List<ITextViewer> viewers;
				synchronized (fViewers) {
					viewers= new ArrayList<>(fViewers);
				}
				for (ITextViewer viewer : viewers) {
					if (viewer instanceof ISourceViewerExtension5 extension && viewer.getTextWidget() != null
							&& !viewer.getTextWidget().isDisposed()) {
						extension.updateCodeMinings();
					}
				}
			});
		} catch (SWTException e) {
			// the display is disposed, the workbench is shutting down
		}
	}

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
		ITextEditor textEditor= super.getAdapter(ITextEditor.class);
		// Only Java editor is supported, see bug 541811
		if (!(textEditor instanceof JavaEditor) || !isEnabled(PreferenceConstants.EDITOR_CODEMINING_ENABLED)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		boolean showStatus= fRegistry != null && isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_STATUS);
		boolean showRun= isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_RUN);
		boolean showDebug= isEnabled(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_DEBUG);
		if (!(showStatus || showRun || showDebug)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		fViewers.add(viewer);
		// cancelling this computation must not cancel the monitor of the code mining manager
		IProgressMonitor ownMonitor= new NullProgressMonitor() {
			@Override
			public boolean isCanceled() {
				return super.isCanceled() || monitor.isCanceled();
			}
		};
		// waiting for the type root has no timeout, only the computation has one
		CompletableFuture<List<? extends ICodeMining>> future= JavaCodeMiningReconciler.getFuture(textEditor)
				.<List<? extends ICodeMining>>thenCompose(typeRoot -> CompletableFuture
						.<List<? extends ICodeMining>>supplyAsync(() -> computeCodeMinings(viewer, typeRoot, ownMonitor, showStatus, showRun, showDebug))
						.orTimeout(15, TimeUnit.SECONDS))
				.handle((result, ex) -> {
					if (ex != null) {
						// cancelled, timed out or failed: stop the computation
						ownMonitor.setCanceled(true);
						Throwable cause= unwrap(ex);
						if (cause instanceof TimeoutException) {
							if (!fTimeoutLogged) {
								fTimeoutLogged= true;
								JUnitPlugin.log(Status.warning("Computing the JUnit code minings took more than 15 seconds, they are not shown", ex)); //$NON-NLS-1$
							}
						} else if (!(cause instanceof CancellationException)) {
							JUnitPlugin.log(ex);
						}
						// like the other providers, a cancellation leaves the code minings as they are
						return cause instanceof CancellationException ? null : Collections.<ICodeMining>emptyList();
					}
					return result;
				});
		// the computation stops when the manager cancels the result
		future.whenComplete((_, _) -> {
			if (future.isCancelled()) {
				ownMonitor.setCanceled(true);
			}
		});
		return future;
	}

	private static Throwable unwrap(Throwable ex) {
		return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
	}

	private List<ICodeMining> computeCodeMinings(ITextViewer viewer, ITypeRoot typeRoot, IProgressMonitor monitor,
			boolean showStatus, boolean showRun, boolean showDebug) {
		if (typeRoot == null) {
			return Collections.emptyList();
		}
		List<ICodeMining> minings= new ArrayList<>();
		try {
			Collector collector= new Collector(viewer, monitor, minings, showStatus, showRun, showDebug);
			for (IJavaElement element : typeRoot.getChildren()) {
				if (element instanceof IType type) {
					collector.collect(type, null);
				}
			}
		} catch (JavaModelException e) {
			// The code minings computed so far are kept
			JUnitPlugin.log(e);
		}
		return minings;
	}

	private final class Collector {

		private final ITextViewer fTextViewer;

		private final IProgressMonitor fMonitor;

		private final List<ICodeMining> fMinings;

		private final boolean fShowStatus;

		private final boolean fShowRun;

		private final boolean fShowDebug;

		private Boolean fHasJUnit;

		Collector(ITextViewer viewer, IProgressMonitor monitor, List<ICodeMining> minings,
				boolean showStatus, boolean showRun, boolean showDebug) {
			fTextViewer= viewer;
			fMonitor= monitor;
			fMinings= minings;
			fShowStatus= showStatus;
			fShowRun= showRun;
			fShowDebug= showDebug;
		}

		void collect(IType type, JUnitStatusRegistry.Results parentResults) throws JavaModelException {
			if (fMonitor.isCanceled()) {
				return;
			}
			// the finder is slow (it parses the class), so it only looks at the classes which may be tests
			ITestKind testKind= hasJUnit(type.getJavaProject()) && mayBeTest(type) ? findTestKind(type) : null;
			JUnitStatusRegistry.Results results= parentResults;
			if (testKind != null) {
				if (fShowStatus && results == null) {
					results= fRegistry.getResults(type); // only for the test classes
				}
				Boolean isTestCase= null; // only computed for the methods named like JUnit 3 tests
				for (IMethod method : type.getMethods()) {
					if (fMonitor.isCanceled()) {
						return;
					}
					if (isTestCase == null && TestSearchEngine.isJUnit3StyleTestMethod(method)) {
						isTestCase= TestSearchEngine.isConcreteTestImplementor(type);
					}
					if (TestSearchEngine.isTestMethod(method, type.getJavaProject(), testKind.getId(), Boolean.TRUE.equals(isTestCase))) {
						addMinings(method, results, testKind.getId(), JUnitMessages.JUnitCodeMining_run, JUnitMessages.JUnitCodeMining_debug);
					}
				}
				addMinings(type, results, testKind.getId(), JUnitMessages.JUnitCodeMining_runAll, JUnitMessages.JUnitCodeMining_debugAll);
			}
			for (IType memberType : type.getTypes()) {
				collect(memberType, results == null ? null : results.getMember(memberType));
			}
		}

		/** Tells whether JUnit is on the classpath of the project, which is looked up once. */
		private boolean hasJUnit(IJavaProject project) {
			if (fHasJUnit == null) {
				fHasJUnit= TestSearchEngine.hasJUnit(project);
			}
			return fHasJUnit;
		}

		/**
		 * Tells whether the class may be a test: it has annotations, member types, a superclass or
		 * superinterfaces (which can bring tests or annotations such as <code>@RunWith</code>), or
		 * methods which are annotated or named like tests or suites.
		 */
		private boolean mayBeTest(IType type) throws JavaModelException {
			if (type.getAnnotations().length > 0 || type.getTypes().length > 0 || type.getSuperclassName() != null
					|| type.getSuperInterfaceNames().length > 0) {
				return true;
			}
			for (IMethod method : type.getMethods()) {
				if (method.getAnnotations().length > 0 || method.getElementName().startsWith("test") || method.getElementName().equals("suite")) { //$NON-NLS-1$ //$NON-NLS-2$
					return true;
				}
			}
			return false;
		}

		private static boolean isAvailable(String testKindId, IJavaProject project) {
			return switch (testKindId) {
				case TestKindRegistry.JUNIT6_TEST_KIND_ID -> CoreTestSearchEngine.hasJUnit6TestAnnotation(project);
				case TestKindRegistry.JUNIT5_TEST_KIND_ID -> CoreTestSearchEngine.hasJUnit5TestAnnotation(project);
				case TestKindRegistry.JUNIT4_TEST_KIND_ID -> CoreTestSearchEngine.hasJUnit4TestAnnotation(project);
				default -> CoreTestSearchEngine.hasTestCaseType(project);
			};
		}

		/**
		 * Returns the first test kind whose finder accepts the (concrete) class: the one of its
		 * project, then the ones of the other JUnit versions, since a project can mix them.
		 */
		private ITestKind findTestKind(IType type) throws JavaModelException {
			if (Flags.isAbstract(type.getFlags())) {
				return null;
			}
			Set<String> ids= new LinkedHashSet<>();
			ids.add(TestKindRegistry.getContainerTestKindId(type));
			ids.addAll(List.of(TestKindRegistry.JUNIT6_TEST_KIND_ID, TestKindRegistry.JUNIT5_TEST_KIND_ID, TestKindRegistry.JUNIT4_TEST_KIND_ID, TestKindRegistry.JUNIT3_TEST_KIND_ID));
			String own= ids.iterator().next();
			for (String id : ids) {
				if (fMonitor.isCanceled()) {
					return null;
				}
				// a fallback kind must be one the launch of the project can use
				if (!id.equals(own) && !isAvailable(id, type.getJavaProject())) {
					continue;
				}
				try {
					ITestKind kind= TestKindRegistry.getDefault().getKind(id);
					if (kind != null && kind.getFinder().isTest(type)) {
						return kind;
					}
				} catch (CoreException e) {
					JUnitPlugin.log(e);
				}
			}
			return null;
		}

		private void addMinings(IJavaElement element, JUnitStatusRegistry.Results results, String testKindId, String runLabel, String debugLabel) {
			try {
				if (fShowStatus) {
					fMinings.add(new JUnitStatusCodeMining(element, results, fTextViewer.getDocument(), JUnitCodeMiningProvider.this));
				}
				if (fShowRun) {
					fMinings.add(new JUnitLaunchCodeMining(element, runLabel, testKindId, ILaunchManager.RUN_MODE, fTextViewer.getDocument(), JUnitCodeMiningProvider.this));
				}
				if (fShowDebug) {
					fMinings.add(new JUnitLaunchCodeMining(element, debugLabel, testKindId, ILaunchManager.DEBUG_MODE, fTextViewer.getDocument(), JUnitCodeMiningProvider.this));
				}
			} catch (BadLocationException e) {
				// the document is not in sync with the model (yet), no code mining for the element
			} catch (JavaModelException e) {
				// no source for the element is expected, no code mining for it
				if (!e.isDoesNotExist()) {
					JUnitPlugin.log(e);
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (fRegistry != null) {
			fRegistry.removeFinishedListener(fRefresh);
		}
		fViewers.clear();
		super.dispose();
	}
}
