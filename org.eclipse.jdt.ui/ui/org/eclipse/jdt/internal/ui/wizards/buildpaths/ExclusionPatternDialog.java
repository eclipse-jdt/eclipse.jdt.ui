/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

public class ExclusionPatternDialog extends StatusDialog {
	
	private StringButtonDialogField fExclusionPatternDialog;
	private StatusInfo fExclusionPatternStatus;
	
	private IProject fCurrProject;
	private IPath[] fExclusionPattern;
		
	public ExclusionPatternDialog(Shell parent, CPListElement entryToEdit) {
		super(parent);
		setTitle(NewWizardMessages.getString("ExclusionPatternDialog.title"));
		fExclusionPatternStatus= new StatusInfo();
		
		String label= NewWizardMessages.getFormattedString("ExclusionPatternDialog.pattern.label", entryToEdit.getPath().makeRelative().toString());
		
		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		fExclusionPatternDialog= new StringButtonDialogField(adapter);
		fExclusionPatternDialog.setLabelText(label);
		fExclusionPatternDialog.setButtonLabel(NewWizardMessages.getString("ExclusionPatternDialog.pattern.button"));
		fExclusionPatternDialog.setDialogFieldListener(adapter);
		
		fCurrProject= entryToEdit.getJavaProject().getProject();
		
		IPath[] pattern= (IPath[]) entryToEdit.getAttribute(CPListElement.EXCLUSION);
		if (pattern == null) {
			fExclusionPatternDialog.setText(""); //$NON-NLS-1$
		} else {
			fExclusionPatternDialog.setText(getPatternString(pattern));
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
		IPath[] pattern= chooseExclusionPattern();
		if (pattern != null) {
			fExclusionPatternDialog.setText(getPatternString(pattern));
		}
	}

	private String getPatternString(IPath[] pattern) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < pattern.length; i++) {
			if (i > 0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(pattern[i].toString());
		}
		return buf.toString();
	}
	
	
	protected void doStatusLineUpdate() {
		checkIfPatternValid();
		updateStatus(fExclusionPatternStatus);
	}		
	
	protected void checkIfPatternValid() {
		ArrayList res= new ArrayList();
		String pattern= fExclusionPatternDialog.getText();
		StringTokenizer tok= new StringTokenizer(pattern, File.pathSeparator);
		while (tok.hasMoreTokens()) {
			String curr= tok.nextToken();
			res.add(new Path(curr));
		}
		fExclusionPattern= (IPath[]) res.toArray(new IPath[res.size()]);
		fExclusionPatternStatus.setOK();
	}
	
		
	public IPath[] getExclusionPattern() {
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

	private IPath[] chooseExclusionPattern() {
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