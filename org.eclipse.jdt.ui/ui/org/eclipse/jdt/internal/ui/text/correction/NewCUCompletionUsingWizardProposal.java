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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;

import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
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
	
	private Name fNode;
	private ICompilationUnit fCompilationUnit;
	private boolean fIsClass;

	private boolean fShowDialog;

    public NewCUCompletionUsingWizardProposal(String name, ICompilationUnit cu, Name node, boolean isClass, int severity) {
        super(name, null, severity, null);
        
        fCompilationUnit= cu;
        fNode= node;
        fIsClass= isClass;
    	
        if (isClass) {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
        } else {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
        }
        fShowDialog= true;
    }
    
	public void apply(IDocument document) {
		NewElementWizard wizard= createWizard();
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fCompilationUnit));
		
		if (fShowDialog) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			WizardDialog dialog= new WizardDialog(shell, wizard);
			PixelConverter converter= new PixelConverter(shell);
			
			dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			dialog.create();
			dialog.getShell().setText("New");
			
			configureWizardPage(wizard);
			dialog.open();
		} else {
			wizard.addPages();
			try {
				NewTypeWizardPage page= configureWizardPage(wizard);
				page.createType(null);
			} catch (CoreException e) {
				JavaPlugin.log(e);				
			} catch (InterruptedException e) {
			}
		}
	}

	private NewElementWizard createWizard() {
		NewElementWizard wizard;
		if (fIsClass) {
			wizard= new NewClassCreationWizard();
		} else {
			wizard= new NewInterfaceCreationWizard();
		}
		return wizard;
	}

	private NewTypeWizardPage configureWizardPage(NewElementWizard wizard) {
        IWizardPage[] pages= wizard.getPages();
        Assert.isTrue(pages.length > 0 && pages[0] instanceof NewTypeWizardPage);

		NewTypeWizardPage page= (NewTypeWizardPage) pages[0];
		fillInWizardPageName(page);
		fillInWizardPageSuperTypes(page);
		return page;
	}
	
	/**
	 * Fill-in the "Package" and "Name" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageName(NewTypeWizardPage page) {
		page.setTypeName(ASTResolving.getSimpleName(fNode), false);
		if (fNode.isQualifiedName()) {
			String packName= ASTResolving.getQualifier(fNode);
			IPackageFragmentRoot root= (IPackageFragmentRoot) fCompilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			page.setPackageFragment(root.getPackageFragment(packName), true);
		}
	}	
	

	/**
	 * Fill-in the "Super Class" and "Super Interfaces" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageSuperTypes(NewTypeWizardPage page) {
        ITypeBinding type= ASTResolving.guessBindingForTypeReference(fNode, true);
        if (type != null) {
        	if (type.isArray()) {
        		type= type.getElementType();
        	}
        	if (type.isTopLevel() || type.isMember()) {
			    if (type.isClass() && fIsClass) {
			        page.setSuperClass(Bindings.getFullyQualifiedName(type), true);
			    } else if (type.isInterface()) {
			    	List superInterfaces = new ArrayList();
			    	superInterfaces.add(Bindings.getFullyQualifiedName(type));
			    	page.setSuperInterfaces(superInterfaces, true);
			    }
        	}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		buf.append("Open wizard to create ");
		if (fIsClass) {
			buf.append("class <b>");
		} else {
			buf.append("interface <b>");
		}
		buf.append(ASTResolving.getSimpleName(fNode));
		if (fNode.isQualifiedName()) {
			buf.append("</b> in package <b>");
			buf.append(ASTResolving.getQualifier(fNode));
		}
		buf.append("</b>");
		return buf.toString();
	}

	/**
	 * Returns the showDialog.
	 * @return boolean
	 */
	public boolean isShowDialog() {
		return fShowDialog;
	}

	/**
	 * Sets the showDialog.
	 * @param showDialog The showDialog to set
	 */
	public void setShowDialog(boolean showDialog) {
		fShowDialog= showDialog;
	}

}
