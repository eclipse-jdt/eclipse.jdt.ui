/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.INewWizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.actions.AbstractOpenWizardAction;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;


public class SourceContainerWorkbookPage extends BuildPathBasePage {

	private class OpenBuildPathWizardAction extends AbstractOpenWizardAction implements IPropertyChangeListener {

		private final BuildPathWizard fWizard;
		private final List<Object> fSelectedElements;

		public OpenBuildPathWizardAction(BuildPathWizard wizard) {
			fWizard= wizard;
			addPropertyChangeListener(this);
			fSelectedElements= fFoldersList.getSelectedElements();
		}

		@Override
		protected INewWizard createWizard() throws CoreException {
			return fWizard;
		}

		/**
		 * {@inheritDoc}
		 * @since 3.7
		 */
		@Override
		protected Shell getShell() {
			return SourceContainerWorkbookPage.this.getShell();
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (IAction.RESULT.equals(event.getProperty())) {
				if (event.getNewValue().equals(Boolean.TRUE)) {
					finishWizard();
				} else {
					fWizard.cancel();
				}
			}
		}

		protected void finishWizard() {
			List<CPListElement> insertedElements= fWizard.getInsertedElements();
			refresh(insertedElements, fWizard.getRemovedElements(), fWizard.getModifiedElements(), fWizard.getOutputLocation());

			if (insertedElements.isEmpty()) {
				fFoldersList.postSetSelection(new StructuredSelection(fSelectedElements));
			}
		}

	}

	private static AddSourceFolderWizard newSourceFolderWizard(CPListElement element, List<CPListElement> existingElements, String outputLocation, boolean newFolder) {
		CPListElement[] existing= existingElements.toArray(new CPListElement[existingElements.size()]);
		AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, element, new Path(outputLocation).makeAbsolute(), false, newFolder, newFolder, newFolder?CPListElement.isProjectSourceFolder(existing, element.getJavaProject()):false, newFolder);
		wizard.setDoFlushChange(false);
		return wizard;
	}

	private static AddSourceFolderWizard newLinkedSourceFolderWizard(CPListElement element, List<CPListElement> existingElements, String outputLocation, boolean newFolder) {
		CPListElement[] existing= existingElements.toArray(new CPListElement[existingElements.size()]);
		AddSourceFolderWizard wizard= new AddSourceFolderWizard(existing, element, new Path(outputLocation).makeAbsolute(), true, newFolder, newFolder, newFolder?CPListElement.isProjectSourceFolder(existing, element.getJavaProject()):false, newFolder);
		wizard.setDoFlushChange(false);
		return wizard;
	}

	private static EditFilterWizard newEditFilterWizard(CPListElement element, List<CPListElement> existingElements, String outputLocation) {
		CPListElement[] existing= existingElements.toArray(new CPListElement[existingElements.size()]);
		EditFilterWizard result = new EditFilterWizard(existing, element, new Path(outputLocation).makeAbsolute());
		result.setDoFlushChange(false);
		return result;
	}

	private final ListDialogField<CPListElement> fClassPathList;
	private IJavaProject fCurrJProject;

	private final TreeListDialogField<CPListElement> fFoldersList;

	private final StringDialogField fOutputLocationField;

	private final SelectionButtonDialogField fUseFolderOutputs;

	private final int IDX_ADD= 0;
	private final int IDX_ADD_LINK= 1;
	private final int IDX_EDIT= 3;
	private final int IDX_REMOVE= 4;

	public SourceContainerWorkbookPage(ListDialogField<CPListElement> classPathList, StringDialogField outputLocationField) {
		fClassPathList= classPathList;

		fOutputLocationField= outputLocationField;

		fSWTControl= null;

		SourceContainerAdapter adapter= new SourceContainerAdapter();

		String[] buttonLabels;

		buttonLabels= new String[] {
			NewWizardMessages.SourceContainerWorkbookPage_folders_add_button,
			NewWizardMessages.SourceContainerWorkbookPage_folders_link_source_button,
			/* 1 */ null,
			NewWizardMessages.SourceContainerWorkbookPage_folders_edit_button,
			NewWizardMessages.SourceContainerWorkbookPage_folders_remove_button
		};

		fFoldersList= new TreeListDialogField<>(adapter, buttonLabels, new CPListLabelProvider());
		fFoldersList.setDialogFieldListener(adapter);
		fFoldersList.setLabelText(NewWizardMessages.SourceContainerWorkbookPage_folders_label);

		fFoldersList.setViewerComparator(new CPListElementSorter());
		fFoldersList.enableButton(IDX_EDIT, false);

		fUseFolderOutputs= new SelectionButtonDialogField(SWT.CHECK);
		fUseFolderOutputs.setSelection(false);
		fUseFolderOutputs.setLabelText(NewWizardMessages.SourceContainerWorkbookPage_folders_check);
		fUseFolderOutputs.setDialogFieldListener(adapter);
	}

	@Override
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			updateFoldersList();
		} else {
			Display.getDefault().asyncExec(this::updateFoldersList);
		}
	}

	private void updateFoldersList() {
		if (fSWTControl == null || fSWTControl.isDisposed()) {
			return;
		}

		ArrayList<CPListElement> folders= new ArrayList<>();

		boolean useFolderOutputs= false;
		List<CPListElement> cpelements= fClassPathList.getElements();
		for (CPListElement cpe : cpelements) {
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				folders.add(cpe);
				boolean hasOutputFolder= (cpe.getAttribute(CPListElement.OUTPUT) != null);
				if (hasOutputFolder) {
					useFolderOutputs= true;
				}

			}
		}
		fFoldersList.setElements(folders);
		fUseFolderOutputs.setSelection(useFolderOutputs);

		for (CPListElement cpe : folders) {
			IPath[] ePatterns= (IPath[]) cpe.getAttribute(CPListElement.EXCLUSION);
			IPath[] iPatterns= (IPath[])cpe.getAttribute(CPListElement.INCLUSION);
			boolean hasOutputFolder= (cpe.getAttribute(CPListElement.OUTPUT) != null);
			if (ePatterns.length > 0 || iPatterns.length > 0 || hasOutputFolder) {
				fFoldersList.expandElement(cpe, 3);
			}
		}
	}

	@Override
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		Composite composite= new Composite(parent, SWT.NONE);

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fFoldersList, fUseFolderOutputs , fOutputLocationField}, true, SWT.DEFAULT, SWT.DEFAULT);
		BidiUtils.applyBidiProcessing(fOutputLocationField.getTextControl(null), StructuredTextTypeHandlerFactory.FILE);
		LayoutUtil.setHorizontalGrabbing(fFoldersList.getTreeControl(null));

		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fFoldersList.setButtonsMinWidth(buttonBarWidth);

		fSWTControl= composite;

		// expand
		List<CPListElement> elements= fFoldersList.getElements();
		for (CPListElement elem : elements) {
			IPath[] exclusionPatterns= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
			IPath[] inclusionPatterns= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
			IPath output= (IPath) elem.getAttribute(CPListElement.OUTPUT);
			if (exclusionPatterns.length > 0 || inclusionPatterns.length > 0 || output != null) {
				fFoldersList.expandElement(elem, 3);
			}
		}
		return composite;
	}

	private class SourceContainerAdapter implements ITreeListAdapter<CPListElement>, IDialogFieldListener {

		private final Object[] EMPTY_ARR= new Object[0];

		// -------- IListAdapter --------
		@Override
		public void customButtonPressed(TreeListDialogField<CPListElement> field, int index) {
			sourcePageCustomButtonPressed(field, index);
		}

		@Override
		public void selectionChanged(TreeListDialogField<CPListElement> field) {
			sourcePageSelectionChanged(field);
		}

		@Override
		public void doubleClicked(TreeListDialogField<CPListElement> field) {
			sourcePageDoubleClicked(field);
		}

		@Override
		public void keyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
			sourcePageKeyPressed(field, event);
		}

		@Override
		public Object[] getChildren(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren(!fUseFolderOutputs.isSelected());
			}
			return EMPTY_ARR;
		}

		@Override
		public Object getParent(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(TreeListDialogField<CPListElement> field, Object element) {
			return (element instanceof CPListElement);
		}

		// ---------- IDialogFieldListener --------
		@Override
		public void dialogFieldChanged(DialogField field) {
			sourcePageDialogFieldChanged(field);
		}

	}

	protected void sourcePageKeyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
		if (field == fFoldersList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List<Object> selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}
	}

	protected void sourcePageDoubleClicked(TreeListDialogField<CPListElement> field) {
		if (field == fFoldersList) {
			List<Object> selection= field.getSelectedElements();
			if (canEdit(selection)) {
				editEntry();
			}
		}
	}

	protected void sourcePageCustomButtonPressed(DialogField field, int index) {
		if (field == fFoldersList) {
			switch (index) {
			case IDX_ADD:
				IProject project= fCurrJProject.getProject();
				if (project.isAccessible() && hasFolders(project)) {
					List<CPListElement> existingElements= fFoldersList.getElements();
					CPListElement[] existing= existingElements.toArray(new CPListElement[existingElements.size()]);
					CreateMultipleSourceFoldersDialog dialog= new CreateMultipleSourceFoldersDialog(fCurrJProject, existing, fOutputLocationField.getText(), getShell());
					if (dialog.open() == Window.OK) {
						refresh(dialog.getInsertedElements(), dialog.getRemovedElements(), dialog.getModifiedElements(), dialog.getOutputLocation());
					}
				} else {
					CPListElement newElement= new CPListElement(fCurrJProject, IClasspathEntry.CPE_SOURCE);
					AddSourceFolderWizard wizard= newSourceFolderWizard(newElement, fFoldersList.getElements(), fOutputLocationField.getText(), true);
					OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
					action.run();
				}
				break;
			case IDX_ADD_LINK:
				CPListElement newElement= new CPListElement(fCurrJProject, IClasspathEntry.CPE_SOURCE);
				AddSourceFolderWizard wizard= newLinkedSourceFolderWizard(newElement, fFoldersList.getElements(), fOutputLocationField.getText(), true);
				OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
				action.run();
				break;
			case IDX_EDIT:
				editEntry();
				break;
			case IDX_REMOVE:
				removeEntry();
				break;
			default:
				break;
			}
		}
	}

	private boolean hasFolders(IContainer container) {

		try {
			for (IResource member : container.members()) {
				if (member instanceof IContainer) {
					return true;
				}
			}
		} catch (CoreException e) {
			// ignore
		}

		List<CPListElement> elements= fFoldersList.getElements();
		if (elements.size() > 1)
			return true;

		if (elements.isEmpty())
			return false;

		CPListElement single= elements.get(0);
		if (single.getPath().equals(fCurrJProject.getPath()))
			return false;

		return true;
	}

	private void editEntry() {
		List<Object> selElements= fFoldersList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (fFoldersList.getIndexOfElement(elem) != -1) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}

	private void editElementEntry(CPListElement elem) {
		if (elem.getLinkTarget() != null) {
			AddSourceFolderWizard wizard= newLinkedSourceFolderWizard(elem, fFoldersList.getElements(), fOutputLocationField.getText(), false);
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		} else {
			AddSourceFolderWizard wizard= newSourceFolderWizard(elem, fFoldersList.getElements(), fOutputLocationField.getText(), false);
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		}
	}

	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		if (CPListElement.OUTPUT.equals(key)) {
			CPListElement selElement=  elem.getParent();
			OutputLocationDialog dialog= new OutputLocationDialog(getShell(), selElement, fClassPathList.getElements(), new Path(fOutputLocationField.getText()).makeAbsolute(), true);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.OUTPUT, dialog.getOutputLocation());
				fFoldersList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
			}
		} else if (CPListElement.EXCLUSION.equals(key) || CPListElement.INCLUSION.equals(key)) {
			EditFilterWizard wizard= newEditFilterWizard(elem.getParent(), fFoldersList.getElements(), fOutputLocationField.getText());
			OpenBuildPathWizardAction action= new OpenBuildPathWizardAction(wizard);
			action.run();
		} else if (CPListElement.IGNORE_OPTIONAL_PROBLEMS.equals(key)) {
			String newValue= "true".equals(elem.getValue()) ? null : "true"; //$NON-NLS-1$ //$NON-NLS-2$
			elem.setValue(newValue);
			fFoldersList.refresh(elem);
		} else if (CPListElement.TEST.equals(key)) {
			String newValue= "true".equals(elem.getValue()) ? null : "true"; //$NON-NLS-1$ //$NON-NLS-2$
			elem.setValue(newValue);
			fFoldersList.refresh(elem.getParent());
			fClassPathList.dialogFieldChanged(); // validate
		} else if (CPListElement.WITHOUT_TEST_CODE.equals(key)) {
			String newValue= "true".equals(elem.getValue()) ? null : "true"; //$NON-NLS-1$ //$NON-NLS-2$
			elem.setValue(newValue);
			fFoldersList.refresh(elem.getParent());
		} else {
			if (editCustomAttribute(getShell(), elem)) {
				fFoldersList.refresh();
				fClassPathList.dialogFieldChanged(); // validate
				checkAttributeEffect(key, fCurrJProject);
			}
		}
	}

	/**
	 * @param field the dialog field
	 */
	protected void sourcePageSelectionChanged(DialogField field) {
		List<Object> selected= fFoldersList.getSelectedElements();
		boolean isIgnoreOptionalProblems= selected.size() == 1
				&& selected.get(0) instanceof CPListElementAttribute
				&& CPListElement.IGNORE_OPTIONAL_PROBLEMS.equals(((CPListElementAttribute) selected.get(0)).getKey());
		boolean isTest= selected.size() == 1
				&& selected.get(0) instanceof CPListElementAttribute
				&& CPListElement.TEST.equals(((CPListElementAttribute) selected.get(0)).getKey());
		boolean isWithoutTestCode= selected.size() == 1
				&& selected.get(0) instanceof CPListElementAttribute
				&& CPListElement.WITHOUT_TEST_CODE.equals(((CPListElementAttribute) selected.get(0)).getKey());
		fFoldersList.getButton(IDX_EDIT).setText((isIgnoreOptionalProblems||isTest||isWithoutTestCode)
				? NewWizardMessages.SourceContainerWorkbookPage_folders_toggle_button
				: NewWizardMessages.SourceContainerWorkbookPage_folders_edit_button);
		fFoldersList.enableButton(IDX_EDIT, canEdit(selected));
		fFoldersList.enableButton(IDX_REMOVE, canRemove(selected));
		boolean noAttributes= containsOnlyTopLevelEntries(selected);
		fFoldersList.enableButton(IDX_ADD, noAttributes);
	}

	private void removeEntry() {
		List<Object> selElements= fFoldersList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					Object value= null;
					if (CPListElement.EXCLUSION.equals(key) || CPListElement.INCLUSION.equals(key)) {
						value= new Path[0];
					}
					attrib.getParent().setAttribute(key, value);
				} else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
			}
		}
		if (selElements.isEmpty()) {
			fFoldersList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			for (Object object : selElements) {
				CPListElement element= (CPListElement)object;
				if (element.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (CPListElement modified : ClasspathModifier.removeFilters(element.getPath(), fCurrJProject, fFoldersList.getElements())) {
						fFoldersList.refresh(modified);
						fFoldersList.expandElement(modified, 3);
					}
				}
			}
			fFoldersList.removeElements(selElements);
		}
	}

	private boolean canRemove(List<Object> selElements) {
		if (selElements.isEmpty()) {
			return false;
		}
		for (Object elem : selElements) {
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					if (CPListElement.INCLUSION.equals(key)
							|| CPListElement.EXCLUSION.equals(key)) {
						if (((IPath[]) attrib.getValue()).length == 0) {
							return false;
						}
					} else if (attrib.getValue() == null) {
						return false;
					}
				} else {
					if  (!canRemoveCustomAttribute(attrib)) {
						return false;
					}
				}
			} else if (elem instanceof CPListElement) {
				CPListElement curr= (CPListElement) elem;
				if (curr.getParentContainer() != null) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean canEdit(List<Object> selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			CPListElement cp= ((CPListElement)elem);
			if (cp.getPath().equals(cp.getJavaProject().getPath()))
				return false;

			return true;
		}
		if (elem instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) elem;
			if (attrib.isBuiltIn() || CPListElement.IGNORE_OPTIONAL_PROBLEMS.equals(attrib.getKey())) {
				return true;
			} else {
				return canEditCustomAttribute(attrib);
			}
		}
		return false;
	}

	private void sourcePageDialogFieldChanged(DialogField field) {
		if (fCurrJProject == null) {
			// not initialized
			return;
		}

		if (field == fUseFolderOutputs) {
			if (!fUseFolderOutputs.isSelected()) {
				int nFolders= fFoldersList.getSize();
				for (int i= 0; i < nFolders; i++) {
					CPListElement cpe= fFoldersList.getElement(i);
					cpe.setAttribute(CPListElement.OUTPUT, null);
				}
			}
			fFoldersList.refresh();
			fFoldersList.dialogFieldChanged(); // validate
		} else if (field == fFoldersList) {
			updateClasspathList();
			fClassPathList.dialogFieldChanged(); // validate
		}
	}


	private void updateClasspathList() {
		List<CPListElement> srcelements= fFoldersList.getElements();

		List<CPListElement> cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		int afterLastSourcePos= 0;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!srcelements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				} else if (lastRemovePos == nEntries) {
					afterLastSourcePos= i + 1;
				}
			}
		}

		if (!srcelements.isEmpty()) {
			int insertPos= Math.min(afterLastSourcePos, lastRemovePos);
			cpelements.addAll(insertPos, srcelements);
		}

		if (lastRemovePos != nEntries || !srcelements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		}
	}

	/*
	 * @see BuildPathBasePage#getSelection
	 */
	@Override
	public List<Object> getSelection() {
		return fFoldersList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */
	@Override
	public void setSelection(List<?> selElements, boolean expand) {
		fFoldersList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (Object selElement : selElements) {
				fFoldersList.expandElement(selElement, 1);
			}
		}
	}

	@Override
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_SOURCE;
	}

	private void refresh(List<CPListElement> insertedElements, List<?> removedElements, List<CPListElement> modifiedElements, IPath outputLocation) {
		fFoldersList.addElements(insertedElements);
		for (CPListElement element : insertedElements) {
			fFoldersList.expandElement(element, 3);
		}

		fFoldersList.removeElements(removedElements);

		for (CPListElement element : modifiedElements) {
			fFoldersList.refresh(element);
			fFoldersList.expandElement(element, 3);
		}

		fFoldersList.refresh(); //does enforce the order of the entries.
		if (!insertedElements.isEmpty()) {
			fFoldersList.postSetSelection(new StructuredSelection(insertedElements));
		}

		fOutputLocationField.setText(outputLocation.makeRelative().toOSString());
	}

    @Override
	public void setFocus() {
    	fFoldersList.setFocus();
    }

}
