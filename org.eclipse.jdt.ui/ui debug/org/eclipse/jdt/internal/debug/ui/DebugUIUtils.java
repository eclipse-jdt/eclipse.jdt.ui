package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class serves as a location for utility methods for the debug UI.
 */
public class DebugUIUtils {

	private static ResourceBundle fgResourceBundle;

	/**
	 * Utility method with conventions
	 */
	public static void errorDialog(Shell shell, String resourcePrefix, IStatus s) {
		String message= getResourceString(resourcePrefix + "message");
		// if the 'message' resource string and the IStatus' message are the same,
		// don't show both in the dialog
		if (message.equals(s.getMessage())) {
			message= null;
		}
		String title= getResourceString(resourcePrefix + "title");
		ErrorDialog.openError(shell, title, message, s);
	}

	/**
	 * Utility method
	 */
	public static String getResourceString(String key) {
		if (fgResourceBundle == null) {
			fgResourceBundle= getResourceBundle();
		}
		if (fgResourceBundle != null) {
			return fgResourceBundle.getString(key);
		} else {
			return "!" + key + "!";
		}
	}

	/**
	 * Returns the resource bundle used by all parts of the debug ui package.
	 */
	public static ResourceBundle getResourceBundle() {
		try {
			return ResourceBundle.getBundle("org.eclipse.jdt.internal.debug.ui.DebugUIResources");
		} catch (MissingResourceException e) {
			logError(e);
		}
		return null;
	}

	/**
	 * Convenience method to log internal UI errors
	 */
	public static void logError(Exception e) {
		if (JavaPlugin.getDefault().isDebugging()) {
			System.out.println("Internal error logged from UI: ");
			e.printStackTrace();
			System.out.println();
		}
	}

}
