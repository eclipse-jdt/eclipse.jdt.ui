/*
 * Copyright (c) 2000, 2002 IBM Corp. and others..
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Finds implementors of the selected element in working sets.
 * The action is applicable for selections representing a Java interface.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindImplementorsInWorkingSetAction extends FindImplementorsAction {

	private IWorkingSet[] fWorkingSets;

	/**
	 * Creates a new <code>FindImplementorsInWorkingSetAction</code>.
	 * The user will be prompted to select the working sets.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindImplementorsInWorkingSetAction(IWorkbenchSite site) {
		super(site);
		init();
	}

	/**
	 * Creates a new <code>FindImplementorsInWorkingSetAction</code>.
	 * 
	 * @param site			the site providing context information for this action
	 * @param workingSets	the working sets to be used in the search
	 */
	public FindImplementorsInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		this(site);
		fWorkingSets= workingSets;
	}

	/**
	 * Creates a new <code>FindImplementorsInWorkingSetAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindImplementorsInWorkingSetAction(JavaEditor editor) {
		super(editor);
		init();
	}

	/**
	 * Creates a new <code>FindImplementorsInWorkingSetAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindImplementorsInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		this(editor);
		fWorkingSets= workingSets;
	}


	private void init() {
		setText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
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

