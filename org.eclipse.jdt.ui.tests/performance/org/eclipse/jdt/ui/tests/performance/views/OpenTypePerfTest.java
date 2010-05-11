/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.OrderedTestSuite;
import org.eclipse.jdt.testplugin.util.DisplayHelper;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;
import org.eclipse.jdt.ui.tests.performance.SWTTestProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class OpenTypePerfTest extends JdtPerformanceTestCase {

	private SelectionDialog fOpenTypeDialog;
	private Shell fShell;

	private static class MyTestSetup extends TestSetup {
		private SWTTestProject fTestProject;
		private boolean fAutoBuilding;

		public MyTestSetup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
			super.setUp();
			fAutoBuilding= CoreUtility.setAutoBuilding(false);
			fTestProject= new SWTTestProject();
		}

		protected void tearDown() throws Exception {
			fTestProject.delete();
			CoreUtility.setAutoBuilding(fAutoBuilding);
			super.tearDown();
		}
	}

	public static Test suite() throws Exception {
		OrderedTestSuite testSuite= new OrderedTestSuite(
				OpenTypePerfTest.class,
				new String[] {
					"testColdException",
					"testWarmException",
					"testWarmException10",
					"testWarmS10",
					"testWarmOpenSWT",
					"testWarmOpenSWTHistory10",
				});
		return new MyTestSetup(testSuite);
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	public  OpenTypePerfTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		System.out.println("starting " + OpenTypePerfTest.class.getName() + "#" + getName());
		super.setUp();
	}

	//---

	public void testColdException() throws Exception {
		//cold
		joinBackgroudActivities();
		try {
			measureOpenType("*Exception");
		} finally {
			commitMeasurements();
			assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
		}
	}

	public void testWarmException() throws Exception {
		//warm
		joinBackgroudActivities();
		try {
			measureOpenType("*Exception");
		} finally {
			commitMeasurements();
			Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 500);
		}
	}

	public void testWarmException10() throws Exception {
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

	public void testWarmS10() throws Exception {
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

	public void testWarmOpenSWT() throws Exception {
		//warm, add SWT to history
		joinBackgroudActivities();
		try {
			measureOpenType("SWT", true);
		} finally {
			commitMeasurements();
			Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 500);
		}
	}

	public void testWarmOpenSWTHistory10() throws Exception {
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
					public ISelectionStatusValidator getSelectionValidator() {
						return new ISelectionStatusValidator() {
							public IStatus validate(Object[] selection) {
								finish(openFirst);
								return Status.OK_STATUS;
							}
						};
					}
				});

		try {
			fOpenTypeDialog.setBlockOnOpen(false);
			fOpenTypeDialog.open();
			new DisplayHelper() {
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
			fShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					openTypeDialog.close();
				}
			});

		} else {
			fShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					openTypeDialog.getOkButton().notifyListeners(SWT.Selection, new Event());
					stopMeasuring();
				}
			});
		}
	}

}
