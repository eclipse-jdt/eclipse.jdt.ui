/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for Bug 465293 - External annotation path per container and project
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.JarImportWizardAction;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.jarimport.JarImportWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.RootCPListElement.RootNodeChange;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;


public class LibrariesWorkbookPage extends BuildPathBasePage {

	private final ListDialogField<CPListElement> fClassPathList;
	private IJavaProject fCurrJProject;

	private final TreeListDialogField<CPListElement> fLibrariesList;

	private final IWorkbenchPreferenceContainer fPageContainer;

	private final int IDX_ADDJAR= 0;
	private final int IDX_ADDEXT= 1;
	private final int IDX_ADDVAR= 2;
	private final int IDX_ADDLIB= 3;
	private final int IDX_ADDFOL= 4;
	private final int IDX_ADDEXTFOL= 5;

	private final int IDX_EDIT= 7;
	private final int IDX_REMOVE= 8;

	private final int IDX_REPLACE= 10;

	private boolean dragDropEnabled;
	private Object draggedItemsLibrary;
	private boolean fromModularLibrary;

	public LibrariesWorkbookPage(CheckedListDialogField<CPListElement> classPathList, IWorkbenchPreferenceContainer pageContainer) {
		fClassPathList= classPathList;
		fPageContainer= pageContainer;
		fSWTControl= null;

		String[] buttonLabels= new String[] {
			NewWizardMessages.LibrariesWorkbookPage_libraries_addjar_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addextjar_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addvariable_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addlibrary_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addclassfolder_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_addextfolder_button,
			/* */ null,
			NewWizardMessages.LibrariesWorkbookPage_libraries_edit_button,
			NewWizardMessages.LibrariesWorkbookPage_libraries_remove_button,
			/* */ null,
			NewWizardMessages.LibrariesWorkbookPage_libraries_replace_button
		};

		LibrariesAdapter adapter= new LibrariesAdapter();

		fLibrariesList= new TreeListDialogField<>(adapter, buttonLabels, new CPListLabelProvider());
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(NewWizardMessages.LibrariesWorkbookPage_libraries_label);

		fLibrariesList.enableButton(IDX_REMOVE, false);
		fLibrariesList.enableButton(IDX_EDIT, false);
		fLibrariesList.enableButton(IDX_REPLACE, false);

		fLibrariesList.setViewerComparator(new CPListElementSorter());

	}

	@Override
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			updateLibrariesList();
		} else {
			Display.getDefault().asyncExec(this::updateLibrariesList);
		}
	}

	private void updateLibrariesList() {
		if(JavaModelUtil.is9OrHigher(fCurrJProject)) {
			updateLibrariesListWithRootNode();
			return;
		}
		List<CPListElement> cpelements= fClassPathList.getElements();
		List<CPListElement> libelements= new ArrayList<>(cpelements.size());

		for (CPListElement cpe : cpelements) {
			if (isEntryKind(cpe.getEntryKind())) {
				libelements.add(cpe);
			}
		}
		fLibrariesList.setElements(libelements);
	}
	private void updateLibrariesListWithRootNode() {
		RootCPListElement rootClasspath= new RootCPListElement(fCurrJProject, NewWizardMessages.PathRootWorkbookPage_classpath,false);
		RootCPListElement rootModulepath= new RootCPListElement(fCurrJProject,NewWizardMessages.PathRootWorkbookPage_modulepath,true);

		List<CPListElement> cpelements= fClassPathList.getElements();
		List<CPListElement> libelements= new ArrayList<>(cpelements.size());

		int size= fLibrariesList.getElements().size();
		for (CPListElement cpe : cpelements) {
			if (isEntryKind(cpe.getEntryKind())) {
				if (size > 0) {
					// only for update
					cpe= checkAndUpdateIfModularJRE(cpe);
				}
				if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					if (cpe.getAttribute(CPListElement.MODULE) == null && !isJREContainer(cpe.getPath())) {
						cpe.updateExtraAttributeOfClasspathEntry();
						cpe.setAttribute(CPListElement.MODULE, null);
					}
				}
				Object mod= cpe.getAttribute(CPListElement.MODULE);
				if(mod == null) {
					rootClasspath.addCPListElement(cpe);
				} else {
					rootModulepath.addCPListElement(cpe);
				}

			}
		}
		libelements.add(rootModulepath);
		libelements.add(rootClasspath);
		fLibrariesList.setTreeExpansionLevel(2);
		fLibrariesList.setElements(libelements);

		fLibrariesList.enableButton(IDX_ADDEXT, false);
		fLibrariesList.enableButton(IDX_ADDFOL, false);
		fLibrariesList.enableButton(IDX_ADDEXTFOL, false);
		fLibrariesList.enableButton(IDX_ADDJAR, false);
		fLibrariesList.enableButton(IDX_ADDLIB, false);
		fLibrariesList.enableButton(IDX_ADDVAR, false);

		if (!dragDropEnabled) {
			enableDragDropSupport();
		}

	}

	private CPListElement checkAndUpdateIfModularJRE(CPListElement cpe) {
		boolean modularJava= false;
		IVMInstall vmInstall= JavaRuntime.getVMInstall(cpe.getPath());
		if (vmInstall != null) {
			modularJava= JavaRuntime.isModularJava(vmInstall);
		}
		if (modularJava) {
			// If JRE is updated to modular JRE, then cpe element has to be recreated
			// so as to have the modular structure
			cpe= CPListElement.create(cpe.getClasspathEntry(), true, fCurrJProject);
		}
		return cpe;
	}

	private void enableDragDropSupport() {
		dragDropEnabled= true;
		int ops= DND.DROP_MOVE;
		Transfer[] transfers= new Transfer[] { ResourceTransfer.getInstance(), FileTransfer.getInstance() };
		fLibrariesList.getTreeViewer().addDragSupport(ops, transfers, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection ssel= (IStructuredSelection) fLibrariesList.getTreeViewer().getSelection();
				if (ssel == null || ssel.isEmpty()) {
					event.doit= false;
				}
				if (ssel != null) {
					Object[] ele= ssel.toArray();
					for (Object element : ele) {
						// dont start drag on root nodes
						if (element instanceof RootCPListElement) {
							event.doit= false;
							break;
						}
						if (element instanceof CPListElement) {
							CPListElement cpe= (CPListElement) element;
							List<CPListElement> elements= fLibrariesList.getElements();
							for (Object cpListElement : elements) {
								if (cpListElement instanceof RootCPListElement) {
									RootCPListElement root= (RootCPListElement) cpListElement;
									if (root.getChildren().contains(cpe)) {
										fromModularLibrary= root.isModulePathRootNode();
										break;
									}
								}
							}
							if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
								IPath path= cpe.getPath();
								if (path != null) {
									IVMInstall vmInstall= JavaRuntime.getVMInstall(path);
									if (vmInstall != null) {
										boolean isJRE= isJREContainer(path);
										if (isJRE == true) {
											event.doit= false;
											break;
										}
									}
								}
							}
						}
					}
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection ssel= (IStructuredSelection) fLibrariesList.getTreeViewer().getSelection();
				event.data= ssel.toArray();
				draggedItemsLibrary= ssel.toArray();
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				draggedItemsLibrary= null;
			}
		});

		fLibrariesList.getTreeViewer().addDropSupport(ops, transfers, new ViewerDropAdapter(fLibrariesList.getTreeViewer()) {
			@Override
			public boolean performDrop(Object data) {
				Object[] objects= (data == null) ? (Object[]) draggedItemsLibrary : (Object[]) data;
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
							// pre-process container items while moving to modulepath
							CPListElement cpe= (CPListElement) object;
							if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER && isModular) {
								IClasspathEntry entry= cpe.getClasspathEntry();
								IClasspathAttribute[] extraAttributes= entry.getExtraAttributes();
								boolean hasModAttr= false;
								for (IClasspathAttribute attr : extraAttributes) {
									if (IClasspathAttribute.MODULE.equals(attr.getName())) {
										hasModAttr= true;
										break;
									}
								}
								if (!hasModAttr) {
									cpe.updateExtraAttributeOfClasspathEntry();
								}
							}

							moveCPElementAcrossNode(fLibrariesList, (CPListElement) object, direction);
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
				return fromModularLibrary ? root.isClassPathRootNode() : root.isModulePathRootNode();
			}

		});


	}

	static boolean isJREContainer(IPath path) {
		if (path == null)
			return false;
		String[] segments= path.segments();
		return Arrays.asList(segments).contains(JavaRuntime.JRE_CONTAINER);
	}

	boolean hasRootNodes(){
		if (fLibrariesList == null)
			return false;
		if(fLibrariesList.getSize()==0)
			return false;
		if(fLibrariesList.getElement(0).isRootNodeForPath())
			return true;
		return false;
	}

	// -------- UI creation

	@Override
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		Composite composite= new Composite(parent, SWT.NONE);

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibrariesList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fLibrariesList.getTreeControl(null));

		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fLibrariesList.setButtonsMinWidth(buttonBarWidth);

		fLibrariesList.setViewerComparator(new CPListElementSorter());

		fSWTControl= composite;

		return composite;
	}

	private class LibrariesAdapter extends CPListAdapter {

		// -------- IListAdapter --------
		@Override
		public void customButtonPressed(TreeListDialogField<CPListElement> field, int index) {
			libaryPageCustomButtonPressed(field, index);
		}

		@Override
		public void selectionChanged(TreeListDialogField<CPListElement> field) {
			libaryPageSelectionChanged(field);
		}

		@Override
		public void doubleClicked(TreeListDialogField<CPListElement> field) {
			libaryPageDoubleClicked(field);
		}

		@Override
		public void keyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
			libaryPageKeyPressed(field, event);
		}

		@Override
		public Object[] getChildren(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute) element;
				if (CPListElement.ACCESSRULES.equals(attribute.getKey())) {
					return (IAccessRule[]) attribute.getValue();
				}
			}
			return super.getChildren(field, element);
		}

		// ---------- IDialogFieldListener --------

		@Override
		public void dialogFieldChanged(DialogField field) {
			libaryPageDialogFieldChanged(field);
		}
	}

	/**
	 * A button has been pressed.
	 *
	 * @param field the dialog field containing the button
	 * @param index the index of the button
	 */
	private void libaryPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] libentries= null;
		switch (index) {
		case IDX_ADDJAR: /* add jar */
			libentries= openJarFileDialog(null);
			break;
		case IDX_ADDEXT: /* add external jar */
			libentries= openExtJarFileDialog(null);
			break;
		case IDX_ADDVAR: /* add variable */
			libentries= openVariableSelectionDialog(null);
			break;
		case IDX_ADDLIB: /* add library */
			libentries= openContainerSelectionDialog(null);
			break;
		case IDX_ADDFOL: /* add folder */
			libentries= openClassFolderDialog(null);
			break;
		case IDX_ADDEXTFOL: /* add external folder */
			libentries= openExternalClassFolderDialog(null);
			break;
		case IDX_EDIT: /* edit */
			editEntry();
			return;
		case IDX_REMOVE: /* remove */
			removeEntry();
			return;
		case IDX_REPLACE: /* replace */
			replaceJarFile();
			return;
		}
		if (libentries != null) {
			int nElementsChosen= libentries.length;
			// remove duplicates
			List<CPListElement> cplist= fLibrariesList.getElements();
			List<CPListElement> elementsToAdd= new ArrayList<>(nElementsChosen);

			for (CPListElement curr : libentries) {
				boolean contains= cplist.contains(curr);
				if(hasRootNodes()) {
					contains= hasCurrentElement(cplist,curr);
				}
				if (!contains && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
					curr.setAttribute(CPListElement.SOURCEATTACHMENT, BuildPathSupport.guessSourceAttachment(curr));
					curr.setAttribute(CPListElement.JAVADOC, BuildPathSupport.guessJavadocLocation(curr));
				}
			}
			if (!elementsToAdd.isEmpty() && (index == IDX_ADDFOL)) {
				askForAddingExclusionPatternsDialog(elementsToAdd);
			}

			if(!hasRootNodes()) {
				fLibrariesList.addElements(elementsToAdd);
			} else {
				// on root nodes, only additions allowed, rest disabled
				List<Object> selectedElements= fLibrariesList.getSelectedElements();
				List<CPListElement> elements= fLibrariesList.getElements();
				// sanity check, button should only be enabled if exactly one root node is selected
				if(selectedElements.size() != 1) {
					return;
				}
				boolean isClassRootExpanded= getRootExpansionState(fLibrariesList, true);
				boolean isModuleRootExpanded= getRootExpansionState(fLibrariesList, false);
				fLibrariesList.removeAllElements();
				RootCPListElement selectedCPElement= (RootCPListElement) selectedElements.get(0);
				if(selectedCPElement.isClassPathRootNode()) {
					for (CPListElement cpListElement : elementsToAdd) {
						cpListElement.setAttribute(IClasspathAttribute.MODULE, null);
					}
					isClassRootExpanded= true;
				} else if (selectedCPElement.isModulePathRootNode()) {
					for (CPListElement cpListElement : elementsToAdd) {
						Object attribute= cpListElement.getAttribute(IClasspathAttribute.MODULE);
						if (attribute == null) {
							cpListElement.setAttribute(IClasspathAttribute.MODULE, new ModuleEncapsulationDetail[0]);
						}
					}
					isModuleRootExpanded= true;
				}
				selectedCPElement.addCPListElement(elementsToAdd);

				fLibrariesList.setElements(elements);
				fLibrariesList.refresh();
				fLibrariesList.getTreeViewer().expandToLevel(2);
				setRootExpansionState(fLibrariesList, isClassRootExpanded, true);
				setRootExpansionState(fLibrariesList, isModuleRootExpanded, false);
			}

			if (index == IDX_ADDLIB || index == IDX_ADDVAR) {
				fLibrariesList.refresh();
			}
			fLibrariesList.postSetSelection(new StructuredSelection(libentries));
		}
	}




	private boolean hasCurrentElement(List<CPListElement> cplist, CPListElement curr) {
		//note that the same cpelement with different attribute can be added
		for (CPListElement cpListElement : cplist) {
			if(cpListElement.isRootNodeForPath()) {
				boolean cont= ( (RootCPListElement)cpListElement).getChildren().contains(curr);
				if(cont == true) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void addElement(CPListElement element) {
		fLibrariesList.addElement(element);
		fLibrariesList.postSetSelection(new StructuredSelection(element));
	}

	private void askForAddingExclusionPatternsDialog(List<CPListElement> newEntries) {
		HashSet<CPListElement> modified= new HashSet<>();
		List<CPListElement> existing= fClassPathList.getElements();
		fixNestingConflicts(newEntries.toArray(new CPListElement[newEntries.size()]), existing.toArray(new CPListElement[existing.size()]), modified);
		if (!modified.isEmpty()) {
			String title= NewWizardMessages.LibrariesWorkbookPage_exclusion_added_title;
			String message= NewWizardMessages.LibrariesWorkbookPage_exclusion_added_message;
			MessageDialog.openInformation(getShell(), title, message);
		}
	}

	protected void libaryPageDoubleClicked(TreeListDialogField<CPListElement> field) {
		List<?> selection= field.getSelectedElements();
		if (canEdit(selection)) {
			editEntry();
		}
	}

	protected void libaryPageKeyPressed(TreeListDialogField<CPListElement> field, KeyEvent event) {
		if (field == fLibrariesList) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				List<?> selection= field.getSelectedElements();
				if (canRemove(selection)) {
					removeEntry();
				}
			}
		}
	}

	private void replaceJarFile() {
		final IPackageFragmentRoot root= getSelectedPackageFragmentRoot();
		if (root != null) {
			final IImportWizard wizard= new JarImportWizard(false);
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(root));
			final WizardDialog dialog= new WizardDialog(getShell(), wizard);
			dialog.create();
			dialog.getShell().setSize(Math.max(JarImportWizardAction.SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), JarImportWizardAction.SIZING_WIZARD_HEIGHT);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IJavaHelpContextIds.JARIMPORT_WIZARD_PAGE);
			dialog.open();
		}
	}

	private IPackageFragmentRoot getSelectedPackageFragmentRoot() {
		final List<Object> elements= fLibrariesList.getSelectedElements();
		if (elements.size() == 1) {
			final Object object= elements.get(0);
			if (object instanceof CPListElement) {
				final CPListElement element= (CPListElement) object;
				final IClasspathEntry entry= element.getClasspathEntry();
				if (JarImportWizard.isValidClassPathEntry(entry)) {
					final IJavaProject project= element.getJavaProject();
					if (project != null) {
						try {
							for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
								if (entry.equals(root.getRawClasspathEntry())) {
									return root;
								}
							}
						} catch (JavaModelException exception) {
							JavaPlugin.log(exception);
						}
					}
				}
			}
		}
		return null;
	}

	private void removeEntry() {
		List<Object> selElements= fLibrariesList.getSelectedElements();
		HashMap<CPListElement, HashSet<String>> containerEntriesToUpdate= new HashMap<>();
		for (int i= selElements.size() - 1; i >= 0 ; i--) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) elem;
				String key= attrib.getKey();
				if (attrib.isBuiltIn()) {
					Object value= null;
					if (CPListElement.ACCESSRULES.equals(key)) {
						value= new IAccessRule[0];
					}
					attrib.setValue(value);
				} else {
					removeCustomAttribute(attrib);
				}
				selElements.remove(i);
				if (attrib.getParent().getParentContainer() instanceof CPListElement) { // inside a container: apply changes right away
					CPListElement containerEntry= attrib.getParent();
					HashSet<String> changedAttributes= containerEntriesToUpdate.get(containerEntry);
					if (changedAttributes == null) {
						changedAttributes= new HashSet<>();
						containerEntriesToUpdate.put(containerEntry, changedAttributes);
					}
					changedAttributes.add(key); // collect the changed attributes
				}
			} else if (elem instanceof ModuleEncapsulationDetail) {
				removeEncapsulationDetail((ModuleEncapsulationDetail) elem);
			}
		}
		if (selElements.isEmpty()) {
			fLibrariesList.refresh();
			fClassPathList.dialogFieldChanged(); // validate
		} else {
			if(hasRootNodes()) {
				List<CPListElement> elements= fLibrariesList.getElements();
				for (CPListElement cpListElement : elements) {
					((RootCPListElement)cpListElement).getChildren().removeAll(selElements);
				}
				fLibrariesList.getTreeViewer().remove(selElements.toArray());
				fLibrariesList.dialogFieldChanged();

			}
			else {
				fLibrariesList.removeElements(selElements);
			}

		}
		for (Map.Entry<CPListElement, HashSet<String>> entry : containerEntriesToUpdate.entrySet()) {
			CPListElement curr= entry.getKey();
			HashSet<String> attribs= entry.getValue();
			String[] changedAttributes= attribs.toArray(new String[attribs.size()]);
			IClasspathEntry changedEntry= curr.getClasspathEntry();
			updateContainerEntry(changedEntry, changedAttributes, fCurrJProject, ((CPListElement) curr.getParentContainer()).getPath());
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
					if (attrib.getParent().isInContainer(JavaRuntime.JRE_CONTAINER) && CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						return false; // workaround for 166519 until we have full story
					}
					if (CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						return ((IAccessRule[]) attrib.getValue()).length > 0;
					}
					if (attrib.getValue() == null) {
						return false;
					}
				} else {
					if (!canRemoveCustomAttribute(attrib)) {
						return false;
					}
				}
			} else if (elem instanceof CPListElement) {
				CPListElement curr= (CPListElement) elem;
				if(curr.isRootNodeForPath()) {
					return false;
				}
				if (curr.getParentContainer() != null) {
					return false;
				}
			} else if (elem instanceof ModuleEncapsulationDetail) {
				return true;
			} else { // unknown element
				return false;
			}
		}
		return true;
	}

	/**
	 * Method editEntry.
	 */
	private void editEntry() {
		List<?> selElements= fLibrariesList.getSelectedElements();
		if (selElements.size() != 1) {
			return;
		}
		Object elem= selElements.get(0);

		boolean canEdit= false;
		if(hasRootNodes()) {
			canEdit= ((RootCPListElement)fLibrariesList.getElement(0)).getChildren().indexOf(elem) != -1 ||
					((RootCPListElement)fLibrariesList.getElement(1)).getChildren().indexOf(elem) != -1 ;

		}
		if (canEdit || fLibrariesList.getIndexOfElement(elem) != -1 ) {
			editElementEntry((CPListElement) elem);
		} else if (elem instanceof CPListElementAttribute) {
			editAttributeEntry((CPListElementAttribute) elem);
		}
	}

	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		CPListElement selElement= elem.getParent();

		boolean canEditEncoding= false;
		for (CPListElementAttribute attribute : selElement.getAllAttributes()) {
			if (CPListElement.SOURCE_ATTACHMENT_ENCODING.equals(attribute.getKey())) {
				canEditEncoding= !attribute.isNonModifiable() && !attribute.isNotSupported();
			}
		}
		if (CPListElement.SOURCEATTACHMENT.equals(key)) {
			IClasspathEntry result= BuildPathDialogAccess.configureSourceAttachment(getShell(), selElement.getClasspathEntry(), canEditEncoding);
			if (result != null) {
				selElement.setAttribute(CPListElement.SOURCEATTACHMENT, result.getSourceAttachmentPath());
				selElement.setAttribute(CPListElement.SOURCE_ATTACHMENT_ENCODING, SourceAttachmentBlock.getSourceAttachmentEncoding(result));
				String[] changedAttributes= { CPListElement.SOURCEATTACHMENT, CPListElement.SOURCE_ATTACHMENT_ENCODING };
				attributeUpdated(selElement, changedAttributes);
				fLibrariesList.refresh(elem);
				fLibrariesList.update(selElement); // image
				fClassPathList.refresh(); // images
				updateEnabledState();
			}
		} else if (CPListElement.ACCESSRULES.equals(key)) {
			AccessRulesDialog dialog= new AccessRulesDialog(getShell(), selElement, fCurrJProject, fPageContainer != null);
			int res= dialog.open();
			if (res == Window.OK || res == AccessRulesDialog.SWITCH_PAGE) {
				selElement.setAttribute(CPListElement.ACCESSRULES, dialog.getAccessRules());
				String[] changedAttributes= { CPListElement.ACCESSRULES };
				attributeUpdated(selElement, changedAttributes);

				fLibrariesList.refresh(elem);
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();

				if (res == AccessRulesDialog.SWITCH_PAGE) { // switch after updates and validation
					dialog.performPageSwitch(fPageContainer);
				}
			}
		} else if (CPListElement.MODULE.equals(key)) {
			boolean wasModular= selElement.getAttribute(CPListElement.MODULE) != null;
			if (showModuleDialog(getShell(), elem)) {
				String[] changedAttributes= { CPListElement.MODULE };
				attributeUpdated(selElement, changedAttributes);
				if (hasRootNodes()) {
					boolean isModular= selElement.getAttribute(CPListElement.MODULE) != null;
					RootNodeChange direction= RootNodeChange.fromOldAndNew(wasModular, isModular);
					if (direction != RootNodeChange.NoChange) {
						moveCPElementAcrossNode(fLibrariesList, selElement, direction);
					}
				}
				fLibrariesList.refresh(elem);
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();
			}
		} else {
			if (editCustomAttribute(getShell(), elem)) {
				String[] changedAttributes= { key };
				attributeUpdated(selElement, changedAttributes);
				if(CPListElement.TEST.equals(key) || CPListElement.WITHOUT_TEST_CODE.equals(key)) {
					fLibrariesList.refresh(elem.getParent());
				} else {
					fLibrariesList.refresh(elem);
				}
				fClassPathList.dialogFieldChanged(); // validate
				updateEnabledState();
				checkAttributeEffect(key, fCurrJProject);
			}
		}
	}

	private void attributeUpdated(CPListElement selElement, String[] changedAttributes) {
		Object parentContainer= selElement.getParentContainer();
		if (parentContainer instanceof CPListElement) { // inside a container: apply changes right away
			IClasspathEntry updatedEntry= selElement.getClasspathEntry();
			updateContainerEntry(updatedEntry, changedAttributes, fCurrJProject, ((CPListElement) parentContainer).getPath());
		}
	}

	private void updateContainerEntry(final IClasspathEntry newEntry, final String[] changedAttributes, final IJavaProject jproject, final IPath containerPath) {
		try {
			IWorkspaceRunnable runnable= monitor -> BuildPathSupport.modifyClasspathEntry(null, newEntry, changedAttributes, jproject, containerPath, false, monitor);
			PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));

		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.LibrariesWorkbookPage_configurecontainer_error_title;
			String message= NewWizardMessages.LibrariesWorkbookPage_configurecontainer_error_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InterruptedException e) {
			//
		}
	}

	private void editElementEntry(CPListElement elem) {
		CPListElement[] res= null;

		switch (elem.getEntryKind()) {
		case IClasspathEntry.CPE_CONTAINER:
			res= openContainerSelectionDialog(elem);
			break;
		case IClasspathEntry.CPE_LIBRARY:
			IResource resource= elem.getResource();
			if (resource == null) {
				File file= elem.getPath().toFile();
				if (file.isDirectory()) {
					res= openExternalClassFolderDialog(elem);
				} else {
					res= openExtJarFileDialog(elem);
				}
			} else if (resource.getType() == IResource.FOLDER) {
				if (resource.exists()) {
					res= openClassFolderDialog(elem);
				} else {
					res= openNewClassFolderDialog(elem);
				}
			} else if (resource.getType() == IResource.FILE) {
				res= openJarFileDialog(elem);
			}
			break;
		case IClasspathEntry.CPE_VARIABLE:
			res= openVariableSelectionDialog(elem);
			break;
		}
		if (res != null && res.length > 0) {
			CPListElement curr= res[0];
			Object attrib= curr.getAttribute(CPListElement.MODULE);
			curr.setExported(elem.isExported());
			curr.setAttributesFromExisting(elem);
			// the module attribute may be changed in curr with respect to elem
			if (attrib != null) {
				curr.setAttribute(IClasspathAttribute.MODULE, attrib);
			}
			if (hasRootNodes()) {
				for (int i= 0; i < fLibrariesList.getElements().size(); i++) {
					CPListElement cpe= fLibrariesList.getElement(i);
					if (cpe.isRootNodeForPath()) {
						if ((((RootCPListElement)cpe).getChildren().contains(elem))) {
							for (int j= 0; j < ((RootCPListElement)cpe).getChildren().size(); j++) {
								// find index
								Object obj= ((RootCPListElement) cpe).getChildren().get(j);
								if (obj.equals(elem)) {
									((RootCPListElement) cpe).getChildren().set(j, curr);
									RootNodeChange changeNodeDirection= doesElementNeedNodeChange(elem, curr);
									if (changeNodeDirection != RootNodeChange.NoChange) {
										moveCPElementAcrossNode(fLibrariesList, curr, changeNodeDirection);
										CPListElementAttribute moduleAttr= curr.findAttributeElement(CPListElement.MODULE);
										Object value=  (changeNodeDirection == RootNodeChange.ToModulepath) ? new ModuleEncapsulationDetail[0] : null;
										if (moduleAttr != null) {
											moduleAttr.setValue(value);
										} else {
											curr.setAttribute(CPListElement.MODULE, value);
										}
									}
									fLibrariesList.dialogFieldChanged();
									fLibrariesList.refresh();
									break;
								}
							}
						}
					}
				}
			} else {
				fLibrariesList.replaceElement(elem, curr);
			}
			if (elem.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				fLibrariesList.refresh();
			}
		}

	}

	/**
	 * @param elem is former cpelement
	 * @param curr is new cpelement
	 * @return 0 is no change is required, -1 if modular element is removed, +1 if non-modular element
	 *         is removed.
	 */
	private RootNodeChange doesElementNeedNodeChange(CPListElement elem, CPListElement curr) {
		if (elem.getClasspathEntry().getEntryKind() != IClasspathEntry.CPE_CONTAINER)
			return RootNodeChange.NoChange;
		if (curr.getClasspathEntry().getEntryKind() != IClasspathEntry.CPE_CONTAINER)
			return RootNodeChange.NoChange;
		String v1= null;
		String v2= null;
		IVMInstall vm1= JavaRuntime.getVMInstall(elem.getPath());
		if (vm1 instanceof AbstractVMInstall) {
			v1= ((AbstractVMInstall) vm1).getJavaVersion();
		}
		IVMInstall vm2= JavaRuntime.getVMInstall(curr.getPath());
		if (vm2 instanceof AbstractVMInstall) {
			v2= ((AbstractVMInstall) vm2).getJavaVersion();
		}
		if (v1 != null && v2 != null) {
			boolean mod1= JavaModelUtil.is9OrHigher(v1);
			boolean mod2= JavaModelUtil.is9OrHigher(v2);
			if (mod1 == true && mod2 == true)
				return RootNodeChange.NoChange;
			if (mod1 == false && mod2 == false)
				return RootNodeChange.NoChange;
			if (mod1 == true && mod2 == false) {
				// removed module
				return RootNodeChange.ToClasspath;
			}
			if (mod1 == false && mod2 == true) {
				//added module
				return RootNodeChange.ToModulepath;
			}
		}
		return RootNodeChange.NoChange;
	}

	/**
	 * @param field  the dilaog field
	 */
	private void libaryPageSelectionChanged(DialogField field) {
		List<Object> selected= fLibrariesList.getSelectedElements();
		String text;
		if (selected.size() == 1
				&& selected.get(0) instanceof CPListElementAttribute) {
			String key= ((CPListElementAttribute) selected.get(0)).getKey();
			if (CPListElement.TEST.equals(key) || CPListElement.WITHOUT_TEST_CODE.equals(key)) {
				text= NewWizardMessages.LibrariesWorkbookPage_libraries_toggle_button;
			} else {
				text= NewWizardMessages.LibrariesWorkbookPage_libraries_edit_button;
			}
		} else {
			text= NewWizardMessages.LibrariesWorkbookPage_libraries_edit_button;
		}
		fLibrariesList.getButton(IDX_EDIT).setText(text);
		updateEnabledState();
	}

	private void updateEnabledState() {
		List<?> selElements= fLibrariesList.getSelectedElements();
		fLibrariesList.enableButton(IDX_EDIT, canEdit(selElements));
		fLibrariesList.enableButton(IDX_REMOVE, canRemove(selElements));
		fLibrariesList.enableButton(IDX_REPLACE, getSelectedPackageFragmentRoot() != null);

		boolean addEnabled= containsOnlyTopLevelEntries(selElements);

		fLibrariesList.enableButton(IDX_ADDEXT, addEnabled);
		fLibrariesList.enableButton(IDX_ADDFOL, addEnabled);
		fLibrariesList.enableButton(IDX_ADDEXTFOL, addEnabled);
		fLibrariesList.enableButton(IDX_ADDJAR, addEnabled);
		fLibrariesList.enableButton(IDX_ADDLIB, addEnabled);
		fLibrariesList.enableButton(IDX_ADDVAR, addEnabled);
	}
	@Override
	protected boolean containsOnlyTopLevelEntries(List<?> selElements) {
		if(hasRootNodes()==false) {
			return super.containsOnlyTopLevelEntries(selElements);
		}
		if (selElements.isEmpty() || selElements.size() > 1) {
			return false;
		}
		for (Object elem : selElements) {
			if (elem instanceof CPListElement) {
				if (!((CPListElement) elem).isRootNodeForPath()) {
					return false;
				}
			} else {
				return false;
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
			CPListElement curr= (CPListElement) elem;
			if(((CPListElement) elem).isRootNodeForPath()) {
				return false;
			}
			return !(curr.getResource() instanceof IFolder) && curr.getParentContainer() == null;
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
				//module attribute should always be enabled
				return true;
			}

			return true;
		}
		return false;
	}

	/**
	 * @param field the dialog field
	 */
	private void libaryPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}

	private void updateClasspathList() {
		 List<CPListElement> projelements= fLibrariesList.getElements();
		 List<CPListElement> flattenedProjElements= new ArrayList<>();
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

		List<CPListElement> cpelements= fClassPathList.getElements();
		int nEntries= cpelements.size();
		// backwards, as entries will be deleted
		int lastRemovePos= nEntries;
		for (int i= nEntries - 1; i >= 0; i--) {
			CPListElement cpe= cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (isEntryKind(kind)) {
				if (!flattenedProjElements.remove(cpe)) {
					cpelements.remove(i);
					lastRemovePos= i;
				}
			}
		}

		cpelements.addAll(lastRemovePos, flattenedProjElements);

		if (lastRemovePos != nEntries || !flattenedProjElements.isEmpty()) {
			fClassPathList.setElements(cpelements);
		}
	}


	private CPListElement[] openNewClassFolderDialog(CPListElement existing) {
		String title= (existing == null) ? NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_new_title : NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_edit_title;
		IProject currProject= fCurrJProject.getProject();

		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getUsedContainers(existing), existing);
		IPath projpath= currProject.getFullPath();
		dialog.setMessage(Messages.format(NewWizardMessages.LibrariesWorkbookPage_NewClassFolderDialog_description, BasicElementLabels.getPathLabel(projpath, false)));
		if (dialog.open() == Window.OK) {
			IFolder folder= dialog.getFolder();
			return new CPListElement[] { newCPLibraryElement(folder) };
		}
		return null;
	}


	private CPListElement[] openClassFolderDialog(CPListElement existing) {
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseClassFolderEntries(getShell(), fCurrJProject.getPath(), getUsedContainers(null));
			if (selected != null) {
				IWorkspaceRoot root= fCurrJProject.getProject().getWorkspace().getRoot();
				ArrayList<CPListElement> res= new ArrayList<>();
				for (IPath curr : selected) {
					IResource resource= root.findMember(curr);
					if (resource instanceof IContainer) {
						CPListElement newCPLibraryElement= newCPLibraryElement(resource);
						newCPLibraryElement.setModuleAttributeIf9OrHigher(fCurrJProject);
						res.add(newCPLibraryElement);
					}
				}
				return res.toArray(new CPListElement[res.size()]);
			}
		} else {
			// disabled
		}
		return null;
	}

	private CPListElement[] openJarFileDialog(CPListElement existing) {
		IWorkspaceRoot root= fCurrJProject.getProject().getWorkspace().getRoot();

		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseJAREntries(getShell(), fCurrJProject.getPath(), getUsedJARFiles(null));
			if (selected != null) {
				ArrayList<CPListElement> res= new ArrayList<>();

				for (IPath curr : selected) {
					IResource resource= root.findMember(curr);
					if (resource instanceof IFile) {
						CPListElement newCPLibraryElement= newCPLibraryElement(resource);
						newCPLibraryElement.setModuleAttributeIf9OrHigher(fCurrJProject);
						res.add(newCPLibraryElement);
					}
				}
				return res.toArray(new CPListElement[res.size()]);
			}
		} else {
			IPath configured= BuildPathDialogAccess.configureJAREntry(getShell(), existing.getPath(), getUsedJARFiles(existing));
			if (configured != null) {
				IResource resource= root.findMember(configured);
				if (resource instanceof IFile) {
					return new CPListElement[] { newCPLibraryElement(resource) };
				}
			}
		}
		return null;
	}

	private IPath[] getUsedContainers(CPListElement existing) {
		ArrayList<IPath> res= new ArrayList<>();
		if (fCurrJProject.exists()) {
			try {
				IPath outputLocation= fCurrJProject.getOutputLocation();
				if (outputLocation != null && outputLocation.segmentCount() > 1) { // != Project
					res.add(outputLocation);
				}
			} catch (JavaModelException e) {
				// ignore it here, just log
				JavaPlugin.log(e.getStatus());
			}
		}

		List<CPListElement> cplist= fLibrariesList.getElements();
		for (CPListElement elem : cplist) {
			if (elem.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IContainer) {
					res.add(resource.getFullPath());
				}
			}
		}
		return res.toArray(new IPath[res.size()]);
	}

	private IPath[] getUsedJARFiles(CPListElement existing) {
		List<IPath> res= new ArrayList<>();
		List<CPListElement> cplist= fLibrariesList.getElements();
		for (CPListElement elem : cplist) {
			if (elem.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (elem != existing)) {
				IResource resource= elem.getResource();
				if (resource instanceof IFile) {
					res.add(resource.getFullPath());
				}
			}
		}
		return res.toArray(new IPath[res.size()]);
	}

	private CPListElement newCPLibraryElement(IResource res) {
		return new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, res.getFullPath(), res);
	}

	private CPListElement[] openExtJarFileDialog(CPListElement existing) {
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseExternalJAREntries(getShell());
			if (selected != null) {
				ArrayList<CPListElement> res= new ArrayList<>();
				for (IPath p : selected) {
					CPListElement cpListElement= new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, p, null);
					cpListElement.setModuleAttributeIf9OrHigher(fCurrJProject);
					res.add(cpListElement);
				}
				return res.toArray(new CPListElement[res.size()]);
			}
		} else {
			IPath path;
			IPackageFragmentRoot[] roots= existing.getJavaProject().findPackageFragmentRoots(existing.getClasspathEntry());
			if (roots.length == 1)
				path= roots[0].getPath();
			else
				path= existing.getPath();
			IPath configured= BuildPathDialogAccess.configureExternalJAREntry(getShell(), path);
			if (configured != null) {
				return new CPListElement[] { new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, configured, null) };
			}
		}
		return null;
	}

	private CPListElement[] openExternalClassFolderDialog(CPListElement existing) {
		if (existing == null) {
			IPath[] selected= BuildPathDialogAccess.chooseExternalClassFolderEntries(getShell());
			if (selected != null) {
				ArrayList<CPListElement> res= new ArrayList<>();
				for (IPath p : selected) {
					CPListElement cpListElement= new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, p, null);
					cpListElement.setModuleAttributeIf9OrHigher(fCurrJProject);
					res.add(cpListElement);
				}
				return res.toArray(new CPListElement[res.size()]);
			}
		} else {
			IPath configured= BuildPathDialogAccess.configureExternalClassFolderEntries(getShell(), existing.getPath());
			if (configured != null) {
				return new CPListElement[] { new CPListElement(fCurrJProject, IClasspathEntry.CPE_LIBRARY, configured, null) };
			}
		}
		return null;
	}

	private CPListElement[] openVariableSelectionDialog(CPListElement existing) {
		List<CPListElement> existingElements= fLibrariesList.getElements();
		ArrayList<IPath> existingPaths= new ArrayList<>(existingElements.size());
		for (CPListElement elem : existingElements) {
			if (elem.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				existingPaths.add(elem.getPath());
			}
		}
		IPath[] existingPathsArray= existingPaths.toArray(new IPath[existingPaths.size()]);

		if (existing == null) {
			IPath[] paths= BuildPathDialogAccess.chooseVariableEntries(getShell(), existingPathsArray);
			if (paths != null) {
				ArrayList<CPListElement> result= new ArrayList<>();
				for (IPath path : paths) {
					CPListElement elem= createCPVariableElement(path);
					if (!existingElements.contains(elem)) {
						elem.setModuleAttributeIf9OrHigher(fCurrJProject);
						result.add(elem);
					}
				}
				return result.toArray(new CPListElement[result.size()]);
			}
		} else {
			IPath path= BuildPathDialogAccess.configureVariableEntry(getShell(), existing.getPath(), existingPathsArray);
			if (path != null) {
				return new CPListElement[] { createCPVariableElement(path) };
			}
		}
		return null;
	}

	private CPListElement createCPVariableElement(IPath path) {
		CPListElement elem= new CPListElement(fCurrJProject, IClasspathEntry.CPE_VARIABLE, path, null);
		IPath resolvedPath= JavaCore.getResolvedVariablePath(path);
		elem.setIsMissing((resolvedPath == null) || !resolvedPath.toFile().exists());
		return elem;
	}

	private CPListElement[] openContainerSelectionDialog(CPListElement existing) {
		boolean shouldAddModule= false;
		if (this.getSelection().size() == 1) {
			Object obj= this.getSelection().get(0);
			if (obj instanceof RootCPListElement) {
				shouldAddModule= ((RootCPListElement) obj).isModulePathRootNode();
			}
		}
		if (existing == null) {
			IClasspathEntry[] created= BuildPathDialogAccess.chooseContainerEntries(getShell(), fCurrJProject, getRawClasspath());
			if (created != null) {
				CPListElement[] res= new CPListElement[created.length];
				for (int i= 0; i < res.length; i++) {
					res[i]= CPListElement.create(created[i], true, fCurrJProject);
					if (res[i].getClasspathEntry().getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						if (!isJREContainer(res[i].getPath())) {
							res[i].updateExtraAttributeOfClasspathEntry();
						}
						if (!shouldAddModule) {
							res[i].setAttribute(CPListElement.MODULE, null);
						}
					}
				}
				return res;
			}
		} else {
			IClasspathEntry existingEntry= existing.getClasspathEntry();
			IClasspathEntry created= BuildPathDialogAccess.configureContainerEntry(getShell(), existingEntry, fCurrJProject, getRawClasspath());
			if (created != null) {
				CPListElement elem= new CPListElement(null, fCurrJProject, created, IClasspathEntry.CPE_CONTAINER, created.getPath(), null, ! created.equals(existingEntry), null, null);
				IVMInstall vmInstall= JavaRuntime.getVMInstall(created.getPath());
				if (vmInstall != null) {
					if(JavaRuntime.isModularJava(vmInstall)) {
						elem.updateExtraAttributeOfClasspathEntry();
					}
				}
				return new CPListElement[] { elem };
			}
		}
		return null;
	}

	private IClasspathEntry[] getRawClasspath() {
		IClasspathEntry[] currEntries= new IClasspathEntry[fClassPathList.getSize()];
		for (int i= 0; i < currEntries.length; i++) {
			CPListElement curr= fClassPathList.getElement(i);
			currEntries[i]= curr.getClasspathEntry();
		}
		return currEntries;
	}

	@Override
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE || kind == IClasspathEntry.CPE_CONTAINER;
	}

	/*
	 * @see BuildPathBasePage#getSelection
	 */
	@Override
	public List<?> getSelection() {
		return fLibrariesList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */
	@Override
	public void setSelection(List<?> selElements, boolean expand) {
		fLibrariesList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (Object selElement : selElements) {
				fLibrariesList.expandElement(selElement, 1);
			}
		}
	}

	@Override
	public void setFocus() {
		fLibrariesList.setFocus();
	}

	public void selectRootNode(boolean modulePath) {
		selectRootNode(fLibrariesList, modulePath);
	}
}
