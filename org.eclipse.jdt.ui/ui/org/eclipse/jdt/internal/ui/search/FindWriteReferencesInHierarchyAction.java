/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindWriteReferencesInHierarchyAction extends FindReferencesInHierarchyAction {

	public FindWriteReferencesInHierarchyAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
	}

	public FindWriteReferencesInHierarchyAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.WRITE_ACCESSES;
	}	
}
