/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class ResourceFilter extends ViewerFilter {
	private String[] fExtensions;
	private Collection fExcludes;
	private boolean fCaseSensitive;
	
	public ResourceFilter(String[] extensions, boolean caseSensitive, Collection exclude) {
		fExtensions= extensions;
		fExcludes= exclude;
		fCaseSensitive= caseSensitive;		
	}
	
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			if (fExcludes != null && fExcludes.contains(element)) {
				return false;
			}
			String ext= ((IFile)element).getFullPath().getFileExtension();
			if (fCaseSensitive) {
				for (int i= 0; i < fExtensions.length; i++) {
					if (ext.equals(fExtensions[i])) {
						return true;
					}
				}
			} else {
				for (int i= 0; i < fExtensions.length; i++) {
					if (ext.equalsIgnoreCase(fExtensions[i])) {
						return true;
					}
				}
			}
			return false;
		} else if (element instanceof IContainer) { // IProject, IFolder
			try {
				IResource[] resources= ((IContainer)element).members();
				for (int i= 0; i < resources.length; i++) {
					// recursive!
					if (select(viewer, parent, resources[i])) {
						return true;
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}				
		}
		return false;
	}
	
	public boolean isFilterProperty(Object element, Object property) {
		return false;
	}					
}