/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.dialogs.ResourceSorter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public class OutputLocationDialog extends StatusDialog {
	
	private StringButtonDialogField fContainerDialogField;
	private StatusInfo fContainerFieldStatus;
	
	private IProject fCurrProject;
	private IPath fOutputLocation;
		
	public OutputLocationDialog(Shell parent, CPListElement entryToEdit) {
		super(parent);
		setTitle(NewWizardMessages.getString("OutputLocationDialog.title"));
		fContainerFieldStatus= new StatusInfo();
		
		String label= NewWizardMessages.getFormattedString("OutputLocationDialog.location.label", entryToEdit.getPath().makeRelative().toString());
		
		OutputLocationAdapter adapter= new OutputLocationAdapter();
		fContainerDialogField= new StringButtonDialogField(adapter);
		fContainerDialogField.setLabelText(label);
		fContainerDialogField.setButtonLabel(NewWizardMessages.getString("OutputLocationDialog.location.button"));
		fContainerDialogField.setDialogFieldListener(adapter);
		
		fCurrProject= entryToEdit.getJavaProject().getProject();
		
		IPath outputLocation= (IPath) entryToEdit.getAttribute(CPListElement.OUTPUT);
		if (outputLocation == null) {
			fContainerDialogField.setText(""); //$NON-NLS-1$
		} else {
			fContainerDialogField.setText(outputLocation.makeRelative().toString());
		}
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		int widthHint= convertWidthInCharsToPixels(60);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		
		fContainerDialogField.doFillIntoGrid(inner, 3);
		
		LayoutUtil.setWidthHint(fContainerDialogField.getLabelControl(null), widthHint);
		LayoutUtil.setHorizontalSpan(fContainerDialogField.getLabelControl(null), 2);
		
		LayoutUtil.setWidthHint(fContainerDialogField.getTextControl(null), widthHint);
		LayoutUtil.setHorizontalGrabbing(fContainerDialogField.getTextControl(null));
				
		fContainerDialogField.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}

		
	// -------- OutputLocationAdapter --------

	private class OutputLocationAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}

		public void changeControlPressed(DialogField field) {
			doChangeControlPressed();
		}
	}
	
	protected void doChangeControlPressed() {
		IContainer container= chooseOutputLocation();
		if (container != null) {
			fContainerDialogField.setText(container.getFullPath().toString());
		}
	}
	
	
	protected void doStatusLineUpdate() {
		checkIfPathValid();
		updateStatus(fContainerFieldStatus);
	}		
	
	protected void checkIfPathValid() {
		fOutputLocation= null;
		IWorkspace workspace= fCurrProject.getWorkspace();
		
		String pathStr= fContainerDialogField.getText();
		if (pathStr.length() == 0) {
			return;
		}
				
		IPath path= new Path(pathStr);
		IStatus pathValidation= workspace.validatePath(path.toString(), IResource.PROJECT | IResource.FOLDER);
		if (!pathValidation.isOK()) {
			fContainerFieldStatus.setError(NewWizardMessages.getFormattedString("OutputLocationDialog.error.invalidpath", pathValidation.getMessage())); //$NON-NLS-1$
			return;
		}
				
		IResource res= workspace.getRoot().findMember(path);
		if (res != null) {
			// if exists, must be a folder or project
			if (res.getType() == IResource.FILE) {
				fContainerFieldStatus.setError(NewWizardMessages.getString("OutputLocationDialog.error.existingisfile")); //$NON-NLS-1$
				return;
			}
		}
		fOutputLocation= path;
		fContainerFieldStatus.setOK();
	}
	
		
	public IPath getOutputLocation() {
		return fOutputLocation;
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OUTPUT_LOCATION_DIALOG);
	}
	
	// ---------- util method ------------

	private IContainer chooseOutputLocation() {
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		IProject[] allProjects= root.getProjects();
		ArrayList rejectedElements= new ArrayList(allProjects.length);
		for (int i= 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(fCurrProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IResource initSelection= null;
		if (fOutputLocation != null) {
			initSelection= root.findMember(fOutputLocation);
		}

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(NewWizardMessages.getString("OutputLocationDialog.ChooseOutputFolder.title")); //$NON-NLS-1$
		dialog.setValidator(validator);
		dialog.setMessage(NewWizardMessages.getString("OutputLocationDialog.ChooseOutputFolder.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(root);
		dialog.setInitialSelection(initSelection);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			return (IContainer)dialog.getFirstResult();
		}
		return null;
	}
	


}