/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.dialogs;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.DialogPage;import org.eclipse.jdt.internal.ui.widgets.MessageLine;
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
	 * Returns error-message / warning-message for a status. 	 * @return Array of size 2. Index 0 is the error message or <null>	 * if not an error. Index 1 the warning message or <null> if not a warning.
	 */
	private static String[] getErrorMessages(IStatus status) {
		String message= status.getMessage();
		if (status.matches(IStatus.ERROR) && !"".equals(message)) { //$NON-NLS-1$
			return new String[] { message, null };
		} else if (status.matches(IStatus.WARNING | IStatus.INFO)) {
			return new String[] { null, message };
		} else {
			return new String[] { null, null };
		}
	}
	
	/**
	 * Applies the status to the status line of a dialog page.
	 */
	public static void applyToStatusLine(DialogPage page, IStatus status) {
		String[] messages= getErrorMessages(status);
		page.setErrorMessage(messages[0]);
		page.setMessage(messages[1]);
	}
	
	/**
	 * Applies the status to a message line
	 */
	public static void applyToStatusLine(MessageLine messageLine, IStatus status) {
		String[] messages= getErrorMessages(status);
		messageLine.setErrorMessage(messages[0]);
		messageLine.setMessage(messages[1]);
	}

}
