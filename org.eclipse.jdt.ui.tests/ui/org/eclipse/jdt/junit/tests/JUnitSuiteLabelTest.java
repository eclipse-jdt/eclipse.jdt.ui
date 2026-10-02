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

import java.io.File;
import java.lang.reflect.Method;

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
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.junit.ui.TestSessionLabelProvider;
import org.eclipse.jdt.internal.junit.ui.TestViewer;

public class JUnitSuiteLabelTest extends AbstractTestRunListenerTest {

	@Rule
	public TemporaryFolder fTemporaryFolder= new TemporaryFolder();

	private IWorkbenchPage fPage;
	private IWorkbenchPart fPreviousPart;
	private TestRunnerViewPart fView;
	private TestRunSession fPreviousSession;
	private TestRunSession fSession;
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
