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
		fVisibilitySelection= 0;
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IType) {
			IType type= (IType) element;
			try {
				switch (fVisibilitySelection) {
					case 0 :
						return true;
					case 1 :
						return !Flags.isPrivate(type.getFlags());
					case 2 :
						return Flags.isProtected(type.getFlags()) || Flags.isPublic(type.getFlags());
					case 3 :
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

	public void setVisibility(String visibility) {
		
		if(visibility.equalsIgnoreCase("private"))
			fVisibilitySelection= 0;
		else if(visibility.equalsIgnoreCase("package"))
			fVisibilitySelection= 1;
		else if(visibility.equalsIgnoreCase("protected"))
			fVisibilitySelection= 2;
		else if(visibility.equalsIgnoreCase("public"))
			fVisibilitySelection= 3;
		else fVisibilitySelection= 0;	
		
	}

} //end class