/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindReadReferencesAction extends FindReferencesAction {

	public FindReadReferencesAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindReadReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesAction.tooltip")); //$NON-NLS-1$
	}

	public FindReadReferencesAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindReadReferencesAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.READ_ACCESSES;
	}	
}
