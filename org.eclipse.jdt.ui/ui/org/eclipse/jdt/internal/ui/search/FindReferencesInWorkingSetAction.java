/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;;

public class FindReferencesInWorkingSetAction extends FindReferencesAction {

	private IWorkingSet[] fWorkingSets;
	
	public FindReferencesInWorkingSetAction(IWorkbenchSite site) {
		super(site);
		init();
	}

	public FindReferencesInWorkingSetAction(JavaEditor editor) {
		super(editor);
		init();
	}

	public FindReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		this(site);
		fWorkingSets= workingSets;
	}

	public FindReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		this(editor);
		fWorkingSets= workingSets;
	}

	private void init() {
		setText(SearchMessages.getString("Search.FindReferencesInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}


	FindReferencesInWorkingSetAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site, label, validTypes);
	}

	FindReferencesInWorkingSetAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
	}

	FindReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets, Class[] validTypes) {
		super(site, "", validTypes);  //$NON-NLS-1$
		fWorkingSets= workingSets;
	}

	FindReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets, Class[] validTypes) {
		super(editor, "", validTypes);  //$NON-NLS-1$
		fWorkingSets= workingSets;
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet[] workingSets= fWorkingSets;
		if (fWorkingSets == null) {
			workingSets= JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets == null)
				return null;
		}
		updateLRUWorkingSets(workingSets);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets), getCollector());
	};

	private IJavaSearchScope getScope(IWorkingSet[] workingSets) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(workingSets);
	}

	private String getScopeDescription(IWorkingSet[] workingSets) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {SearchUtil.toString(workingSets)}); //$NON-NLS-1$

	}
}
