/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.junit.ui.TestSessionLabelProvider;
import org.eclipse.jdt.internal.junit.ui.TestViewer;
import org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader;

public class JUnitSuiteLabelTest extends AbstractTestRunListenerTest {

	@Rule
	public TemporaryFolder fTemporaryFolder= new TemporaryFolder();

	private IWorkbenchPage fPage;
	private IWorkbenchPart fPreviousPart;
	private TestRunnerViewPart fView;
	private TestRunSession fPreviousSession;
	private TestRunSession fSession;
	private ITestRunListener2 fProtocol;
	private IType fSuiteType;
	private IType fTestType;
	private boolean fViewWasOpen;
	private int fPreviousLayout;

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("JUnitSuiteLabelTest", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		JavaProjectHelper.addRTJar18(fProject);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		fSuiteType= createType("package pack; public class NamedSuite {}", "pack", "NamedSuite.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fTestType= createType("package pack; public class SampleTest { public void testExample() {} }", "pack", "SampleTest.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fPage= JUnitPlugin.getActivePage();
		fPreviousPart= fPage.getActivePart();
		fViewWasOpen= fPage.findView(TestRunnerViewPart.NAME) != null;
		fView= (TestRunnerViewPart) fPage.showView(TestRunnerViewPart.NAME);
		fPreviousSession= fView.getTestRunSession();
		fPreviousLayout= fView.getTestViewer().getActiveViewer() instanceof TreeViewer
				? TestRunnerViewPart.LAYOUT_HIERARCHICAL : TestRunnerViewPart.LAYOUT_FLAT;
		fSession= new TestRunSession("Suite labels", fProject); //$NON-NLS-1$
		activate(fSession);
	}

	@After
	public void restoreView() throws Exception {
		if (fView != null) {
			if (fProtocol != null) {
				fProtocol.testRunEnded(0);
			}
			DisplayHelper.driveEventQueue(Display.getCurrent());
			activate(fPreviousSession);
			fView.setLayoutMode(fPreviousLayout);
			if (!fViewWasOpen) {
				fPage.hideView(fView);
			}
			if (fPreviousPart != null) {
				fPage.activate(fPreviousPart);
			}
			DisplayHelper.driveEventQueue(Display.getCurrent());
		}
	}

	@Test
	public void testDisplayNameAndClassTooltipInBothLayouts() throws Exception {
		TestSuiteElement suite= suite("pack.NamedSuite", "Readable suite (with punctuation)"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int layout : new int[] { TestRunnerViewPart.LAYOUT_HIERARCHICAL, TestRunnerViewPart.LAYOUT_FLAT }) {
			fView.setLayoutMode(layout);
			fView.getTestViewer().processChangesInUI();
			CellLabelProvider provider= (CellLabelProvider) fView.getTestViewer().getActiveViewer().getLabelProvider();
			assertEquals("pack.NamedSuite", provider.getToolTipText(suite)); //$NON-NLS-1$
			TestSessionLabelProvider labels= new TestSessionLabelProvider(fView, layout);
			try {
				labels.setShowTime(false);
				assertEquals("Readable suite (with punctuation)", labels.getText(suite)); //$NON-NLS-1$
			} finally {
				labels.dispose();
			}
		}
	}

	@Test
	public void testMissingDisplayNameFallsBackToClassNameWithoutDuplicateTooltip() {
		TestSuiteElement suite= suite("pack.NamedSuite", null); //$NON-NLS-1$
		TestSessionLabelProvider labels= new TestSessionLabelProvider(fView, TestRunnerViewPart.LAYOUT_HIERARCHICAL);
		try {
			labels.setShowTime(false);
			assertEquals("pack.NamedSuite", labels.getText(suite)); //$NON-NLS-1$
		} finally {
			labels.dispose();
		}
		CellLabelProvider provider= (CellLabelProvider) fView.getTestViewer().getActiveViewer().getLabelProvider();
		assertNull(provider.getToolTipText(suite));
	}

	@Test
	public void testUnmappedGroupDoesNotShowAnInventedClass() {
		CellLabelProvider provider= (CellLabelProvider) fView.getTestViewer().getActiveViewer().getLabelProvider();
		assertNull(provider.getToolTipText(suite("Unmapped group", "Readable group"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testOpeningNamedSuiteUsesItsOwnClassBeforeItsFirstChild() throws Exception {
		TestSuiteElement suite= suite("pack.NamedSuite", "Readable suite (with punctuation)"); //$NON-NLS-1$ //$NON-NLS-2$
		new TestCaseElement(suite, "child", "testExample(pack.SampleTest)", null, false, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertOpens(suite, fSuiteType);
	}

	@Test
	public void testOpeningClassNamedSuiteUsesItsOwnClassBeforeItsFirstChild() throws Exception {
		startRuntimeSession(1);
		TestSuiteElement suite= (TestSuiteElement) addTreeEntry("suite,pack.NamedSuite,true,1,false,-1,pack.NamedSuite,,"); //$NON-NLS-1$
		assertNull(suite.getDisplayName());
		addTreeEntry("child,testExample(pack.SampleTest),false,1,false,-1,testExample(pack.SampleTest),,"); //$NON-NLS-1$
		assertOpens(suite, fSuiteType);
	}

	@Test
	public void testEmptySuiteRuntimeTreeOpensItsClass() throws Exception {
		assertEmptyRuntimeSuiteOpens(JUnit4SuiteSourceTest.EmptySuite.class);
	}

	@Test
	public void testMethodLikeEmptySuiteRuntimeTreeOpensItsClass() throws Exception {
		assertEmptyRuntimeSuiteOpens(JUnit4SuiteSourceTest.MethodLikeEmptySuite.class);
	}

	private void assertEmptyRuntimeSuiteOpens(Class<?> suiteClass) throws Exception {
		IType outer= createType("package org.eclipse.jdt.junit.tests; public class JUnit4SuiteSourceTest { public static class " //$NON-NLS-1$
				+ suiteClass.getSimpleName() + " {} }", "org.eclipse.jdt.junit.tests", "JUnit4SuiteSourceTest.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ITestReference test= new JUnit4TestLoader().loadTests(new Class<?>[] { suiteClass }, null, null, null, null, null, null)[0];
		List<String> entries= new ArrayList<>();
		test.sendTree(new RemoteTestRunner() {
			@Override
			protected void notifyTestTreeEntry(String treeEntry) {
				entries.add(treeEntry);
			}
		});
		assertEquals(1, entries.size());
		startRuntimeSession(test.countTestCases());
		TestElement element= addTreeEntry(entries.get(0));
		assertTrue("The runtime tree must create a suite in the session model", element instanceof TestSuiteElement); //$NON-NLS-1$
		TestSuiteElement suite= (TestSuiteElement) element;
		assertEquals(0, suite.getChildren().length);
		assertEquals(0, test.countTestCases());
		assertOpens(suite, outer.getType(suiteClass.getSimpleName()));
	}

	private void startRuntimeSession(int count) throws Exception {
		Class<?> notifier= Class.forName(TestRunSession.class.getName() + "$TestSessionNotifier"); //$NON-NLS-1$
		Constructor<?> constructor= notifier.getDeclaredConstructor(TestRunSession.class);
		constructor.setAccessible(true);
		fProtocol= (ITestRunListener2) constructor.newInstance(fSession);
		fProtocol.testRunStarted(count);
	}

	private TestElement addTreeEntry(String entry) {
		fProtocol.testTreeEntry(entry);
		return fSession.getTestElement(entry.substring(0, entry.indexOf(',')));
	}

	@Test
	public void testParameterizedGroupStillOpensItsChild() throws Exception {
		TestSuiteElement group= suite("[0]", null); //$NON-NLS-1$
		new TestCaseElement(group, "child", "testExample[0](pack.SampleTest)", null, false, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		assertOpens(group, fTestType);
	}

	@Test
	public void testExportAndImportPreserveBothNames() throws Exception {
		suite("pack.NamedSuite", "Readable suite (with punctuation)"); //$NON-NLS-1$ //$NON-NLS-2$
		File file= fTemporaryFolder.newFile("suite.xml"); //$NON-NLS-1$
		JUnitModel.exportTestRunSession(fSession, file);
		TestRunSession imported= JUnitModel.importTestRunSession(file);
		try {
			TestSuiteElement suite= (TestSuiteElement) imported.getChildren()[0];
			assertEquals("pack.NamedSuite", suite.getSuiteTypeName()); //$NON-NLS-1$
			assertEquals("Readable suite (with punctuation)", suite.getDisplayName()); //$NON-NLS-1$
		} finally {
			JUnitCorePlugin.getModel().removeTestRunSession(imported);
			DisplayHelper.driveEventQueue(Display.getCurrent());
		}
	}

	private TestSuiteElement suite(String name, String displayName) {
		return new TestSuiteElement(fSession.getTestRoot(), "suite", name, 1, displayName, null, null); //$NON-NLS-1$
	}

	private void activate(TestRunSession session) throws Exception {
		Method method= TestRunnerViewPart.class.getDeclaredMethod("setActiveTestRunSession", TestRunSession.class); //$NON-NLS-1$
		method.setAccessible(true);
		method.invoke(fView, session);
	}

	private void assertOpens(TestSuiteElement suite, IType expectedType) throws Exception {
		fView.setLayoutMode(TestRunnerViewPart.LAYOUT_HIERARCHICAL);
		TestViewer viewer= fView.getTestViewer();
		viewer.registerActiveSession(fSession);
		viewer.processChangesInUI();
		viewer.getActiveViewer().setSelection(new StructuredSelection(suite));
		Method method= TestViewer.class.getDeclaredMethod("handleDefaultSelected"); //$NON-NLS-1$
		method.setAccessible(true);
		method.invoke(viewer);
		IEditorPart editor= fPage.getActiveEditor();
		assertNotNull(editor);
		try {
			assertEquals(expectedType.getCompilationUnit(), JavaUI.getEditorInputJavaElement(editor.getEditorInput()));
		} finally {
			fPage.closeEditor(editor, false);
		}
	}
}
