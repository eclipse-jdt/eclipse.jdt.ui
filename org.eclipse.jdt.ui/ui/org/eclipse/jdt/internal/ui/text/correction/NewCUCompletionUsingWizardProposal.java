/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt;
 *     IBM Corporation - updates
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
 * @see UnresolvedElementsSubProcessor#getTypeProposals(IInvocationContext, IProblemLocation, Collection)
 */

public class NewCUCompletionUsingWizardProposal extends ChangeCorrectionProposal {

	private Name fNode;
	private ICompilationUnit fCompilationUnit;
	private boolean fIsClass;
	private IJavaElement fTypeContainer; // IType or IPackageFragment

	private boolean fShowDialog;

	public NewCUCompletionUsingWizardProposal(ICompilationUnit cu, Name node, boolean isClass, IJavaElement typeContainer, int severity) {
		super("", null, severity, null); //$NON-NLS-1$

		fCompilationUnit= cu;
		fNode= node;
		fIsClass= isClass;
		fTypeContainer= typeContainer;

		String containerName= ASTNodes.getQualifier(node);
		String typeName= ASTNodes.getSimpleNameIdentifier(node);
		boolean isInnerType= typeContainer instanceof IType;
		if (isClass) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinnerclass.description", typeName)); //$NON-NLS-1$
				} else {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinnerclass.intype.description", new String[] { typeName, containerName })); //$NON-NLS-1$
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createclass.description", typeName)); //$NON-NLS-1$
				} else {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createclass.inpackage.description", new String[] { typeName, containerName })); //$NON-NLS-1$
				}
			}
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinnerinterface.description", typeName)); //$NON-NLS-1$
				} else {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinnerinterface.intype.description", new String[] { typeName, containerName })); //$NON-NLS-1$
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinterface.description", typeName)); //$NON-NLS-1$
				} else {
					setDisplayName(CorrectionMessages.getFormattedString("NewCUCompletionUsingWizardProposal.createinterface.inpackage.description", new String[] { typeName, containerName })); //$NON-NLS-1$
				}
			}
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
			dialog.getShell().setText(CorrectionMessages.getString("NewCUCompletionUsingWizardProposal.dialogtitle")); //$NON-NLS-1$

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
		page.setTypeName(ASTNodes.getSimpleNameIdentifier(fNode), false);
		
		boolean isInEnclosingType= fTypeContainer instanceof IType;
		if (isInEnclosingType) {
			page.setEnclosingType((IType) fTypeContainer, true);
		} else {
			page.setPackageFragment((IPackageFragment) fTypeContainer, true);
		}
		page.setEnclosingTypeSelection(isInEnclosingType, true);
	}

	/**
	 * Fill-in the "Super Class" and "Super Interfaces" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageSuperTypes(NewTypeWizardPage page) {
		ITypeBinding type= getPossibleSuperTypeBinding(fNode);
		type= Bindings.normalizeTypeBinding(type);
		if (type != null) {
			if (type.isArray()) {
				type= type.getElementType();
			}
			if (type.isTopLevel() || type.isMember()) {
				if (type.isClass() && fIsClass) {
					page.setSuperClass(Bindings.getFullyQualifiedName(type), true);
				} else if (type.isInterface()) {
					List superInterfaces= new ArrayList();
					superInterfaces.add(Bindings.getFullyQualifiedName(type));
					page.setSuperInterfaces(superInterfaces, true);
				}
			}
		}
	}
   	
	private static ITypeBinding getPossibleSuperTypeBinding(ASTNode node) {
		AST ast= node.getAST();
		ASTNode parent= node.getParent();
		while (parent instanceof Type) {
			parent= parent.getParent();
		}
		switch (parent.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				MethodDeclaration decl= (MethodDeclaration) parent;
				if (decl.thrownExceptions().contains(node)) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.INSTANCEOF_EXPRESSION:
				InstanceofExpression instanceofExpression= (InstanceofExpression) parent;
				return instanceofExpression.getLeftOperand().resolveTypeBinding();
			case ASTNode.ARRAY_CREATION:
				ArrayCreation creation= (ArrayCreation) parent;
				if (creation.getInitializer() != null) {
					return creation.getInitializer().resolveTypeBinding();
				}
				return ASTResolving.guessBindingForReference(parent);
			case ASTNode.THROW_STATEMENT :
				return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
			case ASTNode.TYPE_LITERAL:
			case ASTNode.CLASS_INSTANCE_CREATION:
			case ASTNode.CAST_EXPRESSION:
				return ASTResolving.guessBindingForReference(parent);
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				ASTNode parentParent= parent.getParent();
				if (parentParent.getNodeType() == ASTNode.CATCH_CLAUSE) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
		}
		return null;
	}



	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		if (fIsClass) {
			buf.append(CorrectionMessages.getString("NewCUCompletionUsingWizardProposal.createclass.info")); //$NON-NLS-1$
		} else {
			buf.append(CorrectionMessages.getString("NewCUCompletionUsingWizardProposal.createinterface.info")); //$NON-NLS-1$
		}
		buf.append("<br>"); //$NON-NLS-1$
		buf.append("<br>"); //$NON-NLS-1$
		if (fTypeContainer instanceof IType) {
			buf.append(CorrectionMessages.getString("NewCUCompletionUsingWizardProposal.tooltip.enclosingtype")); //$NON-NLS-1$
		} else {
			buf.append(CorrectionMessages.getString("NewCUCompletionUsingWizardProposal.tooltip.package")); //$NON-NLS-1$
		}
		buf.append("<b>"); //$NON-NLS-1$
		buf.append(JavaElementLabels.getElementLabel(fTypeContainer, JavaElementLabels.T_FULLY_QUALIFIED));
		buf.append("</b><br>"); //$NON-NLS-1$
		buf.append("public "); //$NON-NLS-1$
		if (fIsClass) {
			buf.append("class <b>"); //$NON-NLS-1$
		} else {
			buf.append("interface <b>"); //$NON-NLS-1$
		}
		buf.append(ASTNodes.getSimpleNameIdentifier(fNode));
		
		ITypeBinding superclass= getPossibleSuperTypeBinding(fNode);
		if (superclass != null) {
			if (superclass.isClass() || !fIsClass) {
				buf.append("</b> extends <b>"); //$NON-NLS-1$
			} else {
				buf.append("</b> implements <b>"); //$NON-NLS-1$
			}
			buf.append(superclass.getName());
		}
		buf.append("</b> {<br>}<br>"); //$NON-NLS-1$
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

	/**
	 * Returns <code>true</code> if is class.
	 * @return boolean
	 */
	public boolean isClass() {
		return fIsClass;
	}

}
