/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class ExclusionInclusionDialog extends StatusDialog {
	
	private static class ExclusionInclusionLabelProvider extends LabelProvider {
		
		private Image fElementImage;

		public ExclusionInclusionLabelProvider(ImageDescriptor descriptor) {
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
	
	
	private ListDialogField fExclusionPatternList;
	private CPListElement fCurrElement;
	private IProject fCurrProject;
	
	private IContainer fCurrSourceFolder;
	private boolean fIsExclusion;
	
	private static final int IDX_ADD= 0;
	private static final int IDX_ADD_MULTIPLE= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 4;
	
		
	public ExclusionInclusionDialog(Shell parent, CPListElement entryToEdit, boolean isExclusion) {
		super(parent);
		fCurrElement= entryToEdit;
		fIsExclusion= isExclusion;
		
		String label;
		ImageDescriptor descriptor;
		
		if (isExclusion) {
			setTitle(NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.title")); //$NON-NLS-1$
			label= NewWizardMessages.getFormattedString("ExclusionInclusionDialog.exclusion.pattern.label", entryToEdit.getPath().makeRelative().toString()); //$NON-NLS-1$
			descriptor= JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB;
		} else {
			setTitle(NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.title")); //$NON-NLS-1$
			label= NewWizardMessages.getFormattedString("ExclusionInclusionDialog.inclusion.pattern.label", entryToEdit.getPath().makeRelative().toString()); //$NON-NLS-1$
			descriptor= JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB;
		}
			
		String[] buttonLabels= new String[] {
			/* IDX_ADD */ NewWizardMessages.getString("ExclusionInclusionDialog.pattern.add"), //$NON-NLS-1$
			/* IDX_ADD_MULTIPLE */ NewWizardMessages.getString("ExclusionInclusionDialog.pattern.add.multiple"), //$NON-NLS-1$
			/* IDX_EDIT */ NewWizardMessages.getString("ExclusionInclusionDialog.pattern.edit"), //$NON-NLS-1$
			null,
			/* IDX_REMOVE */ NewWizardMessages.getString("ExclusionInclusionDialog.pattern.remove") //$NON-NLS-1$
		};

		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		
		fExclusionPatternList= new ListDialogField(adapter, buttonLabels, new ExclusionInclusionLabelProvider(descriptor));
		fExclusionPatternList.setDialogFieldListener(adapter);
		fExclusionPatternList.setLabelText(label);
		fExclusionPatternList.setRemoveButtonIndex(IDX_REMOVE);
		fExclusionPatternList.enableButton(IDX_EDIT, false);
	
		fCurrProject= entryToEdit.getJavaProject().getProject();
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		IResource res= root.findMember(entryToEdit.getPath());
		if (res instanceof IContainer) {
			fCurrSourceFolder= (IContainer) res;
		}				
		
		String key= isExclusion ? CPListElement.EXCLUSION : CPListElement.INCLUSION;
		
		IPath[] pattern= (IPath[]) entryToEdit.getAttribute(key);
		
		ArrayList elements= new ArrayList(pattern.length);
		for (int i= 0; i < pattern.length; i++) {
			elements.add(pattern[i].toString());
		}
		fExclusionPatternList.setElements(elements);
		fExclusionPatternList.selectFirstElement();
		fExclusionPatternList.enableButton(IDX_ADD_MULTIPLE, fCurrSourceFolder != null);
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
		
		applyDialogFont(composite);		
		return composite;
	}
	
	protected void doCustomButtonPressed(ListDialogField field, int index) {
		if (index == IDX_ADD) {
			addEntry();
		} else if (index == IDX_EDIT) {
			editEntry();
		} else if (index == IDX_ADD_MULTIPLE) {
			addMultipleEntries();
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
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), fIsExclusion, entry, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			fExclusionPatternList.replaceElement(entry, dialog.getExclusionPattern());
		}
	}
	
	private void addEntry() {
		List existing= fExclusionPatternList.getElements();
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), fIsExclusion, null, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
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
	
	private void addMultipleEntries() {
		Class[] acceptedClasses= new Class[] { IFolder.class, IFile.class };
		ISelectionStatusValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();
		
		IResource initialElement= null;
		String title, message;
		if (fIsExclusion) {
			title= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseExclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseExclusionPattern.description"); //$NON-NLS-1$
		} else {
			title= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseInclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseInclusionPattern.description"); //$NON-NLS-1$
		}
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setTitle(title);
		dialog.setValidator(validator);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(fCurrSourceFolder);
		dialog.setInitialSelection(initialElement);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == Window.OK) {
			Object[] objects= dialog.getResult();
			int existingSegments= fCurrSourceFolder.getFullPath().segmentCount();
			
			for (int i= 0; i < objects.length; i++) {
				IResource curr= (IResource) objects[i];
				IPath path= curr.getFullPath().removeFirstSegments(existingSegments).makeRelative();
				String res;
				if (curr instanceof IContainer) {
					res= path.addTrailingSeparator().toString();
				} else {
					res= path.toString();
				}
				fExclusionPatternList.addElement(res);
			}
		}
	}	
	
}
