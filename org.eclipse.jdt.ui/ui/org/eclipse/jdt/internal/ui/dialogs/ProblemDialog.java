/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.core.runtime.IStatus;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.resource.JFaceResources;

/**
 * A dialog to display errors and warnings to the user, as contained in an
 * <code>IStatus</code> object.  If the status contains additional detailed
 * information then a Details button is automatically supplied, which shows
 * or hides an error details viewer when pressed by the user.
 *
 * @see org.eclipse.core.runtime.IStatus
 */
public class ProblemDialog extends ErrorDialog {

	private Image fImage;
	
	/**
	 * Creates a problem dialog.
	 * Note that the dialog will have no visual representation (no widgets)
	 * until it is told to open.
	 * <p>
	 * Normally one should use <code>openDialog</code> to create and open one of these.
	 * This constructor is useful only if the error object being displayed contains child
	 * items <it>and</it> you need to specify a mask which will be used to filter the
	 * displaying of these children.
	 * </p>
	 *
	 * @param parentShell the shell under which to create this dialog
	 * @param dialogTitle the title to use for this dialog,
	 *   or <code>null</code> to indicate that the default title should be used
	 * @param message the message to show in this dialog, 
	 *   or <code>null</code> to indicate that the error's message should be shown
	 *   as the primary message
	 * @param status the error to show to the user
	 * @param displayMask the mask to use to filter the displaying of child items,
	 *   as per <code>IStatus.matches</code>
	 * @see org.eclipse.core.runtime.IStatus#matches
	 */
	public ProblemDialog(
			Shell parent,
			String title,
			String message,
			Image image,
			IStatus status,
			int displayMask) {
		super(parent, title, message, status, displayMask);
		fImage= image;
	}
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 * Creates and returns the contents of the upper part 
	 * of the dialog (above the button bar).
	 */
	protected Control createDialogArea(Composite parent) {
		// create composite
		Composite composite = (Composite)super.createDialogArea(parent);

		// find the label
		Control[] kids= composite.getChildren();
		int childCount= kids.length;
		Label label= null;
		int i= 0;
		while (i < childCount) {
			if (kids[i] instanceof Label) {
				label= (Label)kids[i];
				if (label.getImage() != null)
					break;
			}
			i++;
		}
		if (i < childCount && label != null) {
			// set image
			if (fImage != null) {
				fImage.setBackground(label.getBackground());
				label.setImage(fImage);
			}
		}
		return composite;
	}
	/**
	 * Opens a warning dialog to display the given warning.  Use this method if the
	 * warning object being displayed does not contain child items, or if you
	 * wish to display all such items without filtering.
	 *
	 * @param parent the parent shell of the dialog, or <code>null</code> if none
	 * @param title the title to use for this dialog,
	 *   or <code>null</code> to indicate that the default title should be used
	 * @param message the message to show in this dialog, 
	 *   or <code>null</code> to indicate that the error's message should be shown
	 *   as the primary message
	 * @param status the error to show to the user
	 * @return the code of the button that was pressed that resulted in this dialog
	 *     closing.  This will be <code>Dialog.OK</code> if the OK button was 
	 * 	   pressed, or <code>Dialog.CANCEL</code> if this dialog's close window 
	 *     decoration or the ESC key was used.
	 */
	public static int open(Shell parent, String title, String message, IStatus status) {
		return open(parent, title, message, status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
	}
	/**
	 * Opens a dialog to display either an error or warning dialog.  Use this method if the
	 * status being displayed contains child items <it>and</it> you wish to
	 * specify a mask which will be used to filter the displaying of these
	 * children.  The error dialog will only be displayed if there is at
	 * least one child status matching the mask.
	 *
	 * @param parent the parent shell of the dialog, or <code>null</code> if none
	 * @param title the title to use for this dialog,
	 *   or <code>null</code> to indicate that the default title should be used
	 * @param message the message to show in this dialog, 
	 *   or <code>null</code> to indicate that the error's message should be shown
	 *   as the primary message
	 * @param status the error to show to the user
	 * @param displayMask the mask to use to filter the displaying of child items,
	 *   as per <code>IStatus.matches</code>
	 * @return the code of the button that was pressed that resulted in this dialog
	 *     closing.  This will be <code>Dialog.OK</code> if the OK button was 
	 * 	   pressed, or <code>Dialog.CANCEL</code> if this dialog's close window 
	 *     decoration or the ESC key was used.
	 * @see org.eclipse.core.runtime.IStatus#matches
	 */
	public static int open(
			Shell parent,
			String title,
			String message,
			IStatus status,
			int displayMask) {
		Image image;
		if (status == null || status.matches(IStatus.ERROR))
			image= JFaceResources.getImageRegistry().get(DLG_IMG_ERROR);
		else if (status.matches(IStatus.WARNING))
			image= JFaceResources.getImageRegistry().get(DLG_IMG_WARNING);
		else
			image= JFaceResources.getImageRegistry().get(DLG_IMG_INFO);
		ErrorDialog dialog= new ProblemDialog(parent, title, message, image, status, displayMask);
		return dialog.open();
	}
}
