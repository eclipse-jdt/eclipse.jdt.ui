/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.IWorkingSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindReadReferencesInWorkingSetAction extends FindReferencesInWorkingSetAction {

	public FindReadReferencesInWorkingSetAction() {
		super(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	public FindReadReferencesInWorkingSetAction(IWorkingSet workingSet) {
		super(workingSet, new Class[] {IField.class});
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.READ_REFERENCES;
	}	
}
