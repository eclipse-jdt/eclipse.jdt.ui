/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jdt.core.IJavaModel;import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ProjectNamingPolicy implements INamingPolicy {
	private static final String PREFIX= "reorg_policy.project.";
	private static final String ERROR_DUPLICATE= "duplicate";
	private static final String ERROR_INVALID_NAME= "invalid_name";
	
	public String isValidNewName(Object original, Object parent, String name) {
		if ("".equals(name))
			return JavaPlugin.getResourceString(PREFIX+ERROR_INVALID_NAME);
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