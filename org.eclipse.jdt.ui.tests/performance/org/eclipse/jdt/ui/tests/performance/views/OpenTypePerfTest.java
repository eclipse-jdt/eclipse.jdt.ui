/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.performance.views;

import static org.junit.Assert.fail;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.util.DisplayHelper;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Status;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;
import org.eclipse.jdt.ui.tests.performance.SWTTestProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenTypePerfTest extends JdtPerformanceTestCaseCommon {

	private SelectionDialog fOpenTypeDialog;
	private Shell fShell;

	private static class MyTestSetup extends ExternalResource {
		private SWTTestProject fTestProject;
		private boolean fAutoBuilding;

		@Override
		public void before() throws Throwable {
			fAutoBuilding= CoreUtility.setAutoBuilding(false);
			fTestProject= new SWTTestProject();
		}

		@Override
		public void after() {
			try {
				fTestProject.delete();
				CoreUtility.setAutoBuilding(fAutoBuilding);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Rule
	public MyTestSetup stup= new MyTestSetup();

	@Override
	public void setUp() throws Exception {
		System.out.println("starting " + OpenTypePerfTest.class.getName() + "#" + tn.getMethodName());
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	//---

	@Test
	public void testAColdException() throws Exception {
		//cold
		joinBackgroudActivities();
		try {
			measureOpenType("*Exception");
		} finally {
			commitMeasurements();
			assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
		}
	}

	@Test
	public void testBWarmException() throws Exception {
		//warm
		joinBackgroudActivities();
		try {
			measureOpenType("*Exception");
		} finally {
			commitMeasurements();
			Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 500);
		}
	}

	@Test
	public void testCWarmException10() throws Exception {
		//warm, repeated
		joinBackgroudActivities();
		try {
			for (int i= 0; i < 10; i++) {
				measureOpenType("*Exception");
			}
		} finally {
			commitMeasurements();
			assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
		}
	}

	@Test
	public void testDWarmS10() throws Exception {
		//warm, repeated, many matches
		joinBackgroudActivities();
		try {
			for (int i= 0; i < 10; i++) {
				measureOpenType("S");
			}
		} finally {
			commitMeasurements();
			assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
		}
	}

	@Test
	public void testEWarmOpenSWT() throws Exception {
		//warm, add SWT to history
		joinBackgroudActivities();
		try {
			measureOpenType("SWT", true);
		} finally {
			commitMeasurements();
			Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 500);
		}
	}

	@Test
	public void testFWarmOpenSWTHistory10() throws Exception {
		//warm, repeated, open SWT from history
		joinBackgroudActivities();
		try {
			for (int i= 0; i < 10; i++) {
				measureOpenType("SWT", true);
			}
		} finally {
			commitMeasurements();
			assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
		}
	}

	//---

	private void measureOpenType(String pattern) throws Exception {
		measureOpenType(pattern, false);
	}

	private void measureOpenType(String pattern, final boolean openFirst) throws Exception {
		fShell= JavaPlugin.getActiveWorkbenchShell();

		startMeasuring();

		fOpenTypeDialog= JavaUI.createTypeDialog(
				fShell,
				JavaPlugin.getActiveWorkbenchWindow(),
				SearchEngine.createWorkspaceScope(),
				IJavaElementSearchConstants.CONSIDER_ALL_TYPES,
				false,
				pattern,
				new TypeSelectionExtension() {
					@Override
					public ISelectionStatusValidator getSelectionValidator() {
						return selection -> {
							finish(openFirst);
							return Status.OK_STATUS;
						};
					}
				});

		try {
			fOpenTypeDialog.setBlockOnOpen(false);
			fOpenTypeDialog.open();
			new DisplayHelper() {
				@Override
				protected boolean condition() {
					return fOpenTypeDialog == null;
				}
			}.waitForCondition(fShell.getDisplay(), 60 * 1000, 10 * 1000);

		} finally {
			if (fOpenTypeDialog != null) {
				finish(openFirst);
				fail("took too long");
			}
		}
	}

	private void finish(final boolean openFirst) {
		final SelectionDialog openTypeDialog= fOpenTypeDialog;
		fOpenTypeDialog= null;

		if (! openFirst) {
			stopMeasuring();
			fShell.getDisplay().asyncExec(() -> openTypeDialog.close());

		} else {
			fShell.getDisplay().asyncExec(() -> {
				openTypeDialog.getOkButton().notifyListeners(SWT.Selection, new Event());
				stopMeasuring();
			});
		}
	}

}
