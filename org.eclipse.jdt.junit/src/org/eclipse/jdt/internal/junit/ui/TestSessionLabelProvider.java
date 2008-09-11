/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.ui;

import java.text.NumberFormat;

import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;

import org.eclipse.jdt.internal.ui.viewsupport.ColoringLabelProvider;

public class TestSessionLabelProvider extends LabelProvider implements IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	private final NumberFormat timeFormat;

	private boolean fShowTime;

	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart, int layoutMode) {
		fTestRunnerPart= testRunnerPart;
		fLayoutMode= layoutMode;
		fShowTime= true;

		timeFormat= NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}

	public StyledString getStyledText(Object element) {
		String label= getSimpleLabel(element);
		if (label == null) {
			return new StyledString(element.toString());
		}
		StyledString text= new StyledString(label);

		ITestElement testElement= (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			if (testElement.getParentContainer() instanceof ITestRunSession) {
				String testKindDisplayName= fTestRunnerPart.getTestKindDisplayName();
				if (testKindDisplayName != null) {
					String decorated= Messages.format(JUnitMessages.TestSessionLabelProvider_testName_JUnitVersion, new Object[] { label, testKindDisplayName });
					text= ColoringLabelProvider.decorateStyledString(text, decorated, StyledString.QUALIFIER_STYLER);
				}
			}

		} else {
			if (element instanceof ITestCaseElement) {
				String className= BasicElementLabels.getJavaElementName(((ITestCaseElement) element).getTestClassName());
				String decorated= Messages.format(JUnitMessages.TestSessionLabelProvider_testMethodName_className, new Object[] { label, className });
				text= ColoringLabelProvider.decorateStyledString(text, decorated, StyledString.QUALIFIER_STYLER);
			}
		}
		return addElapsedTime(text, testElement.getElapsedTimeInSeconds());
	}

	private StyledString addElapsedTime(StyledString styledString, double time) {
		String string= styledString.getString();
		String decorated= addElapsedTime(string, time);
		return ColoringLabelProvider.decorateStyledString(styledString, decorated, StyledString.COUNTER_STYLER);
	}

	private String addElapsedTime(String string, double time) {
		if (!fShowTime || Double.isNaN(time)) {
			return string;
		}
		String formattedTime= timeFormat.format(time);
		return Messages.format(JUnitMessages.TestSessionLabelProvider_testName_elapsedTimeInSeconds, new String[] { string, formattedTime});
	}

	private String getSimpleLabel(Object element) {
		if (element instanceof ITestCaseElement) {
			return BasicElementLabels.getJavaElementName(((ITestCaseElement) element).getTestMethodName());
		} else if (element instanceof ITestSuiteElement) {
			return BasicElementLabels.getJavaElementName(((ITestSuiteElement) element).getSuiteTypeName());
		}
		return null;
	}

	public String getText(Object element) {
		String label= getSimpleLabel(element);
		if (label == null) {
			return element.toString();
		}
		ITestElement testElement= (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			if (testElement.getParentContainer() instanceof ITestRunSession) {
				String testKindDisplayName= fTestRunnerPart.getTestKindDisplayName();
				if (testKindDisplayName != null) {
					label= Messages.format(JUnitMessages.TestSessionLabelProvider_testName_JUnitVersion, new Object[] { label, testKindDisplayName });
				}
			}
		} else {
			if (element instanceof ITestCaseElement) {
				String className=  BasicElementLabels.getJavaElementName(((ITestCaseElement) element).getTestClassName());
				label= Messages.format(JUnitMessages.TestSessionLabelProvider_testMethodName_className, new Object[] { label, className });
			}
		}
		return addElapsedTime(label, testElement.getElapsedTimeInSeconds());
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

	public void setShowTime(boolean showTime) {
		fShowTime= showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
