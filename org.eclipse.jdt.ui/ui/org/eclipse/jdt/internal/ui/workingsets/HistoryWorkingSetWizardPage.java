/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.workingsets.dyn.WorkingSetManagerExt;


public class HistoryWorkingSetWizardPage extends AbstractWorkingSetWizardPage {

	private static final String PAGE_ID= "historyWorkingSetWizardPage"; //$NON-NLS-1$
	
	public HistoryWorkingSetWizardPage() {
		super(PAGE_ID, "History", null);
	}
	
	protected IWorkingSet createWorkingSet(String workingSetName) {
		return WorkingSetManagerExt.createDynamicWorkingSet(workingSetName, new HistoryWorkingSet());
	}
}
