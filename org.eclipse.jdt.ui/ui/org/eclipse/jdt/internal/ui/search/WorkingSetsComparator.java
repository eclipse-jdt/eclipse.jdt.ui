/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.search;

import java.util.Comparator;

import com.ibm.icu.text.Collator;

import org.eclipse.ui.IWorkingSet;

class WorkingSetsComparator implements Comparator<IWorkingSet[]> {

	private Collator fCollator= Collator.getInstance();

	/*
	 * @see Comparator#compare(Object, Object)
	 */
	@Override
	public int compare(IWorkingSet[] w1, IWorkingSet[] w2) {
		return fCollator.compare(w1[0].getLabel(), w2[0].getLabel());
	}
}
