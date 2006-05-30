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
import org.eclipse.jdt.internal.junit.model.TestRoot;
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
			TestCaseElement testCaseElement= (TestCaseElement) element;
			String testMethodName= testCaseElement.getTestMethodName();
			if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
				return getElementLabel(testMethodName, testCaseElement);
			} else {
				String className= testCaseElement.getClassName();
				return Messages.format(JUnitMessages.TestSessionLabelProvider_testMethodName_className, new Object[] { testMethodName, className });
			}
			
		} else if (element instanceof TestElement) {
			TestElement testElement= (TestElement) element;
			String testName= testElement.getTestName();
			return getElementLabel(testName, testElement);
		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}

	private String getElementLabel(String name, TestElement testElement) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL && testElement.getParent() instanceof TestRoot) {
			String testKindDisplayName= fTestRunnerPart.getTestKindDisplayName();
				if (testKindDisplayName == null)
					return name;
				else
					return Messages.format(JUnitMessages.TestSessionLabelProvider_testName_JUnitVersion, new Object[] { name, testKindDisplayName });
		} else
			return name;
		
	}

	public Image getImage(Object element) {
		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement= ((TestCaseElement) element);
			if (testCaseElement.isIgnored())
				return fTestRunnerPart.fTestIgnoredIcon;
			
			Status status=testCaseElement.getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fTestIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fTestRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fTestErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fTestFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fTestOkIcon;
			else
				throw new IllegalStateException(element.toString());
			
		} else if (element instanceof TestSuiteElement) {
			Status status= ((TestSuiteElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fSuiteIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fSuiteRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fSuiteErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fSuiteFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fSuiteOkIcon;
			else
				throw new IllegalStateException(element.toString());
		
		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}
}
