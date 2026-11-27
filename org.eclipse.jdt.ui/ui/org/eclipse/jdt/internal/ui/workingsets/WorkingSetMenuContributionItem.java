/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceManager;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Menu contribution item which shows and lets select a working set.
 *
 * @since 2.0
 */
public class WorkingSetMenuContributionItem extends ContributionItem {

	private int fId;
	private IWorkingSet fWorkingSet;
	private WorkingSetFilterActionGroup fActionGroup;
	private final ResourceManager fResourceManager;

	/**
	 * Constructor for WorkingSetMenuContributionItem.
	 *
	 * @param id the id
	 * @param actionGroup the action group
	 * @param workingSet the working set
	 * @param resourceManager caches the working set image beyond this item
	 */
	public WorkingSetMenuContributionItem(int id, WorkingSetFilterActionGroup actionGroup, IWorkingSet workingSet, ResourceManager resourceManager) {
		super(getId(id));
		Assert.isNotNull(actionGroup);
		Assert.isNotNull(workingSet);
		Assert.isNotNull(resourceManager);
		fId= id;
		fActionGroup= actionGroup;
		fWorkingSet= workingSet;
		fResourceManager= resourceManager;
	}

	/*
	 * Overrides method from ContributionItem.
	 */
	@Override
	public void fill(Menu menu, int index) {
		MenuItem mi= new MenuItem(menu, SWT.RADIO, index);

		String name= fWorkingSet.getLabel();

		mi.setText("&" + fId + " " + name);  //$NON-NLS-1$  //$NON-NLS-2$
		ImageDescriptor imageDescriptor= fWorkingSet.getImageDescriptor();
		if (imageDescriptor != null)
			mi.setImage(fResourceManager.createImageWithDefault(imageDescriptor));
		mi.setSelection(fWorkingSet.equals(fActionGroup.getWorkingSet()));
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
				fActionGroup.setWorkingSet(fWorkingSet, true);
				manager.addRecentWorkingSet(fWorkingSet);
			}
		});
	}

	/*
	 * @see org.eclipse.jface.action.IContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}

	static String getId(int id) {
		return WorkingSetMenuContributionItem.class.getName() + "." + id;  //$NON-NLS-1$
	}
}
