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
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class FindReferencesAction extends ElementSearchAction {

	public FindReferencesAction() {
		super(JavaPlugin.getResourceString("Search.FindReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class});
		setToolTipText(JavaPlugin.getResourceString("Search.FindReferencesAction.label"));
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.REFERENCES;
	}	
}