/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.IWorkingSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindWriteReferencesInWorkingSetAction extends FindReferencesInWorkingSetAction {

	public FindWriteReferencesInWorkingSetAction() {
		super(SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	public FindWriteReferencesInWorkingSetAction(IWorkingSet workingSet) {
		super(workingSet, new Class[] {IField.class});
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.WRITE_REFERENCES;
	}	
}

