/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class ExclusionPatternDialog extends StatusDialog {
	
	private static class ExclusionPatternLabelProvider extends LabelProvider {
				
		public Image getImage(Object element) {
			ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
			return registry.get(JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB);
		}

		public String getText(Object element) {
			return (String) element;
		}

	}
	
	
	private ListDialogField fExclusionPatternList;
	private CPListElement fCurrElement;
	private IProject fCurrProject;
	
	private static final int IDX_ADD= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 3;
	
		
	public ExclusionPatternDialog(Shell parent, CPListElement entryToEdit) {
		super(parent);
		fCurrElement= entryToEdit;
		setTitle(NewWizardMessages.getString("ExclusionPatternDialog.title")); //$NON-NLS-1$

		String label= NewWizardMessages.getFormattedString("ExclusionPatternDialog.pattern.label", entryToEdit.getPath().makeRelative().toString()); //$NON-NLS-1$
		
		String[] buttonLabels= new String[] {
			/* IDX_ADD */ NewWizardMessages.getString("ExclusionPatternDialog.pattern.add"), //$NON-NLS-1$
			/* IDX_EDIT */ NewWizardMessages.getString("ExclusionPatternDialog.pattern.edit"), //$NON-NLS-1$
			null,
			/* IDX_REMOVE */ NewWizardMessages.getString("ExclusionPatternDialog.pattern.remove") //$NON-NLS-1$
		};

		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();

		fExclusionPatternList= new ListDialogField(adapter, buttonLabels, new ExclusionPatternLabelProvider());
		fExclusionPatternList.setDialogFieldListener(adapter);
		fExclusionPatternList.setLabelText(label);
		fExclusionPatternList.setRemoveButtonIndex(IDX_REMOVE);
		fExclusionPatternList.enableButton(IDX_EDIT, false);
	
		fCurrProject= entryToEdit.getJavaProject().getProject();
		
		IPath[] pattern= (IPath[]) entryToEdit.getAttribute(CPListElement.EXCLUSION);
		
		ArrayList elements= new ArrayList(pattern.length);
		for (int i= 0; i < pattern.length; i++) {
			elements.add(pattern[i].toString());
		}
		fExclusionPatternList.setElements(elements);
		fExclusionPatternList.selectFirstElement();
	}
	
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		
		fExclusionPatternList.doFillIntoGrid(inner, 3);
		LayoutUtil.setHorizontalSpan(fExclusionPatternList.getLabelControl(null), 2);
		
		return composite;
	}
	
	protected void doCustomButtonPressed(ListDialogField field, int index) {
		if (index == IDX_ADD) {
			addEntry();
		} else if (index == IDX_EDIT) {
			editEntry();
		}
	}
	
	protected void doDoubleClicked(ListDialogField field) {
		editEntry();
	}
	
	protected void doSelectionChanged(ListDialogField field) {
		List selected= field.getSelectedElements();
		fExclusionPatternList.enableButton(IDX_EDIT, canEdit(selected));
	}
	
	private boolean canEdit(List selected) {
		return selected.size() == 1;
	}
	
	private void editEntry() {
		
		List selElements= fExclusionPatternList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		List existing= fExclusionPatternList.getElements();
		String entry= (String) selElements.get(0);
		ExclusionPatternEntryDialog dialog= new ExclusionPatternEntryDialog(getShell(), entry, existing, fCurrElement);
		if (dialog.open() == ExclusionPatternEntryDialog.OK) {
			fExclusionPatternList.replaceElement(entry, dialog.getExclusionPattern());
		}
	}
	
	private void addEntry() {
		List existing= fExclusionPatternList.getElements();
		ExclusionPatternEntryDialog dialog= new ExclusionPatternEntryDialog(getShell(), null, existing, fCurrElement);
		if (dialog.open() == ExclusionPatternEntryDialog.OK) {
			fExclusionPatternList.addElement(dialog.getExclusionPattern());
		}
	}	
	
	
		
	// -------- ExclusionPatternAdapter --------

	private class ExclusionPatternAdapter implements IListAdapter, IDialogFieldListener {
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		public void customButtonPressed(ListDialogField field, int index) {
			doCustomButtonPressed(field, index);
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void selectionChanged(ListDialogField field) {
			doSelectionChanged(field);
		}
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
			doDoubleClicked(field);
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
		}
		
	}
	
	protected void doStatusLineUpdate() {
	}		
	
	protected void checkIfPatternValid() {
	}
	
		
	public IPath[] getExclusionPattern() {
		IPath[] res= new IPath[fExclusionPatternList.getSize()];
		for (int i= 0; i < res.length; i++) {
			String entry= (String) fExclusionPatternList.getElement(i);
			res[i]= new Path(entry);
		}
		return res;
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.EXCLUSION_PATTERN_DIALOG);
	}
	
}