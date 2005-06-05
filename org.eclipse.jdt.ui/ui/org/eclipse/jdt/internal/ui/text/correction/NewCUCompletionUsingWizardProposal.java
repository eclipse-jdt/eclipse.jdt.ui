/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt;
 *     IBM Corporation - updates
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewAnnotationCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.internal.ui.wizards.NewEnumCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;

/**
 * This proposal is listed in the corrections list for a "type not found" problem.
 * It offers to create a new type by running the class/interface wizard.
 * If selected, this proposal will open a {@link NewClassCreationWizard},
 * {@link NewInterfaceCreationWizard}, {@link NewEnumCreationWizard} or {@link NewAnnotationCreationWizard}.
 *
 * @see UnresolvedElementsSubProcessor#getTypeProposals(IInvocationContext, IProblemLocation, Collection)
 */

public class NewCUCompletionUsingWizardProposal extends ChangeCorrectionProposal {

	public static final int K_CLASS= 1;
	public static final int K_INTERFACE= 2;
	public static final int K_ENUM= 3;
	public static final int K_ANNOTATION= 4;

	private Name fNode;
	private ICompilationUnit fCompilationUnit;
	private int fTypeKind;
	private IJavaElement fTypeContainer; // IType or IPackageFragment

	private boolean fShowDialog;

	public NewCUCompletionUsingWizardProposal(ICompilationUnit cu, Name node, int typeKind, IJavaElement typeContainer, int severity) {
		super("", null, severity, null); //$NON-NLS-1$

		fCompilationUnit= cu;
		fNode= node;
		fTypeKind= typeKind;
		fTypeContainer= typeContainer;

		String containerName= ASTNodes.getQualifier(node);
		String typeName= ASTNodes.getSimpleNameIdentifier(node);
		boolean isInnerType= typeContainer instanceof IType;
		switch (typeKind) {
		case K_CLASS:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_INTERFACE:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_ENUM:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENUM));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_ANNOTATION:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown type kind"); //$NON-NLS-1$
		}
		fShowDialog= true;
	}

	public void apply(IDocument document) {
		NewElementWizard wizard= createWizard();
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fCompilationUnit));

		IType createdType= null;
		
		if (fShowDialog) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			WizardDialog dialog= new WizardDialog(shell, wizard);
			PixelConverter converter= new PixelConverter(shell);

			dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			dialog.create();
			dialog.getShell().setText(CorrectionMessages.NewCUCompletionUsingWizardProposal_dialogtitle);

			configureWizardPage(wizard);
			if (dialog.open() == Window.OK) {
				createdType= (IType) wizard.getCreatedElement();
			}
		} else {
			wizard.addPages();
			try {
				NewTypeWizardPage page= configureWizardPage(wizard);
				page.createType(null);
				createdType= page.getCreatedType();
			} catch (CoreException e) {
				JavaPlugin.log(e);
			} catch (InterruptedException e) {
			}
		}
		
		if (createdType != null) {
			IJavaElement container= createdType.getParent();
			if (container instanceof ICompilationUnit) {
				container= container.getParent();
			}
			if (!container.equals(fTypeContainer)) {
				// add import
				IJavaProject project= fCompilationUnit.getJavaProject();
				try {
					ImportsStructure imp= new ImportsStructure(fCompilationUnit, JavaPreferencesSettings.getImportOrderPreference(project), JavaPreferencesSettings.getImportNumberThreshold(project), true);
					imp.addImport(createdType.getFullyQualifiedName('.'));
					imp.create(false, null);
				} catch (CoreException e) {
				}
			}
		}
		
	}

	private NewElementWizard createWizard() {
		switch (fTypeKind) {
			case K_CLASS:
				return new NewClassCreationWizard();
			case K_INTERFACE:
				return new NewInterfaceCreationWizard();
			case K_ENUM:
				return new NewEnumCreationWizard();
			case K_ANNOTATION:
				return new NewAnnotationCreationWizard();
		}
		throw new IllegalArgumentException();
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
				if (type.isClass() && (fTypeKind == K_CLASS)) {
					page.setSuperClass(Bindings.getFullyQualifiedName(type), true);
				} else if (type.isInterface()) {
					List superInterfaces= new ArrayList();
					superInterfaces.add(Bindings.getFullyQualifiedName(type));
					page.setSuperInterfaces(superInterfaces, true);
				}
			}
		}
	}

	private ITypeBinding getPossibleSuperTypeBinding(ASTNode node) {
		 if (fTypeKind == K_ANNOTATION) {
		 	return null;
		 }

		AST ast= node.getAST();
		ASTNode parent= node.getParent();
		while (parent instanceof Type) {
			node= parent;
			parent= parent.getParent();
		}
		switch (parent.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				if (node.getLocationInParent() == MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.THROW_STATEMENT :
				return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				if (parent.getLocationInParent() == CatchClause.EXCEPTION_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
		}
		return ASTResolving.guessBindingForTypeReference(node);
	}



	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		switch (fTypeKind) {
			case K_CLASS:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_info);
				break;
			case K_INTERFACE:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_info);
				break;
			case K_ENUM:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_info);
				break;
			case K_ANNOTATION:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_info);
				break;
		}
		buf.append("<br>"); //$NON-NLS-1$
		buf.append("<br>"); //$NON-NLS-1$
		if (fTypeContainer instanceof IType) {
			buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_enclosingtype);
		} else {
			buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_package);
		}
		buf.append(" <b>"); //$NON-NLS-1$
		buf.append(JavaElementLabels.getElementLabel(fTypeContainer, JavaElementLabels.T_FULLY_QUALIFIED));
		buf.append("</b><br>"); //$NON-NLS-1$
		buf.append("public "); //$NON-NLS-1$


		switch (fTypeKind) {
			case K_CLASS:
				buf.append("class <b>"); //$NON-NLS-1$
				break;
			case K_INTERFACE:
				buf.append("interface <b>"); //$NON-NLS-1$
				break;
			case K_ENUM:
				buf.append("enum <b>"); //$NON-NLS-1$
				break;
			case K_ANNOTATION:
				buf.append("@interface <b>"); //$NON-NLS-1$
				break;
		}
		buf.append(ASTNodes.getSimpleNameIdentifier(fNode));

		ITypeBinding superclass= getPossibleSuperTypeBinding(fNode);
		if (superclass != null) {
			if (superclass.isClass()) {
				if (fTypeKind == K_CLASS) {
					buf.append("</b> extends <b>"); //$NON-NLS-1$
				}
			} else {
				if (fTypeKind == K_INTERFACE) {
					buf.append("</b> extends <b>"); //$NON-NLS-1$
				} else {
					buf.append("</b> implements <b>"); //$NON-NLS-1$
				}
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

	public IType getCreatedType() {
		String name= ASTNodes.getSimpleNameIdentifier(fNode);
		if (fTypeContainer instanceof IPackageFragment) {
			return ((IPackageFragment) fTypeContainer).getCompilationUnit(name + ".java").getType(name); //$NON-NLS-1$
		}
		return  ((IType) fTypeContainer).getType(name);
	}


	public int getTypeKind() {
		return fTypeKind;
	}

}
