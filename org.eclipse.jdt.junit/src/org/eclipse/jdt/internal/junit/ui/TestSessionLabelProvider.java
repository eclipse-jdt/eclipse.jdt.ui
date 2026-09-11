/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.ui;

import java.text.NumberFormat;

import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

public class TestSessionLabelProvider extends LabelProvider implements IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	private final NumberFormat timeFormat;

	private boolean fShowTime;
	private boolean fShowTimeDetails;

	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart, int layoutMode) {
		fTestRunnerPart= testRunnerPart;
		fLayoutMode= layoutMode;
		fShowTime= true;
		fShowTimeDetails= false;

		timeFormat= NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}

	@Override
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
					text= StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, text);
				}
			}

		} else {
			if (element instanceof TestCaseElement) {
				String decorated= getTextForFlatLayout((TestCaseElement) testElement, label);
				text= StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, text);
			}
		}
		return addTiming(text, testElement);
	}

	private String getTextForFlatLayout(TestCaseElement testCaseElement, String label) {
		String parentName;
		String parentDisplayName= testCaseElement.getParent().getDisplayName();
		if (parentDisplayName != null) {
			parentName= parentDisplayName;
		} else {
			if (testCaseElement.isDynamicTest()) {
				parentName= testCaseElement.getTestMethodName();
			} else {
				parentName= testCaseElement.getTestClassName();
			}
		}
		return Messages.format(JUnitMessages.TestSessionLabelProvider_testMethodName_className, new Object[] { label, BasicElementLabels.getJavaElementName(parentName) });
	}

	private StyledString addTiming(StyledString styledString, ITestElement testElement) {
		String string= styledString.getString();
		String decorated= addTiming(string, testElement);
		return StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.COUNTER_STYLER, styledString);
	}

	private String addTiming(String string, ITestElement testElement) {
		String decorated= string;
		double time= testElement.getElapsedTimeInSeconds();
		if (fShowTime && !Double.isNaN(time)) {
			String formattedTime= timeFormat.format(time);
			decorated= Messages.format(JUnitMessages.TestSessionLabelProvider_testName_elapsedTimeInSeconds, new String[] { string, formattedTime});
		}
		if (!fShowTimeDetails || !(testElement instanceof TestElement internalTestElement)) {
			return decorated;
		}

		double cpuTime= internalTestElement.getCpuTimeInSeconds();
		if (Double.isNaN(cpuTime)) {
			return decorated;
		}

		String userAndSystemDetails= ""; //$NON-NLS-1$
		double userTime= internalTestElement.getUserCpuTimeInSeconds();
		double systemTime= internalTestElement.getSystemCpuTimeInSeconds();
		if (!Double.isNaN(userTime) && !Double.isNaN(systemTime)) {
			userAndSystemDetails= Messages.format(JUnitMessages.TestSessionLabelProvider_userAndSystemTimeInSeconds,
					new String[] { timeFormat.format(userTime), timeFormat.format(systemTime) });
		}

		String nonCpuDetails= ""; //$NON-NLS-1$
		double nonCpuTime= internalTestElement.getNonCpuTimeInSeconds();
		if (!Double.isNaN(nonCpuTime)) {
			nonCpuDetails= Messages.format(JUnitMessages.TestSessionLabelProvider_nonCpuTimeInSeconds, timeFormat.format(nonCpuTime));
		}
		return Messages.format(JUnitMessages.TestSessionLabelProvider_testName_cpuTimeInSeconds,
				new String[] { decorated, timeFormat.format(cpuTime), userAndSystemDetails, nonCpuDetails });
	}

	private String getSimpleLabel(Object element) {
		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement= (TestCaseElement) element;
			String displayName= testCaseElement.getDisplayName();
			return BasicElementLabels.getJavaElementName(displayName != null ? displayName : testCaseElement.getTestMethodName());
		} else if (element instanceof TestSuiteElement) {
			TestSuiteElement testSuiteElement= (TestSuiteElement) element;
			String displayName= testSuiteElement.getDisplayName();
			return BasicElementLabels.getJavaElementName(displayName != null ? displayName : testSuiteElement.getSuiteTypeName());
		}
		return null;
	}

	@Override
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
			if (element instanceof TestCaseElement) {
				label= getTextForFlatLayout((TestCaseElement) testElement, label);
			}
		}
		return addTiming(label, testElement);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof TestElement && ((TestElement) element).isAssumptionFailure())
			return fTestRunnerPart.fTestAssumptionFailureIcon;

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

		}
		return null;
	}

	public void setShowTime(boolean showTime) {
		fShowTime= showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

	public void setShowTimeDetails(boolean showTimeDetails) {
		fShowTimeDetails= showTimeDetails;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
