/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class FindDeclarationsAction extends ElementSearchAction {

	public FindDeclarationsAction() {
		this(SearchMessages.getString("Search.FindDeclarationAction.label"), new Class[] {IField.class, IMethod.class, IType.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationAction.tooltip")); //$NON-NLS-1$
	}

	public FindDeclarationsAction(String label, Class[] validTypes) {
		super(label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_DECL);
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.DECLARATIONS;
	}

	protected boolean shouldUserBePrompted() {
		return false;
	}
}