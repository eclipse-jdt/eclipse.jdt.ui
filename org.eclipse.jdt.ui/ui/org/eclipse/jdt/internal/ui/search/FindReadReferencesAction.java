/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindReadReferencesAction extends FindReferencesAction {

	public FindReadReferencesAction() {
		super(SearchMessages.getString("Search.FindReadReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.READ_ACCESSES;
	}	
}
