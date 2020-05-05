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
 *     Gautier de Saint Martin Lacaze
 *         - [JUnit] Please add icon for "skipped" tests. https://bugs.eclipse.org/bugs/show_bug.cgi?id=509659
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.internal.junit.Messages;

import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * A panel with counters for the number of Runs, Errors and Failures.
 */
public class CounterPanel extends Composite {
	protected Text fNumberOfErrors;
	protected Text fNumberOfFailures;
	protected Text fNumberOfRuns;
	protected Text fNumberOfSkipped;
	protected int fTotal;
	protected int fAssumptionFailedCount;

	private final Image fSkippedIcon= JUnitPlugin.createImage("ovr16/ignore_optional_problems_ovr.png"); //$NON-NLS-1$
	private final Image fErrorIcon= JUnitPlugin.createImage("ovr16/error_ovr.png"); //$NON-NLS-1$
	private final Image fFailureIcon= JUnitPlugin.createImage("ovr16/failed_ovr.png"); //$NON-NLS-1$

	public CounterPanel(Composite parent) {
		super(parent, SWT.WRAP);
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 12;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		setLayout(gridLayout);

		fNumberOfRuns= createLabel(JUnitMessages.CounterPanel_label_runs, null, " 0/0  "); //$NON-NLS-1$
		fNumberOfSkipped= createLabel(JUnitMessages.CounterPanel_label_skipped, fSkippedIcon, " 0 "); //$NON-NLS-1$
		fNumberOfErrors= createLabel(JUnitMessages.CounterPanel_label_errors, fErrorIcon, " 0 "); //$NON-NLS-1$
		fNumberOfFailures= createLabel(JUnitMessages.CounterPanel_label_failures, fFailureIcon, " 0 "); //$NON-NLS-1$

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
	}

	private void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
	}

	private Text createLabel(String name, Image image, String init) {
		Label label= new Label(this, SWT.NONE);
		if (image != null) {
			image.setBackground(label.getBackground());
			label.setImage(image);
		}
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		label= new Label(this, SWT.NONE);
		label.setText(name);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		//label.setFont(JFaceResources.getBannerFont());

		Text value= new Text(this, SWT.READ_ONLY);
		value.setText(init);
		// bug: 39661 Junit test counters do not repaint correctly [JUnit]
		SWTUtil.fixReadonlyTextBackground(value);
		value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		return value;
	}

	public void reset() {
		setSkippedValue(0);
		setErrorValue(0);
		setFailureValue(0);
		setRunValue(0, 0);
		fTotal= 0;
	}

	public void setTotal(int value) {
		fTotal= value;
	}

	public int getTotal(){
		return fTotal;
	}

	public void setRunValue(int value, int assumptionFailureCount) {
		String runString;
		String runStringTooltip;
		if (assumptionFailureCount == 0) {
			runString= Messages.format(JUnitMessages.CounterPanel_runcount, new String[] { Integer.toString(value), Integer.toString(fTotal) });
			runStringTooltip= runString;
		} else {
			runString= Messages.format(JUnitMessages.CounterPanel_runcount_skipped, new String[] { Integer.toString(value), Integer.toString(fTotal), Integer.toString(assumptionFailureCount) });
			runStringTooltip= Messages.format(JUnitMessages.CounterPanel_runcount_assumptionsFailed, new String[] { Integer.toString(value), Integer.toString(fTotal), Integer.toString(assumptionFailureCount) });
		}
		fNumberOfRuns.setText(runString);
		fNumberOfRuns.setToolTipText(runStringTooltip);

		if (fAssumptionFailedCount == 0 && assumptionFailureCount > 0 || fAssumptionFailedCount != 0 && assumptionFailureCount == 0) {
			layout();
		} else {
			fNumberOfRuns.redraw();
			redraw();
		}
		fAssumptionFailedCount= assumptionFailureCount;
	}

	public void setSkippedValue(int value) {
		fNumberOfSkipped.setText(Integer.toString(value));
		redraw();
	}

	public void setErrorValue(int value) {
		fNumberOfErrors.setText(Integer.toString(value));
		redraw();
	}

	public void setFailureValue(int value) {
		fNumberOfFailures.setText(Integer.toString(value));
		redraw();
	}
}
