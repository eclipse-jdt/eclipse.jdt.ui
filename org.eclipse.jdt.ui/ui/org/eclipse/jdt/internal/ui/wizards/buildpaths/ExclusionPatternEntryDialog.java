/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public class ExclusionPatternEntryDialog extends StatusDialog {
	
	private StringButtonDialogField fExclusionPatternDialog;
	private StatusInfo fExclusionPatternStatus;
	
	private IProject fCurrProject;
	private IPath fExclusionPattern;
		
	public ExclusionPatternEntryDialog(Shell parent, IPath patternToEdit, CPListElement entryToEdit) {
		super(parent);
		if (patternToEdit == null) {
			setTitle(NewWizardMessages.getString("ExclusionPatternEntryDialog.add.title"));
		} else {
			setTitle(NewWizardMessages.getString("ExclusionPatternEntryDialog.edit.title"));			
		}
		
		fExclusionPatternStatus= new StatusInfo();
		
		String label= NewWizardMessages.getFormattedString("ExclusionPatternEntryDialog.pattern.label", entryToEdit.getPath().makeRelative().toString());
		
		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		fExclusionPatternDialog= new StringButtonDialogField(adapter);
		fExclusionPatternDialog.setLabelText(label);
		fExclusionPatternDialog.setButtonLabel(NewWizardMessages.getString("ExclusionPatternEntryDialog.pattern.button"));
		fExclusionPatternDialog.setDialogFieldListener(adapter);
		fExclusionPatternDialog.enableButton(false);
		
		fCurrProject= entryToEdit.getJavaProject().getProject();
		
		if (patternToEdit == null) {
			fExclusionPatternDialog.setText(""); //$NON-NLS-1$
		} else {
			fExclusionPatternDialog.setText(patternToEdit.toString());
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
		
		Label description= new Label(inner, SWT.WRAP);
		description.setText(NewWizardMessages.getString("ExclusionPatternEntryDialog.description"));
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		gd.widthHint= convertWidthInCharsToPixels(80);
		description.setLayoutData(gd);
		
		fExclusionPatternDialog.doFillIntoGrid(inner, 3);
		
		LayoutUtil.setWidthHint(fExclusionPatternDialog.getLabelControl(null), widthHint);
		LayoutUtil.setHorizontalSpan(fExclusionPatternDialog.getLabelControl(null), 2);
		
		LayoutUtil.setWidthHint(fExclusionPatternDialog.getTextControl(null), widthHint);
		LayoutUtil.setHorizontalGrabbing(fExclusionPatternDialog.getTextControl(null));
				
		fExclusionPatternDialog.postSetFocusOnDialogField(parent.getDisplay());
		return composite;
	}

		
	// -------- ExclusionPatternAdapter --------

	private class ExclusionPatternAdapter implements IDialogFieldListener, IStringButtonAdapter {
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			doStatusLineUpdate();
		}

		public void changeControlPressed(DialogField field) {
			doChangeControlPressed();
		}
	}
	
	protected void doChangeControlPressed() {
		IPath pattern= chooseExclusionPattern();
		if (pattern != null) {
			fExclusionPatternDialog.setText(pattern.toString());
		}
	}

	
	
	protected void doStatusLineUpdate() {
		checkIfPatternValid();
		updateStatus(fExclusionPatternStatus);
	}		
	
	protected void checkIfPatternValid() {
		ArrayList res= new ArrayList();
		String pattern= fExclusionPatternDialog.getText();
		if (pattern.trim().length() == 0) {
			fExclusionPatternStatus.setError(NewWizardMessages.getString("ExclusionPatternEntryDialog.error.empty"));
			return;
		}
		fExclusionPattern= new Path(pattern); 
		fExclusionPatternStatus.setOK();
	}
	
		
	public IPath getExclusionPattern() {
		return fExclusionPattern;
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.EXCLUSION_PATTERN_DIALOG);
	}
	
	// ---------- util method ------------

	private IPath chooseExclusionPattern() {
//		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
//		Class[] acceptedClasses= new Class[] { IProject.class, IFolder.class };
//		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
//		IProject[] allProjects= root.getProjects();
//		ArrayList rejectedElements= new ArrayList(allProjects.length);
//		for (int i= 0; i < allProjects.length; i++) {
//			if (!allProjects[i].equals(fCurrProject)) {
//				rejectedElements.add(allProjects[i]);
//			}
//		}
//		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());
//
//		ILabelProvider lp= new WorkbenchLabelProvider();
//		ITreeContentProvider cp= new WorkbenchContentProvider();
//
//		IResource initSelection= null;
//		if (fOutputLocation != null) {
//			initSelection= root.findMember(fOutputLocation);
//		}
//
//		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
//		dialog.setTitle(NewWizardMessages.getString("ExclusionPatternDialog.ChooseExclusionPattern.title")); //$NON-NLS-1$
//		dialog.setValidator(validator);
//		dialog.setMessage(NewWizardMessages.getString("ExclusionPatternDialog.ChooseExclusionPattern.description")); //$NON-NLS-1$
//		dialog.addFilter(filter);
//		dialog.setInput(root);
//		dialog.setInitialSelection(initSelection);
//		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
//
//		if (dialog.open() == ElementTreeSelectionDialog.OK) {
//			return (IContainer)dialog.getFirstResult();
//		}
		return null;
	}
	


}