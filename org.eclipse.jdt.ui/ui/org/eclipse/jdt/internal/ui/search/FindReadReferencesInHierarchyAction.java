/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindReadReferencesInHierarchyAction extends FindReferencesInHierarchyAction {

	public FindReadReferencesInHierarchyAction() {
		super(SearchMessages.getString("Search.FindReadReferencesInHierarchyAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.READ_ACCESSES;
	}	
}

