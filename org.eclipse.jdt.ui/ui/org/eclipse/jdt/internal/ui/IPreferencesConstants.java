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
	static final String LINK_RENAME_IN_PACKAGES_TO_REFACTORING= "org.eclipse.jdt.ui.packages.linkRenameToRefactoring";
	
	static final String ATTACH_LAUNCH_PORT= "org.eclipse.jdt.ui.attachlaunch.port";
	static final String ATTACH_LAUNCH_HOST= "org.eclipse.jdt.ui.attachlaunch.host";
	static final String ATTACH_LAUNCH_ALLOW_TERMINATE= "org.eclipse.jdt.ui.attachlaunch.allowTerminate";
	
	static final String HIGHLIGHT_KEYWORDS= "org.eclipse.jdt.ui.JavaEditor.highlight_keywords";
	static final String HIGHLIGHT_TYPES= "org.eclipse.jdt.ui.JavaEditor.highlight_types";
	static final String HIGHLIGHT_STRINGS= "org.eclipse.jdt.ui.JavaEditor.highlight_strings";
	static final String HIGHLIGHT_COMMENTS= "org.eclipse.jdt.ui.JavaEditor.highlight_comments";
	
	static final String AUTOINDENT= "org.eclipse.jdt.ui.JavaEditor.autoindent";
	
	static final String MODEL_RECONCILER_DELAY= "org.eclipse.jdt.ui.JavaEditor.model_reconciler_delay";
	
	
	// values
	static final String AUTOINDENT_OFF= "off";
	static final String AUTOINDENT_NORMAL= "normal";
	static final String AUTOINDENT_SMART= "smart";
	
	
	// default values
	static final boolean HIGHLIGHT_DEFAULT= true;
	static final String AUTOINDENT_DEFAULT= "smart";
	static final int MODEL_RECONCILER_DELAY_DEFAULT= 500;
}