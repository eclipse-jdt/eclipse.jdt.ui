/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class RefactoringStatusContentProvider implements IStructuredContentProvider{

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object obj) {
			return ((RefactoringStatus)obj).getEntries().toArray();
		}
}

