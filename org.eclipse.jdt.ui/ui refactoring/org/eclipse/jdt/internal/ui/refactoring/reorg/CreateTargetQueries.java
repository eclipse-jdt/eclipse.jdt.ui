/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchWizard;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICreateTargetQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICreateTargetQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICreateTargetQuery.ICreatedTarget;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewPackageCreationWizard;

import org.eclipse.jdt.ui.wizards.NewPackageWizardPage;

public class CreateTargetQueries implements ICreateTargetQueries {

	private static class CreatedTarget implements ICreatedTarget {
		private final Object fNewElement;
		private final Object fParentOfNew;
		
		public CreatedTarget(Object newElement, Object parentOfNew) {
			fNewElement= newElement;
			fParentOfNew= parentOfNew;
		}
		public Object getNewElement() {
			return fNewElement;
		}
		public Object getParentOfNew() {
			return fParentOfNew;
		}
		
	}
	
	private final Wizard fWizard;
	private final Shell fShell;

//	public CreateTargetQueries() {
//		fShell= null;
//		fWizard= null;
//	}
	
	public CreateTargetQueries(Wizard wizard) {
		fWizard= wizard;
		fShell= null;
	}
	
	public CreateTargetQueries(Shell shell) {
		fShell = shell;
		fWizard= null;
	}

	private Shell getShell() {
		Assert.isTrue(fWizard == null || fShell == null);
		if (fWizard != null)
			return fWizard.getContainer().getShell();
		else if (fShell != null)
			return fShell;
		else
			return JavaPlugin.getActiveWorkbenchShell();
	}
	
	public ICreateTargetQuery createNewPackageQuery() {
		return new ICreateTargetQuery() {
			public ICreatedTarget getCreatedTarget(Object selection) {
				IWorkbenchWizard packageCreationWizard= new NewPackageCreationWizard();
				
				IWizardPage[] pages= openNewElementWizard(packageCreationWizard, getShell(), selection);
				
				Assert.isTrue(pages.length > 0 && pages[0] instanceof NewPackageWizardPage);
				NewPackageWizardPage page= (NewPackageWizardPage) pages[0];
				IPackageFragment newPackageFragment= page.getNewPackageFragment();
				if (newPackageFragment != null) {
					return new CreatedTarget(newPackageFragment, newPackageFragment.getParent());
				} else {
					return null;
				}
			}
		};
	}
	
	private IWizardPage[] openNewElementWizard(IWorkbenchWizard wizard, Shell shell, Object selection) {
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(selection));
		
		WizardDialog dialog= new WizardDialog(shell, wizard);
		PixelConverter converter= new PixelConverter(shell);

		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
		dialog.create();
		dialog.open();
		IWizardPage[] pages= wizard.getPages();
		return pages;
	}
}
