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
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

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
	
	private final String fNewTypeName;
	private final boolean fIsClass;
	private final ICompilationUnit fCompilationUnit;
    private final ProblemPosition fProblemPos;

    public NewCUCompletionUsingWizardProposal(String name, String newTypeName, ICompilationUnit compilationUnit, boolean isClass, ProblemPosition problemPos, int severity) {
        super(name, null, severity, null);
        
        fNewTypeName= newTypeName;
        fIsClass= isClass;
        fCompilationUnit= compilationUnit;
        fProblemPos= problemPos;
    	
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
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fCompilationUnit));
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		WizardDialog dialog= new WizardDialog(shell, wizard);
		PixelConverter converter= new PixelConverter(shell);
		
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
		dialog.create();
		dialog.getShell().setText("New");

        fillInWizardPage(wizard);
        
		dialog.open();
	}

	private void fillInWizardPage(NewElementWizard wizard) {
        IWizardPage[] pages= wizard.getPages();
        Assert.isTrue(pages.length > 0 && pages[0] instanceof NewTypeWizardPage);

        NewTypeWizardPage page= (NewTypeWizardPage) pages[0];
                
        page.setTypeName(fNewTypeName, false);
        
		fillInWizardPageSuperTypes(page);
	}

	/**
	 * Fill-in the "Super Class" and "Super Interfaces" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageSuperTypes(NewTypeWizardPage page) {
        List superInterfaces = new ArrayList();
        
		for (Iterator i = getSuperTypes(fCompilationUnit, fProblemPos).iterator(); i.hasNext();) {
			ITypeBinding type = (ITypeBinding) i.next();
                        
		    if (type.isClass()) {
		        page.setSuperClass(Bindings.getFullyQualifiedName(type), true);
		    } else if (type.isInterface()) {
				superInterfaces.add(Bindings.getFullyQualifiedName(type));
		    }
		}
        
        page.setSuperInterfaces(superInterfaces, true);
	}
    
    /**
     * @return a list of {@link ITypeBinding}s representing the super types of the type
     * needing correction. The list may be empty, in which case the only supertype is Object.
     */
    private List getSuperTypes(ICompilationUnit cu, ProblemPosition problemPos) {
        List superTypes= new ArrayList();        
        CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);

        if (problemPos.getId() == IProblem.ExceptionTypeNotFound) {
            superTypes.add(astRoot.getAST().resolveWellKnownType("java.lang.Exception"));
        } else {
            ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
            if (selectedNode != null) {
                ITypeBinding type= ASTResolving.getTypeBinding(selectedNode.getParent());
                if (type != null) {
                    if (type.isArray()) {
                        type= type.getElementType();
                    }
                    if (type.isClass() || type.isInterface()) {
                        superTypes.add(type);            
                    }
                }
            }
        }
                
        return superTypes;
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
