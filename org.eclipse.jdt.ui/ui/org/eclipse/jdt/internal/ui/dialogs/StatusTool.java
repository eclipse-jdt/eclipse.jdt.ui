package org.eclipse.jdt.internal.ui.dialogs;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.DialogPage;

public class StatusTool {

	/**
	 * Compare two IStatus. The more severe is returned:
	 * An error is more severe than a warning, and a warning is more severe
	 * than ok.
	 */
	public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
		if (s1.getSeverity() > s2.getSeverity()) {
			return s1;
		} else {
			return s2;
		}
	}

	/**
	 * Finds the most severe status from a array of status
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
	 * Applies the status to tey status line of a dialog page
	 */
	public static void applyToStatusLine(DialogPage page, IStatus status) {
		String message= status.getMessage();
		if (status.matches(IStatus.ERROR) && !"".equals(message)) {
			page.setErrorMessage(message);
			page.setMessage(null);
		} else if (status.matches(IStatus.WARNING | IStatus.INFO)) {
			page.setErrorMessage(null);
			page.setMessage(message);
		} else {
			page.setErrorMessage(null);
			page.setMessage(null);
		}
	}


}
