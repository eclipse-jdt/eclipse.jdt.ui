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
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Finds references of the selected element in the workspace.
 * The action is applicable to selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindReferencesAction extends FindAction {

	private static final String BIN_PRIM_CONST_WARN_DIALOG_ID= "BinaryPrimitiveConstantWarningDialog"; //$NON-NLS-1$


	/**
	 * Creates a new <code>FindReferencesAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReferencesAction(IWorkbenchSite site) {
		this(site, SearchMessages.getString("Search.FindReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReferencesAction(JavaEditor editor) {
		this(editor, SearchMessages.getString("Search.FindReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesAction.tooltip")); //$NON-NLS-1$
	}

	FindReferencesAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
	}

	FindReferencesAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
	}

	int getLimitTo() {
		return IJavaSearchConstants.REFERENCES;
	}	

	void run(IJavaElement element) {
		if (isBinaryPrimitveConstant(element))
			OptionalMessageDialog.open(
				BIN_PRIM_CONST_WARN_DIALOG_ID,
				getShell(),
				SearchMessages.getString("Search.FindReferencesAction.BinPrimConstWarnDialog.title"), //$NON-NLS-1$
				null,
				SearchMessages.getString("Search.FindReferencesAction.BinPrimConstWarnDialog.message"), //$NON-NLS-1$
				OptionalMessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL },
				0);

		super.run(element);
	}
	
	private boolean isBinaryPrimitveConstant(IJavaElement element) {
		if (element.getElementType() == IJavaElement.FIELD) {
			IField field= (IField)element;
			int flags;
			try {
				flags= field.getFlags();
			} catch (JavaModelException ex) {
				return false;
			}
			return field.isBinary() && Flags.isStatic(flags) && Flags.isFinal(flags) && isPrimitive(field);
		}
		return false;
	}

	private static boolean isPrimitive(IField field) {
		String fieldType;
		try {
			fieldType= field.getTypeSignature();
		} catch (JavaModelException ex) {
			return false;
		}
		char first= fieldType.charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED && first != Signature.C_ARRAY);
	}
}