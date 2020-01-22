/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.util.JDTUIHelperClasses;

/**
 * A label provider for basic elements like paths. The label provider will make sure that the labels are correctly
 * shown in RTL environments.
 *
 * @see JDTUIHelperClasses
 * @since 3.4
 */
public class BasicElementLabels extends org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels {

	/**
	 * Returns a label for a working set
	 *
	 * @param set the working set
	 * @return the label of the working set
	 */
	public static String getWorkingSetLabel(IWorkingSet set) {
		return Strings.markLTR(set.getLabel());
	}


}
