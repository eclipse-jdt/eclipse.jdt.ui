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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

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
    private final ProblemPosition fProblemPos;

	private boolean fShowDialog;

    public NewCUCompletionUsingWizardProposal(String name, String newTypeName, boolean isClass, ProblemPosition problemPos, int severity) {
        super(name, null, severity, null);
        
        fNewTypeName= newTypeName;
        fIsClass= isClass;
        fProblemPos= problemPos;
    	
        if (isClass) {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
        } else {
            setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
        }
        fShowDialog= true;
    }
    
	public void apply(IDocument document) {
		NewElementWizard wizard= createWizard();
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fProblemPos.getCompilationUnit()));
		
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
        page.setTypeName(fNewTypeName, false);
        
		fillInWizardPageSuperTypes(page);
		return page;
	}

	/**
	 * Fill-in the "Super Class" and "Super Interfaces" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageSuperTypes(NewTypeWizardPage page) {
        List superInterfaces = new ArrayList();
        
		for (Iterator i = getSuperTypes(fProblemPos).iterator(); i.hasNext();) {
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
    private List getSuperTypes(ProblemPosition problemPos) {
        List superTypes= new ArrayList();
        ICompilationUnit cu= fProblemPos.getCompilationUnit();
        CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);

		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		if (selectedNode != null) {
			ITypeBinding type= getBindingForTypeReference(selectedNode.getAST(), selectedNode);
			if (type != null) {
				if (type.isArray()) {
					type= type.getElementType();
				}
				if (type.isClass() || type.isInterface()) {
					superTypes.add(type);            
				}
			}
        }
        return superTypes;
    }
    
    private ITypeBinding getBindingForTypeReference(AST ast, ASTNode node) {
    	ASTNode parent= node.getParent();
    	switch (parent.getNodeType()) {
    	case ASTNode.METHOD_DECLARATION:
			MethodDeclaration decl= (MethodDeclaration) parent;
			if (decl.thrownExceptions().contains(node)) {
				return ast.resolveWellKnownType("java.lang.Exception");
			}
			break;
		case ASTNode.INSTANCEOF_EXPRESSION:
			InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
			return instanceofExpression.getLeftOperand().resolveTypeBinding();
    	case ASTNode.VARIABLE_DECLARATION_STATEMENT:
    		VariableDeclarationStatement statement= (VariableDeclarationStatement) parent;
    		List fragments= statement.fragments();
    		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
				VariableDeclarationFragment frag= (VariableDeclarationFragment) iter.next();
				if (frag.getInitializer() != null) {
					return frag.getInitializer().resolveTypeBinding();
				}
			}
			break;
		case ASTNode.ARRAY_CREATION:
			ArrayCreation creation= (ArrayCreation) parent;
			if (creation.getInitializer() != null) {
				return creation.getInitializer().resolveTypeBinding();
			}
			break;
        case ASTNode.CATCH_CLAUSE:
            return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$						
     	}   	
    	return null;
    
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
