/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindReadReferencesInWorkingSetAction extends FindReferencesInWorkingSetAction {

	public FindReadReferencesInWorkingSetAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	public FindReadReferencesInWorkingSetAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	public FindReadReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		super(site, workingSets, new Class[] {IField.class});
	}

	public FindReadReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		super(editor, workingSets, new Class[] {IField.class});
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.READ_ACCESSES;
	}	
}
