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
	static final String LINK_PACKAGES_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktoeditor"; //$NON-NLS-1$
	static final String LINK_TYPEHIERARCHY_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktypehierarchytoeditor"; //$NON-NLS-1$
	static final String SRCBIN_FOLDERS_IN_NEWPROJ= "org.eclipse.jdt.ui.wizards.srcBinFoldersInNewProjects"; //$NON-NLS-1$
	static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.openTypeHierarchy"; //$NON-NLS-1$
	static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= "perspective"; //$NON-NLS-1$
	static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= "viewPart"; //$NON-NLS-1$
	static final String DOUBLE_CLICK_GOES_INTO= "packageview.gointo"; //$NON-NLS-1$
	
	static final String ATTACH_LAUNCH_PORT= "org.eclipse.jdt.ui.attachlaunch.port"; //$NON-NLS-1$
	static final String ATTACH_LAUNCH_HOST= "org.eclipse.jdt.ui.attachlaunch.host"; //$NON-NLS-1$
	static final String ATTACH_LAUNCH_ALLOW_TERMINATE= "org.eclipse.jdt.ui.attachlaunch.allowTerminate"; //$NON-NLS-1$
	
	static final String EDITOR_SHOW_HOVER= "org.eclipse.jdt.ui.editor.showHover"; //$NON-NLS-1$
	static final String EDITOR_SHOW_SEGMENTS= "org.eclipse.jdt.ui.editor.showSegments"; //$NON-NLS-1$
	
	static final String SHOW_HEX_VALUES= "org.eclipse.jdt.ui.javaDebug.showHexValues"; //$NON-NLS-1$
	static final String SHOW_CHAR_VALUES= "org.eclipse.jdt.ui.javaDebug.showCharValues"; //$NON-NLS-1$
	static final String SHOW_UNSIGNED_VALUES= "org.eclipse.jdt.ui.javaDebug.showUnsignedValues"; //$NON-NLS-1$
	// Preference update flag useful for IPropertyChangeListeners to
	// by notified of variable rendering preference changes
	static final String VARIABLE_RENDERING = "VARIABLE_RENDERING";
	
}