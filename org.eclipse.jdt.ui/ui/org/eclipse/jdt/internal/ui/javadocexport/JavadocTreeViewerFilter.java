/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavadocTreeViewerFilter extends ViewerFilter {

	private int fVisibilitySelection;

	public JavadocTreeViewerFilter() {
		//by default the veiwer will show everything
		fVisibilitySelection= JavadocWizard.PRIVATE;
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IType) {
			IType type= (IType) element;
			try {
				switch (fVisibilitySelection) {
					case JavadocWizard.PRIVATE :
						return true;
					case JavadocWizard.PACKAGE :
						return !Flags.isPrivate(type.getFlags());
					case JavadocWizard.PROTECTED :
						return Flags.isProtected(type.getFlags()) || Flags.isPublic(type.getFlags());
					case JavadocWizard.PUBLIC :
						return Flags.isPublic(type.getFlags());

					default :
						return true;
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return true;
	}

	public void setVisibility(int visibility) {
		fVisibilitySelection= visibility;
	}

} //end class