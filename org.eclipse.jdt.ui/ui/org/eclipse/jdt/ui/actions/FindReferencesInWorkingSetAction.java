/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchOperation;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;


/**
 * Finds references of the selected element in working sets.
 * The action is applicable to selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindReferencesInWorkingSetAction extends FindReferencesAction {

	private IWorkingSet[] fWorkingSets;
	
	/**
	 * Creates a new <code>FindReferencesInWorkingSetAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>. The user will 
	 * be prompted to select the working sets.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReferencesInWorkingSetAction(IWorkbenchSite site) {
		super(site);
		init();
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	/**
	 * Creates a new <code>FindReferencesInWorkingSetAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site			the site providing context information for this action
	 * @param workingSets	the working sets to be used in the search
	 */
	public FindReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		this(site);
		fWorkingSets= workingSets;
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReferencesInWorkingSetAction(JavaEditor editor) {
		super(editor);
		init();
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
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
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	FindReferencesInWorkingSetAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	FindReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets, Class[] validTypes) {
		super(site, "", validTypes);  //$NON-NLS-1$
		fWorkingSets= workingSets;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	FindReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets, Class[] validTypes) {
		super(editor, "", validTypes);  //$NON-NLS-1$
		fWorkingSets= workingSets;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKING_SET_ACTION);
	}

	JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet[] workingSets= fWorkingSets;
		if (fWorkingSets == null) {
			workingSets= JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets == null)
				return null;
		}
		SearchUtil.updateLRUWorkingSets(workingSets);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets), getCollector());
	}

	protected JavaSearchQuery createJob(IJavaElement element) throws JavaModelException {
		IWorkingSet[] workingSets= fWorkingSets;
		if (fWorkingSets == null) {
			workingSets= JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets == null)
				return null;
		}
		SearchUtil.updateLRUWorkingSets(workingSets);
		return new JavaSearchQuery(element, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets));
	}

	private IJavaSearchScope getScope(IWorkingSet[] workingSets) {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(workingSets);
	}

	private String getScopeDescription(IWorkingSet[] workingSets) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {SearchUtil.toString(workingSets)}); //$NON-NLS-1$

	}
}
