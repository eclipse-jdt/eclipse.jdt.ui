/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Shows an error dialog for exceptions that contain an <code>IStatus</code>.
 * If the throwable passed to the methods is of a kind that the methods can handle, 
 * the error dialog is shown and <code>true</code> is returned. Otherwise <code>false
 * </code>is returned, and the client has to handle the error itself. If the passed
 * throwable is of type <code>InvocationTargetException</code> the wrapped excpetion
 * is considered.
 */
public class ExceptionHandler {

	private static ExceptionHandler fgInstance= new ExceptionHandler();
	
	/**
	 * Logs the given exception using the platforms logging mechanism. The exception is
	 * looged as an error with the error code <code>JavaStatusConstants.INTERNAL_ERROR</code>.
	 */
	public static void log(Throwable t, String message) {
		JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 
			JavaStatusConstants.INTERNAL_ERROR, message, t));
	}
	
	public static void handle(CoreException e, String title, String message) {
		handle(e, JavaPlugin.getActiveWorkbenchShell(), title, message);
	}
	
	public static void handle(CoreException e, Shell shell, String title, String message) {
		fgInstance.perform(e, shell, title, message);
	}
	
	public static void handle(InvocationTargetException e, String title, String message) {
		handle(e, JavaPlugin.getActiveWorkbenchShell(), title, message);
	}
	
	public static void handle(InvocationTargetException e, Shell shell, String title, String message) {
		fgInstance.perform(e, shell, title, message);
	}

	//---- Hooks for subclasses to control exception handling ------------------------------------
	
	protected void perform(CoreException e, Shell shell, String title, String message) {
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, title, message, status);
		} else {
			displayMessageDialog(e, shell, title, message);
		}
	}

	protected void perform(InvocationTargetException e, Shell shell, String title, String message) {
		Throwable target= e.getTargetException();
		if (target instanceof CoreException) {
			perform((CoreException)target, shell, title, message);
		} else {
			if (e.getMessage() != null && e.getMessage().length() > 0) {
				displayMessageDialog(e, shell, title, message);
			} else {
				displayMessageDialog(target, shell, title, message);
			}
		}
	}

	//---- Helper methods -----------------------------------------------------------------------
	
	private void displayMessageDialog(Throwable t, Shell shell, String title, String message) {
		StringWriter msg= new StringWriter();
		if (message != null) {
			msg.write(message);
			msg.write("\n\n"); //$NON-NLS-1$
		}
		if (t.getMessage() == null || t.getMessage().length() == 0)
			msg.write(t.toString());
		else
			msg.write(t.getMessage());
		MessageDialog.openError(shell, title, msg.toString());			
	}
	
	private static String getResourceString(ResourceBundle bundle, String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}	
}