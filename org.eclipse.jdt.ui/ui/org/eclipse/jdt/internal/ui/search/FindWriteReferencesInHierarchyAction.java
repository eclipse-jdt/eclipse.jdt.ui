/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindWriteReferencesInHierarchyAction extends FindReferencesInHierarchyAction {

	public FindWriteReferencesInHierarchyAction() {
		super(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.WRITE_REFERENCES;
	}	
}
