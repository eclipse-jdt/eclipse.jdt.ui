/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaElementComparator;

import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.RootCPListElement.RootNodeChange;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;


public class ProjectsWorkbookPage extends BuildPathBasePage {

	private final int IDX_ADDPROJECT= 0;

	private final int IDX_EDIT= 2;
	private final int IDX_REMOVE= 3;

	private final ListDialogField<CPListElement> fClassPathList;
	private IJavaProject fCurrJProject;

	private final TreeListDialogField<CPListElement> fProjectsList;

	private final IWorkbenchPreferenceContainer fPageContainer;

	private boolean dragDropEnabled;
	private Object draggedItemsProject;
	private boolean fromModularProject;

	public ProjectsWorkbookPage(ListDialogField<CPListElement> classPathList, IWorkbenchPreferenceContainer pageContainer) {
		fClassPathList= classPathList;
		fPageContainer= pageContainer;
		fSWTControl= null;

		String[] buttonLabels= new String[] {
			NewWizardMessages.ProjectsWorkbookPage_projects_add_button,
			null,
			NewWizardMessages.ProjectsWorkbookPage_projects_edit_button,
			NewWizardMessages.ProjectsWorkbookPage_projects_remove_button
		};

		ProjectsAdapter adapter= new ProjectsAdapter();

		fProjectsList= new TreeListDialogField<>(adapter, buttonLabels, new CPListLabelProvider());
		fProjectsList.setDialogFieldListener(adapter);
		fProjectsList.setLabelText(NewWizardMessages.ProjectsWorkbookPage_projects_label);

		fProjectsList.enableButton(IDX_REMOVE, false);
		fProjectsList.enableButton(IDX_EDIT, false);

		fProjectsList.setViewerComparator(new CPListElementSorter());
	}

	@Override
	public void init(final IJavaProject jproject) {
		fCurrJProject= jproject;

		if (Display.getCurrent() != null) {
			updateProjectsList();
		} else {
			Display.getDefault().asyncExec(this::updateProjectsList);
		}
	}

	private void updateProjectsList() {
			if(JavaModelUtil.is9OrHigher(fCurrJProject)) {
				updateProjectsListWithRootNode();
				return;
			}
			// add the projects-cpentries that are already on the class path
			List<CPListElement> cpelements= fClassPathList.getElements();

			final List<CPListElement> checkedProjects= new ArrayList<>(cpelements.size());

			for (int i= cpelements.size() - 1 ; i >= 0; i--) {
				CPListElement cpelem= cpelements.get(i);
				if (isEntryKind(cpelem.getEntryKind())) {
					checkedProjects.add(cpelem);
				}
			}
			fProjectsList.setElements(checkedProjects);
	}

	boolean hasRootNodes(){
		if (fProjectsList == null)
			return false;
		if(fProjectsList.getSize()==0)
			return false;
		if(fProjectsList.getElement(0).isRootNodeForPath())
			return true;
		return false;
	}
	private void updateProjectsListWithRootNode() {
		RootCPListElement rootClasspath = new RootCPListElement(fCurrJProject, NewWizardMessages.PathRootWorkbookPage_classpath,false);
		RootCPListElement rootModulepath =  new RootCPListElement(fCurrJProject,NewWizardMessages.PathRootWorkbookPage_modulepath,true);

		// add the projects-cpentries that are already on the class path
		List<CPListElement> cpelements= fClassPathList.getElements();

		final List<CPListElement> checkedProjects= new ArrayList<>(cpelements.size());

		for (int i= cpelements.size() - 1 ; i >= 0; i--) {
			CPListElement cpelem= cpelements.get(i);
			if (isEntryKind(cpelem.getEntryKind())) {
				Object mod= cpelem.getAttribute(CPListElement.MODULE);
				if(mod == null) {
					rootClasspath.addCPListElement(cpelem);
				} else {
					rootModulepath.addCPListElement(cpelem);
				}
			}
		}
		checkedProjects.add(rootModulepath);
		checkedProjects.add(rootClasspath);

		fProjectsList.setTreeExpansionLevel(2);
		fProjectsList.setElements(checkedProjects);
		fProjectsList.enableButton(IDX_ADDPROJECT, false);
		if (!dragDropEnabled) {
			enableDragDropSupport();
		}
	}

	private void enableDragDropSupport() {
		dragDropEnabled= true;
		int ops= DND.DROP_MOVE;
		Transfer[] transfers= new Transfer[] { ResourceTransfer.getInstance(), FileTransfer.getInstance() };

		fProjectsList.getTreeViewer().addDragSupport(ops, transfers, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection ssel= (IStructuredSelection) fProjectsList.getTreeViewer().getSelection();
				if (ssel == null || ssel.isEmpty()) {
					event.doit= false;
				}
				if (ssel != null) {
					Object[] ele= ssel.toArray();
					for (Object element : ele) {
						if (element instanceof RootCPListElement) {
							event.doit= false;
							break;
						}
						if (element instanceof CPListElement) {
							CPListElement cpe= (CPListElement) element;
							List<CPListElement> elements= fProjectsList.getElements();
							for (Object cpListElement : elements) {
								if (cpListElement instanceof RootCPListElement) {
									RootCPListElement root= (RootCPListElement) cpListElement;
									if (root.getChildren().contains(cpe)) {
										fromModularProject= root.isModulePathRootNode();
										break;
									}
								}
							}
						}
					}
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection ssel= (IStructuredSelection) fProjectsList.getTreeViewer().getSelection();
				event.data= ssel.toArray();
				draggedItemsProject= ssel.toArray();
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				draggedItemsProject= null;
			}
		});

		fProjectsList.getTreeViewer().addDropSupport(ops, transfers, new ViewerDropAdapter(fProjectsList.getTreeViewer()) {
			@Override
			public boolean performDrop(Object data) {
				Object[] objects= (data == null) ? (Object[]) draggedItemsProject : (Object[]) data;
				if (objects == null)
					return false;
				Object target= getCurrentTarget();
				if (target instanceof RootCPListElement) {
					for (Object object : objects) {
						if (!(object instanceof CPListElement))
							return false;
						if(object instanceof RootCPListElement)
							return false;
						boolean contains= ((RootCPListElement) target).getChildren().contains(object);
						if (contains == true)
							return false;
						RootCPListElement rootNode= (RootCPListElement) target;
						boolean isModular= rootNode.isModulePathRootNode();
						RootNodeChange direction= RootNodeChange.fromOldAndNew(!isModular, isModular);
						if (direction != RootNodeChange.NoChange) {
							moveCPElementAcrossNode(fProjectsList, (CPListElement) object, direction);
						}
						((CPListElement) object).setAttribute(IClasspathAttribute.MODULE, isModular ? new ModuleEncapsulationDetail[0] : null);
					}
					return true;

				}
				return false;
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (!(target instanceof RootCPListElement))
					return false;
				RootCPListElement root= (RootCPListElement) target;
				return fromModularProject ? root.isClassPathRootNode() : root.isModulePathRootNode();
			}
		});

	}

	// -------- UI creation ---------

	@Override
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		Composite composite= new Composite(parent, SWT.NONE);

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fProjectsList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fProjectsList.getTreeControl(null));

		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fProjectsList.setButtonsMinWidth(buttonBarWidth);

		fSWTControl= composite;

		return composite;
	}

	private void updateClasspathList() {
		List<CPListElement> projelements= fProjectsList.getElements();

		 List<CPListElement> flattenedProjElements = new ArrayList<>();
		 for (CPListElement ele : projelements) {
		 	// if root node, collect the CPList elements
		 	if(ele.isRootNodeForPath()) {
		 		for (Object object : ((RootCPListElement)ele).getChildren()) {
		 			if(object instanceof CPListElement) {
		 				flattenedProjElements.add((CPListElement) object);
		 			}
		 		}
		 	}
		 	else {
		 		flattenedProjElements.add(ele);
		 	}
		 }

		boolean remove= false;
		List<CPListElement> cpelements= fClassPathList.getElements();
		// backwards, as entries will be deleted
		for (int i= cpelements.size() -1; i >= 0 ; i--) {
			CPListElement cpe= cpelements.get(i);
			if (isEntryKind(cpe.getEntryKind())) {
				if (!flattenedProjElements.remove(cpe)) {
					cpelements.remove(i);
					remove= true;
				}
			}
		}
		cpelements.addAll(flattenedProjElements);
		if (remove || (flattenedProjElements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}

	/*
	 * @see BuildPathBasePage#getSelection
	 */
	@Override
	public List<Object> getSelection() {
		return fProjectsList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */
	@Override
	public void setSelection(List<?> selElements, boolean expand) {
		fProjectsList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (Object selElement : selElements) {
				fProjectsList.expandElement(selElement, 1);
			}
		}
	}

	@Override
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_PROJECT;
	}


	private class ProjectsAdapter extends CPListAdapter {

		// -------- IListAdapter --------
		@Override
		public void customButtonPressed(TreeListDialogField<CPListElement> field, int index) {
			projectPageCustomButtonPressed(field, index);
		}

		@Override
		public void selectionChanged(TreeListDialogField<CPListElement> field) {
			projectPageSelectionChanged(field);
		}

		@Override
		public void doubleClicked(TreeListDialogField<CPListElement> field) {
			projectPageDoubleClicked(field);
		}

		@Override
		public void keyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
			projectPageKeyPressed(field, event);
		}


		// ---------- IDialogFieldListener --------

		@Override
		public void dialogFieldChanged(DialogField field) {
			projectPageDialogFieldChanged(field);
		}
	}

	/**
	 * @param field the dialog field
	 * @param index the button index
	 */
	private void projectPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] entries= null;
		switch (index) {
		case IDX_ADDPROJECT: /* add project */
			entries= addProjectDialog();
			break;
		case IDX_EDIT: /* edit */
			editEntry();
			return;
		case IDX_REMOVE: /* remove */
			removeEntry();
			return;
		}
		if (entries != null) {
			int nElementsChosen= entries.length;
			// remove duplicates
			List<CPListElement> cplist= fProjectsList.getElements();
			List<CPListElement> elementsToAdd= new ArrayList<>(nElementsChosen);
			for (int i= 0; i < nElementsChosen; i++) {
				CPListElement curr= entries[i];
				if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
				}
			}

			if(!hasRootNodes()) {
			fProjectsList.addElements(elementsToAdd);
			}
			else {
				// on the new nodes, only additions allowed, rest disabled
				List<Object> selectedElements= fProjectsList.getSelectedElements();
				List<CPListElement> elements= fProjectsList.getElements();
				// if nothing selected, do nothing
				if(selectedElements.isEmpty())
					return;
				boolean isClassRootExpanded= getRootExpansionState(fProjectsList, true);
				boolean isModuleRootExpanded= getRootExpansionState(fProjectsList, false);
				List<CPListElement> checkedElements= new ArrayList<>();
				if (fClassPathList instanceof CheckedListDialogField) {
					CheckedListDialogField<CPListElement> checkedDialogField= (CheckedListDialogField<CPListElement>)fClassPathList;
					for (CPListElement element : checkedDialogField.getElements()) {
						if (checkedDialogField.isChecked(element)) {
							checkedElements.add(element);
						}
					}
				}
				fProjectsList.removeAllElements();
				for (Object selectedElement : selectedElements) {
					if( ((CPListElement)selectedElement).isClassPathRootNode()) {
						for (CPListElement cpListElement : elementsToAdd) {
							cpListElement.setAttribute(IClasspathAttribute.MODULE, null);
						}
						isClassRootExpanded= true;
					}
					if( ((CPListElement)selectedElement).isModulePathRootNode()) {
						for (CPListElement cpListElement : elementsToAdd) {
							Object attribute= cpListElement.getAttribute(IClasspathAttribute.MODULE);
							if(attribute == null) {
								cpListElement.setAttribute(IClasspathAttribute.MODULE, new ModuleEncapsulationDetail[0]);

							}
						}
						isModuleRootExpanded= true;
					}
					((RootCPListElement)selectedElement).addCPListElement(elementsToAdd);
				}
				fProjectsList.setElements(elements);
				fProjectsList.refresh();
				fProjectsList.getTreeViewer().expandToLevel(2);
				setRootExpansionState(fProjectsList, isClassRootExpanded, true);
				setRootExpansionState(fProjectsList, isModuleRootExpanded, false);
				if (fClassPathList instanceof CheckedListDialogField) {
					CheckedListDialogField<CPListElement> checkedDialogField= (CheckedListDialogField<CPListElement>)fClassPathList;
					List<CPListElement> dialogElements= checkedDialogField.getElements();
					for (CPListElement element : checkedElements) {
						if (dialogElements.contains(element)) {
							checkedDialogField.setChecked(element, true);
						}
					}
				}
			}

			if (index == IDX_ADDPROJECT && !hasRootNodes()) {
				fProjectsList.refresh();
			}
			fProjectsList.postSetSelection(new StructuredSelection(entries));
		}
	}

	private void removeEntry() {
		List<Object> selElements= fProjectsList.getSelectedElements();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				if (attrib.isBuiltIn()) {
					String key= attrib.getKey();
					Object value= null;
					if (CPListElement.ACCESSRULES.equals(key)) {
						value= new IAccessRule[0];
					}
					attrib.getParent().setAttribute(key, value);
				} else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
			} else if (elem instanceof ModuleEncapsulationDetail) {
				removeEncapsulationDetail((ModuleEncapsulationDetail) elem);
			}
		}
		if (selElements.isEmpty()) {
			fProjectsList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			if(hasRootNodes()) {
				List<CPListElement> elements= fProjectsList.getElements();
				for (CPListElement cpListElement : elements) {
					((RootCPListElement)cpListElement).getChildren().removeAll(selElements);
				}
				fProjectsList.getTreeViewer().remove(selElements.toArray());
				fProjectsList.dialogFieldChanged();

			}
			else {
				fProjectsList.removeElements(selElements);
			}

		}
	}

	private boolean canRemove(List<?> selElements) {
		if (selElements.isEmpty()) {
			return false;
		}

		for (Object elem : selElements) {
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				if (IClasspathAttribute.MODULE.equals(attrib.getKey())) {
					return false;
				}
				if (attrib.isNonModifiable()) {
					return false;
				}
				if (attrib.isBuiltIn()) {
					if (CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						if (((IAccessRule[]) attrib.getValue()).length == 0) {
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
			} else if (elem instanceof ModuleEncapsulationDetail) {
				return true;
			} else if (elem instanceof CPListElement) {
				if (((CPListElement)elem).isRootNodeForPath()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean canEdit(List<?> selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			return false;
		}
		if (elem instanceof CPListElementAttribute) {
			CPListElementAttribute attrib= (CPListElementAttribute) elem;
			if (attrib.isNonModifiable()) {
				return false;
			}
			if (!attrib.isBuiltIn()) {
				return canEditCustomAttribute(attrib);
			}
			if (hasRootNodes() && IClasspathAttribute.MODULE.equals(attrib.getKey())) {
				// module info should always be enabled.
				return true;
			}
			return true;
		}
		return false;
	}

	/**
	 * Method editEntry.
	 */
	private void editEntry() {
		List<Object> selElements= fProjectsList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}

	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		boolean needRefresh= false;
		boolean wasModular= false;
		if (CPListElement.ACCESSRULES.equals(key)) {
			showAccessRestrictionDialog(elem.getParent());
		} else if (CPListElement.MODULE.equals(key)) {
			wasModular= elem.getValue() != null;
			needRefresh= showModuleDialog(getShell(), elem);
		} else {
			needRefresh= editCustomAttribute(getShell(), elem);
		}
		if (needRefresh) {
			if (CPListElement.MODULE.equals(key) && hasRootNodes()) {
				// if module attribute is changed, the element may change nodes
				CPListElement selElement= elem.getParent();
				boolean isModular= selElement.getAttribute(CPListElement.MODULE) != null;
				RootNodeChange changeDirection= RootNodeChange.fromOldAndNew(wasModular, isModular);
				if (changeDirection != RootNodeChange.NoChange) {
					moveCPElementAcrossNode(fProjectsList, selElement, changeDirection);
				}
			}
			if(CPListElement.TEST.equals(key) || CPListElement.WITHOUT_TEST_CODE.equals(key)) {
				fProjectsList.refresh(elem.getParent());
			} else {
				fProjectsList.refresh(elem);
			}
			fClassPathList.dialogFieldChanged(); // validate
			fProjectsList.postSetSelection(new StructuredSelection(elem));
			// if module attribute was changed - it will switch nodes and hence parent should be
			// selected
			if (CPListElement.MODULE.equals(key) && hasRootNodes()) {
				fProjectsList.postSetSelection(new StructuredSelection(elem.getParent()));
			} else {
				fProjectsList.postSetSelection(new StructuredSelection(elem));
			}

		}
	}

	private void showAccessRestrictionDialog(CPListElement selElement) {
		AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, fCurrJProject, fPageContainer != null);
		int res= dialog.open();
		if (res == Window.OK || res == AccessRulesDialog.SWITCH_PAGE) {
			selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
			selElement.setAttribute(CPListElement.COMBINE_ACCESSRULES, Boolean.valueOf(dialog.doCombineAccessRules()));
			fProjectsList.refresh();
			fClassPathList.dialogFieldChanged(); // validate

			if (res == AccessRulesDialog.SWITCH_PAGE) {
				dialog.performPageSwitch(fPageContainer);
			}
		}
	}

	private CPListElement[] addProjectDialog() {

		try {
			Object[] selectArr= getNotYetRequiredProjects();
			new JavaElementComparator().sort(null, selectArr);

			ListSelectionDialog dialog= new ListSelectionDialog(getShell(), Arrays.asList(selectArr), ArrayContentProvider.getInstance(), new JavaUILabelProvider(), NewWizardMessages.ProjectsWorkbookPage_chooseProjects_message);
			dialog.setTitle(NewWizardMessages.ProjectsWorkbookPage_chooseProjects_title);
			dialog.setHelpAvailable(false);
			if (dialog.open() == Window.OK) {
				Object[] result= dialog.getResult();
				CPListElement[] cpElements= new CPListElement[result.length];
				for (int i= 0; i < result.length; i++) {
					IJavaProject curr= (IJavaProject) result[i];
					CPListElement cpListElement= new CPListElement(fCurrJProject, IClasspathEntry.CPE_PROJECT, curr.getPath(), curr.getResource());
					cpListElement.setModuleAttributeIf9OrHigher(fCurrJProject);
					cpElements[i]= cpListElement;
				}
				return cpElements;
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	private Object[] getNotYetRequiredProjects() throws JavaModelException {
		ArrayList<IJavaProject> selectable= new ArrayList<>(Arrays.asList(fCurrJProject.getJavaModel().getJavaProjects()));
		selectable.remove(fCurrJProject);

		for (CPListElement curr : fProjectsList.getElements()) {
			if (curr.isRootNodeForPath()) {
				for (Object object : ((RootCPListElement)curr).getChildren()) {
					if(object instanceof CPListElement) {
						CPListElement cpe = (CPListElement)object;
						IJavaProject proj= (IJavaProject) JavaCore.create(cpe.getResource());
						selectable.remove(proj);
					}
				}
			}
			IJavaProject proj= (IJavaProject) JavaCore.create(curr.getResource());
			selectable.remove(proj);
		}
		Object[] selectArr= selectable.toArray();
		return selectArr;
	}

	/**
	 * @param field the dialog field
	 */
	protected void projectPageDoubleClicked(TreeListDialogField<CPListElement> field) {
		List<Object> selection= fProjectsList.getSelectedElements();
		if (canEdit(selection)) {
			editEntry();
		}
	}

	protected void projectPageKeyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
		if (field == fProjectsList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List<Object> selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}
	}

	/**
	 * @param field the dialog field
	 */
	private void projectPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}

	/**
	 * @param field the dialog field
	 */
	private void projectPageSelectionChanged(DialogField field) {
		List<Object> selElements= fProjectsList.getSelectedElements();

		String text;
		if (selElements.size() == 1
				&& selElements.get(0) instanceof CPListElementAttribute) {
			String key= ((CPListElementAttribute) selElements.get(0)).getKey();
			if (CPListElement.TEST.equals(key) || CPListElement.WITHOUT_TEST_CODE.equals(key)) {
				text= NewWizardMessages.ProjectsWorkbookPage_projects_toggle_button;
			} else {
				text= NewWizardMessages.ProjectsWorkbookPage_projects_edit_button;
			}
		} else {
			text= NewWizardMessages.ProjectsWorkbookPage_projects_edit_button;
		}
		fProjectsList.getButton(IDX_EDIT).setText(text);

		fProjectsList.enableButton(IDX_EDIT, canEdit(selElements));
		fProjectsList.enableButton(IDX_REMOVE, canRemove(selElements));

		boolean enabled;
		try {
			enabled= getNotYetRequiredProjects().length > 0;
		} catch (JavaModelException ex) {
			enabled= false;
		}
		fProjectsList.enableButton(IDX_ADDPROJECT, enabled);
		if(hasRootNodes()) {
			fProjectsList.enableButton(IDX_ADDPROJECT, enabled && canAdd(selElements));
		}
	}

    private boolean canAdd(List<Object> selElements) {
		if (selElements.size() != 1) {
			return false;
		}
		Object elem= selElements.get(0);
		if (elem instanceof CPListElement) {
			return ((CPListElement)elem).isRootNodeForPath();
		}
		if(elem instanceof CPListElementAttribute) {
			return false;
		}
		return true;
	}

	@Override
	public void setFocus() {
    	fProjectsList.setFocus();
    }

	public void selectRootNode(boolean modulePath) {
		selectRootNode(fProjectsList, modulePath);
	}
}
