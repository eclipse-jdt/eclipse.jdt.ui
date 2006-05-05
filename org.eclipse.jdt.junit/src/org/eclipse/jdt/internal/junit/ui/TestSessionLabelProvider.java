/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;

public class TestSessionLabelProvider extends LabelProvider {
	
	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	
	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart, int layoutMode) {
		fTestRunnerPart= testRunnerPart;
		fLayoutMode= layoutMode;
	}
	
	public String getText(Object element) {
		if (element instanceof TestCaseElement) {
			String testMethodName= ((TestCaseElement) element).getTestMethodName();
			if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
				return testMethodName;
			} else {
				String className= ((TestCaseElement) element).getClassName();
				return Messages.format(JUnitMessages.TestSessionLabelProvider_testMethodName_className, new Object[] { testMethodName, className });
			}
			
		} else if (element instanceof TestElement) {
			return ((TestElement) element).getTestName();
			
		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}
	
	public Image getImage(Object element) {
		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement= ((TestCaseElement) element);
			if (testCaseElement.getTestName().startsWith("@Ignore: ")) //$NON-NLS-1$
				return fTestRunnerPart.fTestIcon;
			
			Status status=testCaseElement.getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fTestIcon;
			else if (status.isError())
				return fTestRunnerPart.fTestErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fTestFailIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fTestRunningIcon;
			else if (status.isOK())
				return fTestRunnerPart.fTestOkIcon;
			else
				throw new IllegalStateException(element.toString());
			
		} else if (element instanceof TestSuiteElement) {
			Status status= ((TestSuiteElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fSuiteIcon;
			else if (status.isError())
				return fTestRunnerPart.fSuiteErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fSuiteFailIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fSuiteRunningIcon;
			else if (status.isOK())
				return fTestRunnerPart.fSuiteOkIcon;
			else
				throw new IllegalStateException(element.toString());
		
		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}
}
