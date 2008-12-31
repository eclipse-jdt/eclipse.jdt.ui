/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.FailureTableDisplay;
import org.eclipse.jdt.internal.junit.ui.FailureTrace;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;

public class WrappingSystemTest extends TestCase implements ILaunchesListener2 {
	private boolean fLaunchHasTerminated = false;

	private IJavaProject fProject;

	public void launchesAdded(ILaunch[] launches) {
		// nothing
	}

	public void launchesChanged(ILaunch[] launches) {
		// nothing
	}

	public void launchesRemoved(ILaunch[] launches) {
		// nothing
	}

	public void launchesTerminated(ILaunch[] launches) {
		fLaunchHasTerminated = true;
	}

	public void test00characterizeSecondLine() throws Exception {
		runTests("\\n", 1000, 2, false);
		String text = getText(1);
		assertTrue(text, text.startsWith("Numbers"));
	}

	public void test01shouldWrapSecondLine() throws Exception {
		runTests("\\n", 1000, 2, false);
		String text = getText(1);
		assertTrue(text, text.length() < 300);
	}

	public void test02characterizeImages() throws Exception {
		runTests("\\n", 0, 3, true);
		assertEquals(getFailureTrace().getTrace(), getFailureDisplay().getExceptionIcon(), getImage(0));
		assertEquals(getFailureTrace().getTrace(), null, getImage(1));
		assertEquals(getFailureTrace().getTrace(), getFailureDisplay().getStackIcon(), getImage(2));
	}

	private FailureTableDisplay getFailureDisplay() throws PartInitException {
		return getFailureTrace().getFailureTableDisplay();
	}

	public void test03shouldWrapFirstLine() throws Exception {
		runTests("", 1000, 1, false);
		String text = getText(0);
		assertTrue(text, text.length() < 300);
	}

	private String getText(int i) throws PartInitException {
		return getTable().getItem(i).getText();
	}

	private FailureTrace getFailureTrace() throws PartInitException {
		TestRunnerViewPart viewPart = (TestRunnerViewPart) PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.showView(TestRunnerViewPart.NAME);
		return viewPart.getFailureTrace();
	}

	private Image getImage(int i) throws PartInitException {
		return getTable().getItem(i).getImage();
	}

	private Table getTable() throws PartInitException {
		return getFailureDisplay().getTable();
	}

	protected synchronized boolean hasNotTerminated() {
		return !fLaunchHasTerminated;
	}

	public void runTests(String prefixForErrorMessage,
			int howManyNumbersInErrorString, int numExpectedTableItems, boolean lastItemHasImage)
			throws CoreException, JavaModelException, PartInitException {
		launchTests(prefixForErrorMessage, howManyNumbersInErrorString);
		waitForTableToFill(numExpectedTableItems, 60000, lastItemHasImage);
	}

	protected void launchTests(String prefixForErrorMessage,
			int howManyNumbersInErrorString) throws CoreException,
			JavaModelException {
		// have to set up an 1.3 project to avoid requiring a 5.0 VM
		JavaProjectHelper.addRTJar13(fProject);
		JavaProjectHelper.addVariableEntry(fProject, new Path(
				"JUNIT_HOME/junit.jar"), null, null);

		IPackageFragmentRoot root = JavaProjectHelper.addSourceContainer(
				fProject, "src");
		IPackageFragment pack = root.createPackageFragment("pack", true, null);

		ICompilationUnit cu1 = pack.getCompilationUnit("LongTraceLines.java");

		String initialString = prefixForErrorMessage + "Numbers:";

		String initializeString = "String errorString = \"" + initialString
				+ "\";";

		String contents = "public class LongTraceLines extends TestCase {\n"
							+ "	public void testLongTraceLine() throws Exception {\n"
							+ ("		" + initializeString + "\n")
							+ ("		for (int i = 0; i < "
								+ howManyNumbersInErrorString + "; i++) {\n")
							+ "			errorString += \" \" + i;\n"
							+ "		}\n"
							+ "		throw new RuntimeException(errorString);\n"
							+ "	}\n"
							+ "}";

		IType type = cu1.createType(contents, null, true, null);
		cu1.createImport("junit.framework.TestCase", null, Flags.AccDefault,
				null);
		cu1.createImport("java.util.Arrays", null, Flags.AccDefault, null);

		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		lm.addLaunchListener(this);

		LaunchConfigurationManager manager = DebugUIPlugin.getDefault()
				.getLaunchConfigurationManager();
		List launchShortcuts = manager.getLaunchShortcuts();
		LaunchShortcutExtension ext = null;
		for (Iterator iter = launchShortcuts.iterator(); iter.hasNext();) {
			ext = (LaunchShortcutExtension) iter.next();
			if (ext.getLabel().equals("JUnit Test"))
				break;
		}
		ext.launch(new StructuredSelection(type), ILaunchManager.RUN_MODE);
	}

	protected void waitForTableToFill(int numExpectedTableLines,
			int millisecondTimeout, boolean lastItemHasImage) throws PartInitException {
		long startTime = System.currentTimeMillis();
		while (stillWaiting(numExpectedTableLines, lastItemHasImage)) {
			if (System.currentTimeMillis() - startTime > millisecondTimeout)
				fail("Timeout waiting for " + numExpectedTableLines
						+ " lines in table. Present: " + getNumTableItems() + " items.\n"
						+ "The 2nd vm has " + (hasNotTerminated() ? "not " : "") + "terminated.");
			dispatchEvents();
		}
	}

	protected void dispatchEvents() {
		PlatformUI.getWorkbench().getDisplay().readAndDispatch();
	}

	protected boolean stillWaiting(int numExpectedTableLines, boolean lastItemHasImage)
			throws PartInitException {
		return hasNotTerminated() || getNumTableItems() < numExpectedTableLines || (lastItemHasImage && getImage(numExpectedTableLines - 1) == null);
	}

	protected int getNumTableItems() throws PartInitException {
		return getTable().getItemCount();
	}

	protected void setUp() throws Exception {
		super.setUp();
		fProject = JavaProjectHelper.createJavaProject("a", "bin");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}
}
