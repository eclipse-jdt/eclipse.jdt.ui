/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

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

public class ExclusionPatternEntryDialog extends StatusDialog {
	
	private StringButtonDialogField fExclusionPatternDialog;
	private StatusInfo fExclusionPatternStatus;
	
	private IContainer fCurrSourceFolder;
	private String fExclusionPattern;
	private List fExistingPatterns;
		
	public ExclusionPatternEntryDialog(Shell parent, String patternToEdit, List existingPatterns, CPListElement entryToEdit) {
		super(parent);
		fExistingPatterns= existingPatterns;
		if (patternToEdit == null) {
			setTitle(NewWizardMessages.getString("ExclusionPatternEntryDialog.add.title"));
		} else {
			setTitle(NewWizardMessages.getString("ExclusionPatternEntryDialog.edit.title"));
			fExistingPatterns.remove(patternToEdit);
		}
		
		IWorkspaceRoot root= entryToEdit.getJavaProject().getProject().getWorkspace().getRoot();
		IResource res= root.findMember(entryToEdit.getPath());
		if (res instanceof IContainer) {
			fCurrSourceFolder= (IContainer) res;
		}		
		
		fExclusionPatternStatus= new StatusInfo();
		
		String label= NewWizardMessages.getFormattedString("ExclusionPatternEntryDialog.pattern.label", entryToEdit.getPath().makeRelative().toString());
		
		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		fExclusionPatternDialog= new StringButtonDialogField(adapter);
		fExclusionPatternDialog.setLabelText(label);
		fExclusionPatternDialog.setButtonLabel(NewWizardMessages.getString("ExclusionPatternEntryDialog.pattern.button"));
		fExclusionPatternDialog.setDialogFieldListener(adapter);
		fExclusionPatternDialog.enableButton(fCurrSourceFolder != null);
		
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
		String pattern= fExclusionPatternDialog.getText().trim();
		if (pattern.length() == 0) {
			fExclusionPatternStatus.setError(NewWizardMessages.getString("ExclusionPatternEntryDialog.error.empty"));
			return;
		}
		IPath path= new Path(pattern);
		if (path.isAbsolute() || path.getDevice() != null) {
			fExclusionPatternStatus.setError(NewWizardMessages.getString("ExclusionPatternEntryDialog.error.notrelative"));
			return;
		}
		if (fExistingPatterns.contains(pattern)) {
			fExclusionPatternStatus.setError(NewWizardMessages.getString("ExclusionPatternEntryDialog.error.exists"));
			return;
		}
		
		fExclusionPattern= pattern; 
		fExclusionPatternStatus.setOK();
	}
	
		
	public String getExclusionPattern() {
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
		Class[] acceptedClasses= new Class[] { IFolder.class, IFile.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, false);
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();
		
		IPath initialPath= new Path(fExclusionPatternDialog.getText());
		IResource initialElement= null;
		IContainer curr= fCurrSourceFolder;
		int nSegments= initialPath.segmentCount();
		for (int i= 0; i < nSegments; i++) {
			IResource elem= curr.findMember(initialPath.segment(i));
			if (elem != null) {
				initialElement= elem;
			}
			if (elem instanceof IContainer) {
				curr= (IContainer) elem;
			} else {
				break;
			}
		}			

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(NewWizardMessages.getString("ExclusionPatternEntryDialog.ChooseExclusionPattern.title")); //$NON-NLS-1$
		dialog.setValidator(validator);
		dialog.setMessage(NewWizardMessages.getString("ExclusionPatternEntryDialog.ChooseExclusionPattern.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fCurrSourceFolder);
		dialog.setInitialSelection(initialElement);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IPath path= ((IResource)dialog.getFirstResult()).getFullPath();
			return path.removeFirstSegments(fCurrSourceFolder.getFullPath().segmentCount()).makeRelative();
		}
		return null;
	}
	


}