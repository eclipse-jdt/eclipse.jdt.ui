/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public class FindWriteReferencesAction extends FindReferencesAction {

	public FindWriteReferencesAction() {
		super(SearchMessages.getString("Search.FindWriteReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.WRITE_ACCESSES;
	}	
}
