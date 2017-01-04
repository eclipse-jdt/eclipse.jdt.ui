/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
