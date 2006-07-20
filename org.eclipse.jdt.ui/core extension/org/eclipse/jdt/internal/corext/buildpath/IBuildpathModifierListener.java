package org.eclipse.jdt.internal.corext.buildpath;

/**
 * Interface for listeners that want to receive a notification about 
 * changes on a virtual buildpath
 */
public interface IBuildpathModifierListener {
	
	public void buildpathChanged(BuildpathDelta delta);
}
