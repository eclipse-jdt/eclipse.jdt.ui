/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.dialogs;import org.eclipse.jface.dialogs.DialogPage;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.jface.dialogs.TitleAreaDialog;import org.eclipse.core.runtime.IStatus;
/** * A utility class to work with IStatus. */
public class StatusUtil {

	/**
	 * Compares two instances of <code>IStatus</code>. The more severe is returned:
	 * An error is more severe than a warning, and a warning is more severe
	 * than ok. If the two stati have the same severity, the second is returned.
	 */
	public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
		if (s1.getSeverity() > s2.getSeverity()) {
			return s1;
		} else {
			return s2;
		}
	}

	/**
	 * Finds the most severe status from a array of stati.
	 * An error is more severe than a warning, and a warning is more severe
	 * than ok.
	 */
	public static IStatus getMostSevere(IStatus[] status) {
		IStatus max= null;
		for (int i= 0; i < status.length; i++) {
			IStatus curr= status[i];
			if (curr.matches(IStatus.ERROR)) {
				return curr;
			}
			if (max == null || curr.getSeverity() > max.getSeverity()) {
				max= curr;
			}
		}
		return max;
	}
		
	/**
	 * Applies the status to the status line of a dialog page.
	 */
	public static void applyToStatusLine(DialogPage page, IStatus status) {
		String message= status.getMessage();		switch (status.getSeverity()) {			case IStatus.OK:				page.setErrorMessage(null);				page.setMessage(message);				break;			case IStatus.WARNING:				case IStatus.INFO:				page.setErrorMessage(null);				page.setMessage(message);				break;						default:				if (message.length() == 0) {					message= null;				}				page.setErrorMessage(message);				page.setMessage(null);				break;				}
	}		/**	 * Applies the status to the status line of a dialog page.	 */	public static void applyToStatusLine(WizardPage page, IStatus status) {		String message= status.getMessage();		switch (status.getSeverity()) {			case IStatus.OK:				page.setErrorMessage(null);				page.setMessage(message, null);				break;			case IStatus.WARNING:				page.setErrorMessage(null);				page.setMessage(message, WizardPage.WARNING_MESSAGE);				break;							case IStatus.INFO:				page.setErrorMessage(null);				page.setMessage(message, WizardPage.INFO_MESSAGE);				break;						default:				if (message.length() == 0) {					message= null;				}				page.setErrorMessage(message);				page.setMessage(null);				break;				}	}	
	
}
