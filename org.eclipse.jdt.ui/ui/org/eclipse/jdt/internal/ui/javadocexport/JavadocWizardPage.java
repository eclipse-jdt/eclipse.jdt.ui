/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizardPage;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;

/**
 * @version 	1.0
 * @author
 */
public abstract class JavadocWizardPage extends NewElementWizardPage {

	/*
	 * @see IDialogPage#createControl(Composite)
	 * 
	 */

	protected JavadocWizardPage(String pageName) {
		super(pageName);
		setTitle("Javadoc Generation");
	}

	protected Button createButton(Composite composite, int style, String message, GridData gd) {
		Button button = new Button(composite, style);
		button.setText(message);
		button.setLayoutData(gd);
		return button;
	}

	protected GridLayout createGridLayout(int columns) {
		GridLayout gl = new GridLayout();
		gl.numColumns = columns;
		return gl;
	}

	protected GridData createGridData(int flag, int hspan, int vspan, int indent) {
		GridData gd = new GridData(flag);
		gd.horizontalIndent = indent;
		gd.horizontalSpan = hspan;
		gd.verticalSpan = vspan;
		return gd;

	}

	protected GridData createGridData(int flag, int hspan, int indent) {
		GridData gd = new GridData(flag);
		gd.horizontalIndent = indent;
		gd.horizontalSpan = hspan;
		return gd;

	}

	protected GridData createGridData(int hspan) {
		GridData gd = new GridData();
		gd.horizontalSpan = hspan;
		return gd;

	}

	protected Label createLabel(Composite composite, int style, String message, GridData gd) {
		Label label = new Label(composite, style);
		label.setText(message);
		label.setLayoutData(gd);
		return label;
	}

	protected Text createText(Composite composite, int style, String message, GridData gd) {
		Text text = new Text(composite, style);
		if (message != null)
			text.setText(message);
		text.setLayoutData(gd);
		return text;
	}

	protected void handleFileBrowseButtonPressed(Text text, String[] extensions, String title) {

		FileDialog dialog = new FileDialog(text.getShell());
		dialog.setText(title);
		dialog.setFilterExtensions(extensions);
		String dirName = text.getText();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path = new File(dirName);
			if (path.exists())
				dialog.setFilterPath(dirName);

		}
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null)
			text.setText(selectedDirectory);
	}

	protected void handleFolderBrowseButtonPressed(Text text, String title, String message) {

		DirectoryDialog dialog = new DirectoryDialog(text.getShell());
		dialog.setFilterPath(text.getText());
		dialog.setText(title);
		dialog.setMessage(message);
		String res = dialog.open();
		if (res != null) {
			File file = new File(res);
			if (file.exists() && file.isDirectory())
				text.setText(res);
		}

	}

	protected IContainer chooseFolder(IProject project, String title, String message) {
		Class[] acceptedClasses = new Class[] { IFolder.class, IProject.class };
		ISelectionValidator validator = new TypedElementSelectionValidator(acceptedClasses, false);

		ViewerFilter filter = new TypedViewerFilter(acceptedClasses);

		ILabelProvider lp = new WorkbenchLabelProvider();
		ITreeContentProvider cp = new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(project.getWorkspace().getRoot());
		dialog.setInitialSelection(project);

		if (dialog.open() == dialog.OK) {
			return (IContainer) dialog.getFirstResult();
		}
		return null;
	}


	protected static class EnableSelectionAdapter extends SelectionAdapter {

		private Control[] fEnable;
		private Control[] fDisable;
		private boolean single;

		protected EnableSelectionAdapter(Control[] enable, Control[] disable) {
			super();
			fEnable = enable;
			fDisable = disable;
		}

		public void widgetSelected(SelectionEvent e) {
			for (int i = 0; i < fEnable.length; i++) {
				((Control) fEnable[i]).setEnabled(true);
			}
			for (int i = 0; i < fDisable.length; i++) {
				((Control) fDisable[i]).setEnabled(false);
			}
			validate();
		}
		//copied from  WizardNewProjectCreationPage
		public void validate() {
		}

	} //end class EnableSelectionAdapter

	protected static class ToggleSelectionAdapter extends SelectionAdapter {

		Control[] controls;

		protected ToggleSelectionAdapter(Control[] controls) {
			this.controls = controls;
		}

		public void widgetSelected(SelectionEvent e) {

			for (int i = 0; i < controls.length; i++) {
				Control control = controls[i];
				control.setEnabled(!control.getEnabled());
			}
			validate();
		}

		public void validate() {
		}

	} //end class ToggleSelection Adapter

}