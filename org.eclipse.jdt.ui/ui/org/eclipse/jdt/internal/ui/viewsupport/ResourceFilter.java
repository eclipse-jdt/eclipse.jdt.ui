package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Collection;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;


public class ResourceFilter extends ViewerFilter {
	protected String[] fExtensions;
	protected Collection fExcludes;
	
	public ResourceFilter(String[] extensions, Collection exclude) {
		fExtensions= extensions;
		fExcludes= exclude;
	}
	
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			if (fExcludes != null && fExcludes.contains(element)) {
				return false;
			}
			String name= ((IFile)element).getName();
			for (int i= 0; i < fExtensions.length; i++) {
				if (name.endsWith(fExtensions[i]))
					return true;
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
			}				
		}
		return false;
	}
	
	public boolean isFilterProperty(Object element, Object property) {
		return false;
	}					
}