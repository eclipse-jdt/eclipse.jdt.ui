/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;


/**
 * Defines constants which are used to refer to values in the plugin's preference bundle.
 */
public interface IPreferencesConstants {
	
	
	// keys
	static final String LINK_PACKAGES_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktoeditor";
	static final String SRCBIN_FOLDERS_IN_NEWPROJ= "org.eclipse.jdt.ui.wizards.srcBinFoldersInNewProjects";
	static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.openTypeHierarchy";
	static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= "perspective";
	static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= "viewPart";
	
	static final String ATTACH_LAUNCH_PORT= "org.eclipse.jdt.ui.attachlaunch.port";
	static final String ATTACH_LAUNCH_HOST= "org.eclipse.jdt.ui.attachlaunch.host";
	static final String ATTACH_LAUNCH_ALLOW_TERMINATE= "org.eclipse.jdt.ui.attachlaunch.allowTerminate";
	
	static final String EDITOR_SHOW_HOVER= "org.eclipse.jdt.ui.editor.showHover";
	static final String EDITOR_SHOW_SEGMENTS= "org.eclipse.jdt.ui.editor.showSegments";
}