/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;import org.eclipse.jdt.ui.JavaUI;


public interface IUIConstants {
	
	/**
	 * The id of the Java Refactoring action set
	 * (value <code>"org.eclipse.jdt.ui.refactoring.actionSet"</code>).
	 */
	public static final String ID_REFACTORING_ACTION_SET= "org.eclipse.jdt.ui.refactoring.actionSet"; //$NON-NLS-1$
	
		
	public static final String KEY_OK= JavaUI.ID_PLUGIN + ".ok.label"; //$NON-NLS-1$
	public static final String KEY_CANCEL= JavaUI.ID_PLUGIN + ".cancel.label"; //$NON-NLS-1$
	
	public static final String P_ICON_NAME= JavaUI.ID_PLUGIN + ".icon_name"; //$NON-NLS-1$
	
	public static final String DIALOGSTORE_LASTEXTJAR= JavaUI.ID_PLUGIN + ".lastextjar"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTJARATTACH= JavaUI.ID_PLUGIN + ".lastjarattach"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTVARIABLE= JavaUI.ID_PLUGIN + ".lastvariable";	 //$NON-NLS-1$
	
	
}