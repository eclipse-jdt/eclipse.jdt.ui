/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt;
 *     IBM Corporation - updates
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;

/**
 * This proposal is listed in the corrections list for a "type not found" problem.
 * It offers to create a new type by running the class/interface wizard.
 * If selected, this proposal will open a {@link NewClassCreationWizard} or
 * {@link NewInterfaceCreationWizard}.
 * 
 * @see UnresolvedElementsSubProcessor#getTypeProposals
 */

public class NewCUCompletionUsingWizardProposal extends ChangeCorrectionProposal {
	
	private String fNewTypeName;
	private boolean fIsClass;
	private IPackageFragment fPackage;

    public NewCUCompletionUsingWizardProposal(String name, String newTypeName, IPackageFragment pack, boolean isClass, int severity) {
        super(name, null, severity, null);
        
        fNewTypeName= newTypeName;
        fIsClass= isClass;
        fPackage= pack;
    	
        if (isClass) {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
        } else {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
        }
    }
    
	public void apply(IDocument document) {
		NewElementWizard wizard;
		if (fIsClass) {
			wizard= new NewClassCreationWizard();
		} else {
			wizard= new NewInterfaceCreationWizard();
		}
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fPackage));
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		WizardDialog dialog= new WizardDialog(shell, wizard);
		PixelConverter converter= new PixelConverter(shell);
		
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
		dialog.create();
		dialog.getShell().setText("New");

		IWizardPage[] pages= wizard.getPages();
		Assert.isTrue(pages.length == 1 && pages[0] instanceof NewTypeWizardPage);
		
		NewTypeWizardPage page= (NewTypeWizardPage) pages[0];
		page.setTypeName(fNewTypeName, false);
		
		dialog.open();
	}    
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		if (fIsClass) {
			return "Open new class wizard";
		} else {
			return "Open new interface wizard";
		}
	}

}
