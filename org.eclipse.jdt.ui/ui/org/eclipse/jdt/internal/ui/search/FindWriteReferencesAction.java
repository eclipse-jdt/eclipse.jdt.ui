/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindWriteReferencesAction extends FindReferencesAction {

	public FindWriteReferencesAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindWriteReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesAction.tooltip")); //$NON-NLS-1$
	}

	public FindWriteReferencesAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindWriteReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesAction.tooltip")); //$NON-NLS-1$
	}

	int getLimitTo() {
		return IJavaSearchConstants.WRITE_ACCESSES;
	}	

	String getOperationUnavailableMessage() {
		return SearchMessages.getString("JavaElementAction.operationUnavailable.field"); //$NON-NLS-1$
	}
}
