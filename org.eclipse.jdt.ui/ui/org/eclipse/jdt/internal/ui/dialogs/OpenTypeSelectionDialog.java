/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A dialog to select a type from a list of types. The selected type will be
 * opened in the editor.
 */
public class OpenTypeSelectionDialog extends TypeSelectionDialog {

	public static final int IN_HIERARCHY= IDialogConstants.CLIENT_ID + 1;
	/** The dialog location. */
	private Point fLocation;
	/** The dialog size. */
	private Point fSize;

	/**
	 * Constructs an instance of <code>OpenTypeSelectionDialog</code>.
	 * @param parent  the parent shell.
	 * @param context the context.
	 * @param elementKinds <code>IJavaSearchConstants.CLASS</code>, <code>IJavaSearchConstants.INTERFACE</code>
	 * or <code>IJavaSearchConstants.TYPE</code>
	 * @param scope   the java search scope.
	 */
	public OpenTypeSelectionDialog(Shell parent, IRunnableContext context, int elementKinds, IJavaSearchScope scope) {
		super(parent, context, elementKinds, scope);
	}
	
	/**
	 * @see Windows#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	/*
	 * @see Window#close()
	 */
	public boolean close() {
		writeSettings();
		return super.close();
	}

	/**
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control control= super.createContents(parent);
		readSettings();
		return control;
	}


	/**
	 * Returns the dialog settings object used to share state
	 * between several find/replace dialogs.
	 *
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		String sectionName= getClass().getName();
		IDialogSettings subSettings= settings.getSection(sectionName);
		if (subSettings == null)
			subSettings= settings.addNewSection(sectionName);
		return subSettings;
	}

	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readSettings() {
		IDialogSettings s= getDialogSettings();

		try {
			Shell shell= getShell();

			int x= s.getInt("x"); //$NON-NLS-1$
			int y= s.getInt("y"); //$NON-NLS-1$
			shell.setLocation(x, y);

			int width= s.getInt("width"); //$NON-NLS-1$
			int height= s.getInt("height"); //$NON-NLS-1$
			shell.setSize(width, height);

		} catch (NumberFormatException e) {
			fLocation= null;
			fSize= null;
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		IDialogSettings s= getDialogSettings();

		Point location= getShell().getLocation();
		s.put("x", location.x); //$NON-NLS-1$
		s.put("y", location.y); //$NON-NLS-1$

		Point size= getShell().getSize();
		s.put("width", size.x); //$NON-NLS-1$
		s.put("height", size.y); //$NON-NLS-1$
	}

}
