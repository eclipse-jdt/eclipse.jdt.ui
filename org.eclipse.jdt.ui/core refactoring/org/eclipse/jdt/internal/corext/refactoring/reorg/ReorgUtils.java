/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IWorkingSet;


public class ReorgUtils extends ReorgUtilsCore {
	private ReorgUtils() {
	}


	/**
	 * Checks whether the given object is a working set.
	 *
	 * @param element the element to test
	 * @return <code>true</code> if the element is a working set, <code>false</code> otherwise
	 * @since 3.5
	 */

	public static boolean isWorkingSet(Object element){
		return (element instanceof IWorkingSet);
	}

	/**
	 * Checks whether the given list contains only working sets.
	 *
	 * @param elements the list with elements to check
	 * @return <code>true</code> if the list contains only working sets, <code>false</code>
	 *         otherwise
	 * @since 3.5
	 */
	public static boolean containsOnlyWorkingSets (List<?> elements){
		if (elements.isEmpty())
			return false;
		for (Object name : elements) {
			if (!isWorkingSet(name))
				return false;
		}
		return true;
	}


	public static IWorkingSet[] getWorkingSets(List<?> elements) {
		List<IWorkingSet> result= new ArrayList<>(1);
		for (Object element : elements) {
			if (element instanceof IWorkingSet) {
				result.add((IWorkingSet) element);
			}
		}
		return result.toArray(new IWorkingSet[result.size()]);
	}
}
