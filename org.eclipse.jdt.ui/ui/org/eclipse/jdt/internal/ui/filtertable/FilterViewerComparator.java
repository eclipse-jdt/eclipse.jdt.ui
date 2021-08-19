/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 * Note:
 *     Moved from org.eclipse.jdt.internal.debug.ui from the eclipse.jdt.debug project
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filtertable;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Comparator for {@link Filter} objects.
 * @since 3.26
 *
 */
public class FilterViewerComparator extends WorkbenchViewerComparator {
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		ILabelProvider lprov =
			(ILabelProvider) ((ContentViewer) viewer).getLabelProvider();
		String name1 = lprov.getText(e1);
		String name2 = lprov.getText(e2);
		if (name1 == null) {
			name1 = ""; //$NON-NLS-1$
		}
		if (name2 == null) {
			name2 = ""; //$NON-NLS-1$
		}
		if (name1.length() > 0 && name2.length() > 0) {
			char char1 = name1.charAt(name1.length() - 1);
			char char2 = name2.charAt(name2.length() - 1);
			if (char1 == '*' && char1 != char2) {
				return -1;
			}
			if (char2 == '*' && char2 != char1) {
				return 1;
			}
		}
		return name1.compareTo(name2);
	}
}
