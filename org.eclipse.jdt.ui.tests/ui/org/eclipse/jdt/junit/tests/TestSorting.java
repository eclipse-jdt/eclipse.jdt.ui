/*******************************************************************************
 * Copyright (c) 2020 Sandra Lions and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [JUnit] allow to sort by name and by execution time - https://bugs.eclipse.org/bugs/show_bug.cgi?id=219466
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart.SortingCriterion;

public class TestSorting extends AbstractTestRunListenerTest {
	private String[] runSequenceTest(IType typeToLaunch) throws Exception {
		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			return launchJUnit(typeToLaunch, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	@Test
	public void testSorting() throws Exception {
		IWorkbenchPage activePage= JUnitPlugin.getActivePage();
		TestRunnerViewPart testRunnerViewPart= (TestRunnerViewPart)activePage.showView(TestRunnerViewPart.NAME);
		testRunnerViewPart.setLayoutMode(TestRunnerViewPart.LAYOUT_FLAT); // TableViewer

		String source= """
			package pack;
			import junit.framework.TestCase;
			public class ATestCase extends TestCase {
				private String fString;
				public void testB_FirstTest() throws Exception {
				    fString= "first";
				    Thread.sleep(30);
				}
				public void testa_SecondTest() throws Exception {
				    fString= "second";
				    Thread.sleep(50);
				}
				public void testC_ThirdTest() throws Exception {
				    fString= "second";
				    Thread.sleep(50);
				}
				public void testA_FourthTest() throws Exception {
				    fString= "third";
				    Thread.sleep(40);
				}
			}""";

		IType aTestCase= createType(source, "pack", "ATestCase.java");
		runSequenceTest(aTestCase);

		Table table= ((TableViewer)testRunnerViewPart.getTestViewer().getActiveViewer()).getTable();
		assertEquals(4, table.getItemCount());

		List<String> testResults;

		assertFalse(testRunnerViewPart.getTestRunSession().isRunning());
		testRunnerViewPart.setSortingCriterion(SortingCriterion.SORT_BY_NAME);
		testResults= new ArrayList<>();
		for (int i= 0; i < table.getItems().length; i++) {
			String text= table.getItems()[i].getText();
			testResults.add(i, text.substring(0, text.indexOf("_")));
		}
		assertArrayEquals(new String[] { "testA", "testa", "testB", "testC" }, testResults.toArray());

		testRunnerViewPart.setSortingCriterion(SortingCriterion.SORT_BY_EXECUTION_TIME);
		testResults= new ArrayList<>();
		for (TableItem tableItem : table.getItems()) {
			String text= tableItem.getText();
			testResults.add(0, text.substring(text.indexOf("(") + 1, text.length()));
		}
		String previousResult= null;
		for (String testResult : testResults) {
			if (previousResult != null) {
				assertTrue(previousResult.compareTo(testResult) <= 0);
			}
			previousResult= testResult;
		}
	}
}