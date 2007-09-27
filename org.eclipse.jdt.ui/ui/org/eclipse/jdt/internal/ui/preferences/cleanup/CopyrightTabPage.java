/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.ui.fix.CopyrightUpdaterCleanUp;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;

public class CopyrightTabPage extends CleanUpTabPage {
	
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.copyright"; //$NON-NLS-1$

	public CopyrightTabPage() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	protected ICleanUp[] createPreviewCleanUps(Map values) {
		return new ICleanUp[] {new CopyrightUpdaterCleanUp(values)};
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group group= createGroup(numColumns, composite, "IBM Copyright"); //$NON-NLS-1$
		
		final CheckboxPreference format= createCheckboxPref(group, numColumns, "Update IBM copyright to current year", CopyrightUpdaterCleanUp.UPDATE_IBM_COPYRIGHT_TO_CURRENT_YEAR, CleanUpModifyDialog.FALSE_TRUE); //$NON-NLS-1$
		registerPreference(format);
	}

}
