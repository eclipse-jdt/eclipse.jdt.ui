/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Reproduces a real production UI freeze: three independent, unrelated call sites all asked
 * {@link CoreASTProvider#getAST} for the AST of the same active editor's element while a reconcile
 * for it was in progress, and that reconcile never completed (the background reconciler thread had
 * already gone idle - it was not working on this element anymore, so nothing was ever going to call
 * {@link CoreASTProvider#reconciled} for it again). Every one of the three callers got stuck in an
 * infinite, 30-seconds-per-retry loop: each {@code fWaitLock.wait(30000)} timed out,
 * {@code isReconciling} was still {@code true}, so the (originally recursive) retry waited another
 * fresh 30 seconds - forever. A thread dump of the real freeze recorded it still ongoing after
 * 4,581 seconds (about 76 minutes, i.e. roughly 152 stacked 30-second retries), with one of the
 * three callers blocking the SWT UI thread itself.
 * <p>
 * The three callers, and the exact flag each one used:
 * <ul>
 * <li>{@code OrganizeImportsAction.run()} - on the <b>main UI thread</b> - called
 * {@code getAST(cu, WAIT_ACTIVE_ONLY, null)}. {@code WAIT_ACTIVE_ONLY} is not the conservative
 * "don't build, don't wait" flag its name suggests: for an element that <em>is</em> the active one
 * but has no AST cached yet, it takes exactly the same wait/retry path as {@code WAIT_YES}.</li>
 * <li>{@code OverrideIndicatorLabelDecorator} (a background "Decoration Calculation" job) called
 * {@code getAST(cu, WAIT_YES, null)}.</li>
 * <li>{@code SelectionListenerWithASTManager} (a background "Requesting Java AST from selection"
 * job) also called {@code getAST(cu, WAIT_YES, null)}.</li>
 * </ul>
 * This test forces the same "reconcile started, never finishes" state directly and
 * deterministically (instead of racing a real reconciler thread, which the
 * {@code isReconciling(input)} check-then-wait in {@code getAST} makes unreliable to hit by timing
 * alone - see git history for the earlier, abandoned attempt), then issues the same three calls
 * concurrently and asserts none of them can be stuck forever.
 */
public class CoreASTProviderConcurrencyTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	/**
	 * CoreASTProvider's own overall reconcile timeout for this test, kept small so a deadlocked
	 * {@code getAST()} call gives up and returns almost immediately.
	 */
	private static final long CORE_AST_PROVIDER_TIMEOUT_MILLISECONDS= 50;

	/**
	 * Upper bound for the whole test.
	 */
	private static final long TEST_TIMEOUT_MILLISECONDS= 10_000;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	private CoreASTProvider fProvider;

	@Before
	public void setUp() throws Exception {
		System.setProperty(CoreASTProvider.CORE_AST_PROVIDER_RECONCILE_TIMEOUT_MILLISECONDS_PROPERTY, "" + CORE_AST_PROVIDER_TIMEOUT_MILLISECONDS);

		fJProject1= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fProvider= CoreASTProvider.getInstance();
	}

	@After
	public void tearDown() throws Exception {
		System.clearProperty(CoreASTProvider.CORE_AST_PROVIDER_RECONCILE_TIMEOUT_MILLISECONDS_PROPERTY);

		// CoreASTProvider is a process-wide singleton: leave it in a clean state for other tests.
		fProvider.setActiveJavaElement(null);
		fProvider.disposeAST();
		fProvider.clearReconciliation();
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testGetASTDoesNotHangWhenReconcileNeverCompletes() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
				package test1;
				public class E1 {
				}
				""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		fProvider.setActiveJavaElement(cu);

		// Simulate a reconcile job that started but will never finish (e.g. it crashed, threw, or
		// was silently cancelled along a path that skips CoreASTProvider#reconciled). Nothing will
		// ever call reconciled(...)/cache(...) for `cu` again, so isReconciling(cu) stays true and
		// fWaitLock is never notified for this element again - this is what makes the retry loop in
		// getAST() unbounded on the unfixed implementation.
		fProvider.aboutToBeReconciled(cu);
		assertTrue("Test setup problem: expected cu to be reported as reconciling", fProvider.isReconciling());

		@SuppressWarnings("resource") // The pool is going to be "closed" in the finally-block by calling pool.shutdownNow()
		ExecutorService pool= Executors.newFixedThreadPool(3);
		try {
			// The three real callers from the production freeze, same element, same flags.
			CompletableFuture<CompilationUnit> organizeImports= CompletableFuture.supplyAsync(
					() -> fProvider.getAST(cu, CoreASTProvider.WAIT_ACTIVE_ONLY, null), pool);
			CompletableFuture<CompilationUnit> decorationCalculation= CompletableFuture.supplyAsync(
					() -> fProvider.getAST(cu, CoreASTProvider.WAIT_YES, null), pool);
			CompletableFuture<CompilationUnit> selectionAst= CompletableFuture.supplyAsync(
					() -> fProvider.getAST(cu, CoreASTProvider.WAIT_YES, null), pool);

			CompletableFuture<Void> all= CompletableFuture.allOf(organizeImports, decorationCalculation, selectionAst);
			try {
				all.get(TEST_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				fail((!organizeImports.isDone() ? 1 : 0) + (!decorationCalculation.isDone() ? 1 : 0) + (!selectionAst.isDone() ? 1 : 0)
						+ " of 3 concurrent CoreASTProvider.getAST callers (OrganizeImportsAction/WAIT_ACTIVE_ONLY, "
						+ "Decoration Calculation/WAIT_YES, Requesting Java AST from selection/WAIT_YES) were still "
						+ "stuck after " + TEST_TIMEOUT_MILLISECONDS + "ms, waiting for a reconcile that "
						+ "was never meant to complete but should have been aborted after " + CORE_AST_PROVIDER_TIMEOUT_MILLISECONDS + "ms. This reproduces a real production hang: "
						+ "https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/3207");
			}

			assertTrue("OrganizeImportsAction's getAST(WAIT_ACTIVE_ONLY) call is still pending", organizeImports.isDone());
			assertTrue("Decoration Calculation's getAST(WAIT_YES) call is still pending", decorationCalculation.isDone());
			assertTrue("Requesting Java AST from selection's getAST(WAIT_YES) call is still pending", selectionAst.isDone());
			try {
				// Since all calls hit the timeout, they all end up receiving null.
				assertNull(organizeImports.get());
				assertNull(decorationCalculation.get());
				assertNull(selectionAst.get());
			} catch (ExecutionException e) {
				fail("getAST() failed unexpectedly: " + e.getCause());
			}
		} finally {
			// Interrupts any callers still stuck in fWaitLock.wait(...) so the test doesn't leak
			// threads even when it fails (getAST() treats InterruptedException as "return null").
			pool.shutdownNow();
		}
	}
}
