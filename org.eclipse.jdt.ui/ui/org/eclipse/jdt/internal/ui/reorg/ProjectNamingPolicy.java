/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;
import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.IJavaModel;import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ProjectNamingPolicy implements INamingPolicy {
	
	public String isValidNewName(Object original, Object parent, String name) {
		//fix for: 1GF0GEL: ITPJUI:WIN2000 - Rename in packages view should validate name
		IStatus status= JavaPlugin.getWorkspace().validateName(name, IResource.PROJECT);
		if (!status.isOK()){
			if (status.isMultiStatus())
				return ReorgMessages.getString("ProjectNamingPolicy.invalidName"); //$NON-NLS-1$
			return status.getMessage();	
		}
		
		IJavaModel jm= (IJavaModel)parent;
		if (jm.getJavaProject(name).exists())
			return ReorgMessages.getString("ProjectNamingPolicy.duplicate"); //$NON-NLS-1$
		return null;
	}
	
	public boolean canReplace(Object original, Object parent, String newName) {
		Object o= getElement(parent, newName);
		if (o != null && o.equals(original))
			return false;
		return true;
	}
	
	public Object getElement(Object parent, String name) {
		IJavaModel jm= (IJavaModel)parent;
		return jm.getJavaProject(name);
	}
}