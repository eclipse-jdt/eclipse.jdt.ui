/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;

/**
 * @since 3.5
 */
public class ContributedCleanUpTabPage extends CleanUpTabPage {

	private final ICleanUpTabPage fContribution;

	public ContributedCleanUpTabPage(ICleanUpTabPage contribution) {
		fContribution= contribution;
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#setOptionsKind(int)
	 */
	public void setOptionsKind(int kind) {
		super.setOptionsKind(kind);

		fContribution.setOptionsKind(kind);
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#setWorkingValues(java.util.Map)
	 */
	public void setWorkingValues(Map workingValues) {
		super.setWorkingValues(workingValues);

		fContribution.setOptions(new CleanUpOptions(workingValues) {
			/* 
			 * @see org.eclipse.jdt.internal.ui.fix.CleanUpOptions#setOption(java.lang.String, java.lang.String)
			 */
			public void setOption(String key, String value) {
				super.setOption(key, value);

				doUpdatePreview();
				notifyValuesModified();
			}
		});
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#setOptions(org.eclipse.jdt.internal.ui.fix.CleanUpOptions)
	 */
	public void setOptions(CleanUpOptions options) {
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreatePreferences(org.eclipse.swt.widgets.Composite, int)
	 */
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Composite parent= new Composite(composite, SWT.NONE);
		GridData layoutData= new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.horizontalSpan= numColumns;
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout(1, false));

		fContribution.createContents(parent);
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#getPreview()
	 */
	public String getPreview() {
		return fContribution.getPreview();
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#getSelectedCleanUpCount()
	 */
	public int getSelectedCleanUpCount() {
		return fContribution.getSelectedCleanUpCount();
	}

	/* 
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#getCleanUpCount()
	 */
	public int getCleanUpCount() {
		return fContribution.getCleanUpCount();
	}

}
