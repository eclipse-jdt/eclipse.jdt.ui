/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

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

public class TypeRestrictionDialog extends StatusDialog {
	
	private static class TypeRestrictionLabelProvider extends LabelProvider {
		
		private Image fElementImage;

		public TypeRestrictionLabelProvider(ImageDescriptor descriptor) {
			ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
			fElementImage= registry.get(descriptor);
		}
		
		public Image getImage(Object element) {
			return fElementImage;
		}

		public String getText(Object element) {
			return (String) element;
		}

	}
	
	private ListDialogField fInclusionPatternList;
	private ListDialogField fExclusionPatternList;
	private CPListElement fCurrElement;
	//private IProject fCurrProject;
	
	//private IContainer fCurrSourceFolder;
	
	private static final int IDX_ADD= 0;
	//private static final int IDX_ADD_MULTIPLE= 1;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 3;
	
		
	public TypeRestrictionDialog(Shell parent, CPListElement entryToEdit, boolean focusOnExcluded) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fCurrElement= entryToEdit;

		setTitle(NewWizardMessages.getString("TypeRestrictionDialog.title")); //$NON-NLS-1$

		//fCurrProject= entryToEdit.getJavaProject().getProject();
		//IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		//IResource res= root.findMember(entryToEdit.getPath());
		//if (res instanceof IContainer) {
		//	fCurrSourceFolder= (IContainer) res;
		//}	
		
		String excLabel= NewWizardMessages.getString("TypeRestrictionDialog.exclusion.pattern.label"); //$NON-NLS-1$
		ImageDescriptor excDescriptor= JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB;
		String[] excButtonLabels= new String[] {
				/* IDX_ADD */ NewWizardMessages.getString("TypeRestrictionDialog.exclusion.pattern.add"), //$NON-NLS-1$
			//	/* IDX_ADD_MULTIPLE */ NewWizardMessages.getString("TypeRestrictionDialog.exclusion.pattern.add.multiple"), //$NON-NLS-1$
				/* IDX_EDIT */ NewWizardMessages.getString("TypeRestrictionDialog.exclusion.pattern.edit"), //$NON-NLS-1$
				null,
				/* IDX_REMOVE */ NewWizardMessages.getString("TypeRestrictionDialog.exclusion.pattern.remove") //$NON-NLS-1$
			};
		
		
		String incLabel= NewWizardMessages.getString("TypeRestrictionDialog.inclusion.pattern.label"); //$NON-NLS-1$
		ImageDescriptor incDescriptor= JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB;
		String[] incButtonLabels= new String[] {
				/* IDX_ADD */ NewWizardMessages.getString("TypeRestrictionDialog.inclusion.pattern.add"), //$NON-NLS-1$
			//	/* IDX_ADD_MULTIPLE */ NewWizardMessages.getString("TypeRestrictionDialog.inclusion.pattern.add.multiple"), //$NON-NLS-1$
				/* IDX_EDIT */ NewWizardMessages.getString("TypeRestrictionDialog.inclusion.pattern.edit"), //$NON-NLS-1$
				null,
				/* IDX_REMOVE */ NewWizardMessages.getString("TypeRestrictionDialog.inclusion.pattern.remove") //$NON-NLS-1$
			};	
		
		fExclusionPatternList= createListContents(entryToEdit, CPListElement.EXCLUSION, excLabel, excDescriptor, excButtonLabels);
	//	fExclusionPatternList.enableButton(IDX_ADD_MULTIPLE, false);
		fInclusionPatternList= createListContents(entryToEdit, CPListElement.INCLUSION, incLabel, incDescriptor, incButtonLabels);
	//	fInclusionPatternList.enableButton(IDX_ADD_MULTIPLE, false);
		if (focusOnExcluded) {
			fExclusionPatternList.postSetFocusOnDialogField(parent.getDisplay());
		} else {
			fInclusionPatternList.postSetFocusOnDialogField(parent.getDisplay());
		}
	}
	
	
	private ListDialogField createListContents(CPListElement entryToEdit, String key, String label, ImageDescriptor descriptor, String[] buttonLabels) {
		TypeRestrictionAdapter adapter= new TypeRestrictionAdapter();
		
		ListDialogField patternList= new ListDialogField(adapter, buttonLabels, new TypeRestrictionLabelProvider(descriptor));
		patternList.setDialogFieldListener(adapter);
		patternList.setLabelText(label);
		patternList.setRemoveButtonIndex(IDX_REMOVE);
		patternList.enableButton(IDX_EDIT, false);
	
		IPath[] pattern= (IPath[]) entryToEdit.getAttribute(key);
		
		ArrayList elements= new ArrayList(pattern.length);
		for (int i= 0; i < pattern.length; i++) {
			elements.add(pattern[i].toString());
		}
		patternList.setElements(elements);
		patternList.selectFirstElement();
	//	patternList.enableButton(IDX_ADD_MULTIPLE, fCurrSourceFolder != null);
		patternList.setViewerSorter(new ViewerSorter());
		return patternList;
	}


	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		DialogField labelField= new DialogField();
		String name= fCurrElement.getPath().makeRelative().toString();
		labelField.setLabelText(NewWizardMessages.getFormattedString("TypeRestrictionDialog.description", name)); //$NON-NLS-1$
		labelField.doFillIntoGrid(inner, 2);
		
		fInclusionPatternList.doFillIntoGrid(inner, 3);
		LayoutUtil.setHorizontalSpan(fInclusionPatternList.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fInclusionPatternList.getListControl(null));
		
		fExclusionPatternList.doFillIntoGrid(inner, 3);
		LayoutUtil.setHorizontalSpan(fExclusionPatternList.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fExclusionPatternList.getListControl(null));
		
		applyDialogFont(composite);		
		return composite;
	}
	
	protected void doCustomButtonPressed(ListDialogField field, int index) {
		if (index == IDX_ADD) {
			addEntry(field);
		} else if (index == IDX_EDIT) {
			editEntry(field);
	//	} else if (index == IDX_ADD_MULTIPLE) {
	//		addMultipleEntries(field);
		}
	}
	
	protected void doDoubleClicked(ListDialogField field) {
		editEntry(field);
	}
	
	protected void doSelectionChanged(ListDialogField field) {
		List selected= field.getSelectedElements();
		field.enableButton(IDX_EDIT, canEdit(selected));
	}
	
	private boolean canEdit(List selected) {
		return selected.size() == 1;
	}
	
	private void editEntry(ListDialogField field) {
		
		List selElements= field.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		List existing= field.getElements();
		String entry= (String) selElements.get(0);
		TypeRestrictionEntryDialog dialog= new TypeRestrictionEntryDialog(getShell(), isExclusion(field), entry, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.replaceElement(entry, dialog.getExclusionPattern());
		}
	}
	
	private boolean isExclusion(ListDialogField field) {
		return field == fExclusionPatternList;
	}


	private void addEntry(ListDialogField field) {
		List existing= field.getElements();
		TypeRestrictionEntryDialog dialog= new TypeRestrictionEntryDialog(getShell(), isExclusion(field), null, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.addElement(dialog.getExclusionPattern());
		}
	}	
	
	
		
	// -------- TypeRestrictionAdapter --------

	private class TypeRestrictionAdapter implements IListAdapter, IDialogFieldListener {
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
	
	
	private IPath[] getPattern(ListDialogField field) {
		Object[] arr= field.getElements().toArray();
		Arrays.sort(arr);
		IPath[] res= new IPath[arr.length];
		for (int i= 0; i < res.length; i++) {
			res[i]= new Path((String) arr[i]);
		}
		return res;
	}
	
	public IPath[] getExclusionPattern() {
		return getPattern(fExclusionPatternList);
	}
	
	public IPath[] getInclusionPattern() {
		return getPattern(fInclusionPatternList);
	}
		
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.EXCLUSION_PATTERN_DIALOG);
	}
	/*
	private void addMultipleEntries(ListDialogField field) {
		String title, message;
		if (isExclusion(field)) {
			title= NewWizardMessages.getString("TypeRestrictionDialog.ChooseExclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("TypeRestrictionDialog.ChooseExclusionPattern.description"); //$NON-NLS-1$
		} else {
			title= NewWizardMessages.getString("TypeRestrictionDialog.ChooseInclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("TypeRestrictionDialog.ChooseInclusionPattern.description"); //$NON-NLS-1$
		}
		
		IPath[] res= TypeRestrictionEntryDialog.chooseExclusionPattern(getShell(), fCurrSourceFolder, title, message, null, true);
		if (res != null) {
			for (int i= 0; i < res.length; i++) {
				field.addElement(res[i].toString());
			}
		}
	}
	*/
}
