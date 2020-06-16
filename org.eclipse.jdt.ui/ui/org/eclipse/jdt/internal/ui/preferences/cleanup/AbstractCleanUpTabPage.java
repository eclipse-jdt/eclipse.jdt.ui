/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.MapCleanUpOptions;

public abstract class AbstractCleanUpTabPage extends CleanUpTabPage {

	private AbstractCleanUp[] fPreviewCleanUps;
	private Map<String, String> fValues;

	public AbstractCleanUpTabPage() {
		super();
	}

	protected abstract AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values);

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#setWorkingValues(java.util.Map)
	 */
	@Override
	public void setWorkingValues(Map<String, String> workingValues) {
		super.setWorkingValues(workingValues);
		fValues= workingValues;
		setOptions(new MapCleanUpOptions(workingValues));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#setOptions(org.eclipse.jdt.internal.ui.fix.CleanUpOptions)
	 */
	@Override
	public void setOptions(CleanUpOptions options) {
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#getPreview()
	 */
	@Override
	public String getPreview() {
		if (fPreviewCleanUps == null) {
			fPreviewCleanUps= createPreviewCleanUps(fValues);
		}

		StringBuilder buf= new StringBuilder();
		for (AbstractCleanUp fPreviewCleanUp : fPreviewCleanUps) {
			buf.append(fPreviewCleanUp.getPreview());
			buf.append("\n"); //$NON-NLS-1$ One free line between each code sample,
			// in most cases the preview variants should always have the same number of lines
		}
		return buf.toString();
	}

}
