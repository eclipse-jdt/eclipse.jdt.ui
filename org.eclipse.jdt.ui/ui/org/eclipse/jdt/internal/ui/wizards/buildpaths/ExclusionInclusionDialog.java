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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

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

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

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
	
	private ListDialogField fInclusionPatternList;
	private ListDialogField fExclusionPatternList;
	private CPListElement fCurrElement;
	private IProject fCurrProject;
	
	private IContainer fCurrSourceFolder;
	
	private static final int IDX_ADD= 0;
	private static final int IDX_ADD_MULTIPLE= 1;
	private static final int IDX_EDIT= 2;
	private static final int IDX_REMOVE= 4;
	
		
	public ExclusionInclusionDialog(Shell parent, CPListElement entryToEdit, boolean focusOnExcluded) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		fCurrElement= entryToEdit;

		setTitle(NewWizardMessages.getString("ExclusionInclusionDialog.title")); //$NON-NLS-1$

		fCurrProject= entryToEdit.getJavaProject().getProject();
		IWorkspaceRoot root= fCurrProject.getWorkspace().getRoot();
		IResource res= root.findMember(entryToEdit.getPath());
		if (res instanceof IContainer) {
			fCurrSourceFolder= (IContainer) res;
		}	
		
		String excLabel= NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.pattern.label"); //$NON-NLS-1$
		ImageDescriptor excDescriptor= JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB;
		String[] excButtonLabels= new String[] {
				/* IDX_ADD */ NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.pattern.add"), //$NON-NLS-1$
				/* IDX_ADD_MULTIPLE */ NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.pattern.add.multiple"), //$NON-NLS-1$
				/* IDX_EDIT */ NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.pattern.edit"), //$NON-NLS-1$
				null,
				/* IDX_REMOVE */ NewWizardMessages.getString("ExclusionInclusionDialog.exclusion.pattern.remove") //$NON-NLS-1$
			};
		
		
		String incLabel= NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.pattern.label"); //$NON-NLS-1$
		ImageDescriptor incDescriptor= JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB;
		String[] incButtonLabels= new String[] {
				/* IDX_ADD */ NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.pattern.add"), //$NON-NLS-1$
				/* IDX_ADD_MULTIPLE */ NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.pattern.add.multiple"), //$NON-NLS-1$
				/* IDX_EDIT */ NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.pattern.edit"), //$NON-NLS-1$
				null,
				/* IDX_REMOVE */ NewWizardMessages.getString("ExclusionInclusionDialog.inclusion.pattern.remove") //$NON-NLS-1$
			};	
		
		fExclusionPatternList= createListContents(entryToEdit, CPListElement.EXCLUSION, excLabel, excDescriptor, excButtonLabels);
		fInclusionPatternList= createListContents(entryToEdit, CPListElement.INCLUSION, incLabel, incDescriptor, incButtonLabels);
		if (focusOnExcluded) {
			fExclusionPatternList.postSetFocusOnDialogField(parent.getDisplay());
		} else {
			fInclusionPatternList.postSetFocusOnDialogField(parent.getDisplay());
		}
	}
	
	
	private ListDialogField createListContents(CPListElement entryToEdit, String key, String label, ImageDescriptor descriptor, String[] buttonLabels) {
		ExclusionPatternAdapter adapter= new ExclusionPatternAdapter();
		
		ListDialogField patternList= new ListDialogField(adapter, buttonLabels, new ExclusionInclusionLabelProvider(descriptor));
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
		patternList.enableButton(IDX_ADD_MULTIPLE, fCurrSourceFolder != null);
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
		labelField.setLabelText(NewWizardMessages.getFormattedString("ExclusionInclusionDialog.description", name)); //$NON-NLS-1$
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
		} else if (index == IDX_ADD_MULTIPLE) {
			addMultipleEntries(field);
		}
	}
	
	protected void doDoubleClicked(ListDialogField field) {
		editEntry(field);
	}
	
	protected void doSelectionChanged(ListDialogField field) {
		List selected= field.getSelectedElements();
		field.enableButton(IDX_EDIT, canEdit(selected));
        field.enableButton(IDX_REMOVE, canRemove(selected));
	}
    
    protected boolean canRemove(List selected) {
        IJavaProject project= fCurrElement.getJavaProject();
        IClasspathEntry entry= fCurrElement.getClasspathEntry();
        IPath rootPath= fCurrElement.getPath();
        
        try {
            IClasspathEntry[] cpEntries= project.getRawClasspath();
            for(int i= 0; i < cpEntries.length; i++) {
                if (cpEntries[i].getEntryKind() != IClasspathEntry.CPE_SOURCE)
                    continue;
                Iterator iterator= selected.iterator();
                while(iterator.hasNext()) {
                    IPath path= rootPath.append((String)iterator.next()).removeTrailingSeparator();
                    if (cpEntries[i].getPath().equals(path))
                        return false;
                }
            }
        } catch(JavaModelException e) {
            JavaPlugin.log(e);
        }
        
        Iterator iterator= selected.iterator();
        if (entry.getOutputLocation() != null) {
            while(iterator.hasNext()) {
                if (completeName(entry.getOutputLocation().lastSegment()).equals(iterator.next()))
                    return false;
            }
        }
        return true;
    }
    
    private String completeName(String name) {
        if (!name.endsWith(".java")) { //$NON-NLS-1$
            name= name + "/"; //$NON-NLS-1$
            name= name.replace('.', '/');
            return name;
        }
        return name;
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
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), isExclusion(field), entry, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.replaceElement(entry, dialog.getExclusionPattern());
		}
	}
	
	private boolean isExclusion(ListDialogField field) {
		return field == fExclusionPatternList;
	}


	private void addEntry(ListDialogField field) {
		List existing= field.getElements();
		ExclusionInclusionEntryDialog dialog= new ExclusionInclusionEntryDialog(getShell(), isExclusion(field), null, existing, fCurrElement);
		if (dialog.open() == Window.OK) {
			field.addElement(dialog.getExclusionPattern());
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
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.EXCLUSION_PATTERN_DIALOG);
	}
	
	private void addMultipleEntries(ListDialogField field) {
		String title, message;
		if (isExclusion(field)) {
			title= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseExclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseExclusionPattern.description"); //$NON-NLS-1$
		} else {
			title= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseInclusionPattern.title"); //$NON-NLS-1$
			message= NewWizardMessages.getString("ExclusionInclusionDialog.ChooseInclusionPattern.description"); //$NON-NLS-1$
		}
		
		IPath[] res= ExclusionInclusionEntryDialog.chooseExclusionPattern(getShell(), fCurrSourceFolder, title, message, null, true);
		if (res != null) {
			for (int i= 0; i < res.length; i++) {
				field.addElement(res[i].toString());
			}
		}
	}
}
