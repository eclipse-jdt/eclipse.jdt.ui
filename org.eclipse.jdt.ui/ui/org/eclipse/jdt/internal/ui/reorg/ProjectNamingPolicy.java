/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;
import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.IJavaModel;import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ProjectNamingPolicy implements INamingPolicy {
	private static final String PREFIX= "reorg_policy.project.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_INVALID_NAME= "invalid_name";
	
	public String isValidNewName(Object original, Object parent, String name) {
		//fix for: 1GF0GEL: ITPJUI:WIN2000 - Rename in packages view should validate name
		IStatus status= JavaPlugin.getWorkspace().validateName(name, IResource.PROJECT);
		if (!status.isOK()){
			if (status.isMultiStatus())
				return JavaPlugin.getResourceString(PREFIX+ERROR_INVALID_NAME);
			return status.getMessage();	
		}
		
		IJavaModel jm= (IJavaModel)parent;
		if (jm.getJavaProject(name).exists())
			return JavaPlugin.getResourceString(PREFIX+ERROR_DUPLICATE);
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