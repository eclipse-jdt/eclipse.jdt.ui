/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.RootCPListElement.RootNodeChange;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public abstract class BuildPathBasePage {

	private final ClasspathAttributeConfigurationDescriptors fAttributeDescriptors;
	protected Control fSWTControl;

	public BuildPathBasePage() {
		fAttributeDescriptors= JavaPlugin.getDefault().getClasspathAttributeConfigurationDescriptors();
	}

	protected boolean editCustomAttribute(Shell shell, CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			IClasspathAttribute result= config.performEdit(shell, elem.getClasspathAttributeAccess());
			if (result != null) {
				elem.setValue(result.getValue());
				return true;
			}
		}
		return false;
	}

	protected boolean showModuleDialog(Shell shell, CPListElementAttribute elem) {
		CPListElement selElement= elem.getParent();
		// the element targeted by the CP entry will be the source module from which packages are exported:
		IJavaElement[] selectedJavaElements= ModuleEncapsulationDetail.getTargetJavaElements(selElement.getJavaProject(), selElement.getPath());
		if (selectedJavaElements == null) {
			MessageDialog dialog= new MessageDialog(shell, NewWizardMessages.BuildPathBasePage_notAddedQuestion_title, null,
					Messages.format(NewWizardMessages.BuildPathBasePage_notAddedQuestion_description, selElement.getPath().toString()),
					MessageDialog.QUESTION, 0,
					NewWizardMessages.BuildPathBasePage_addNow_button,
					NewWizardMessages.BuildPathBasePage_proceedWithoutAdding_button,
					NewWizardMessages.BuildPathBasePage_cancel_button);
			int answer= dialog.open();
			switch (answer) {
				case 0: // Add now ...
					try {
						selectedJavaElements= persistEntry(selElement);
					} catch (InvocationTargetException e) {
						ExceptionHandler.handle(e, shell, PreferencesMessages.BuildPathsPropertyPage_error_title, PreferencesMessages.BuildPathsPropertyPage_error_message);
						return false;
					} catch (InterruptedException e) {
						return false;
					}
					break;
				case 1: // Process without adding ...
					break;
				case SWT.DEFAULT:
				case 2: // Cancel
					return false;
				default:
					throw new IllegalStateException(Messages.format(NewWizardMessages.BuildPathBasePage_unexpectedAnswer_error, String.valueOf(answer)));
			}
		}
		ModuleDialog dialog= new ModuleDialog(shell, selElement, selectedJavaElements, this);
		int res= dialog.open();
		if (res == Window.OK) {
			ModuleEncapsulationDetail[] newDetails= dialog.getAllDetails();
			elem.setValue(newDetails);
			return true;
		}
		return false;
	}

	private IJavaElement[] persistEntry(CPListElement element) throws InterruptedException, InvocationTargetException {
		// NB: we assume that element is a *new* entry
		IJavaElement[] selectedJavaElements;
		IJavaProject javaProject= element.getJavaProject();
		IClasspathEntry newEntry= element.getClasspathEntry();
		IWorkspaceRunnable runnable= monitor -> {
			IClasspathEntry[] oldClasspath= javaProject.getRawClasspath();
			int nEntries= oldClasspath.length;
			IClasspathEntry[] newEntries= Arrays.copyOf(oldClasspath, nEntries+1);
			newEntries[nEntries]= newEntry;
			javaProject.setRawClasspath(newEntries, monitor);
		};
		PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));
		selectedJavaElements= ModuleEncapsulationDetail.getTargetJavaElements(element.getJavaProject(), element.getPath());
		return selectedJavaElements;
	}

	protected boolean removeCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			IClasspathAttribute result= config.performRemove(elem.getClasspathAttributeAccess());
			if (result != null) {
				elem.setValue(result.getValue());
				return true;
			}
		}
		return false;
	}

	protected void removeEncapsulationDetail(ModuleEncapsulationDetail detail) {
		CPListElementAttribute parent= detail.getParent();
		if (parent != null) {
			Object value= parent.getValue();
			if (value instanceof ModuleEncapsulationDetail[]) {
				ModuleEncapsulationDetail[] existingDetails= (ModuleEncapsulationDetail[]) value;
				int count= 0;
				for (ModuleEncapsulationDetail aDetail : existingDetails) {
					if (aDetail != detail)
						existingDetails[count++]= aDetail;
				}
				if (count < existingDetails.length) {
					ModuleEncapsulationDetail[] newDetails= new ModuleEncapsulationDetail[count];
					System.arraycopy(existingDetails, 0, newDetails, 0, count);
					parent.setValue(newDetails);
					parent.getParent().attributeChanged(CPListElement.MODULE);
				}
			}
		}
	}

	protected boolean canEditCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			return config.canEdit(elem.getClasspathAttributeAccess());
		}
		return false;
	}

	protected boolean canRemoveCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			return config.canRemove(elem.getClasspathAttributeAccess());
		}
		return false;
	}


	public abstract List<?> getSelection();
	public abstract void setSelection(List<?> selection, boolean expand);


	/**
	 * Adds an element to the page
	 *
	 * @param element the element to add
	 */
	public void addElement(CPListElement element) {
		// default implementation does nothing
	}

	public abstract boolean isEntryKind(int kind);

	protected void filterAndSetSelection(List<?> list) {
		ArrayList<Object> res= new ArrayList<>(list.size());
		for (int i= list.size()-1; i >= 0; i--) {
			Object curr= list.get(i);
			if (curr instanceof CPListElement) {
				CPListElement elem= (CPListElement) curr;
				if (elem.getParentContainer() == null && isEntryKind(elem.getEntryKind())) {
					res.add(curr);
				}
			}
		}
		setSelection(res, false);
	}

	public static void fixNestingConflicts(CPListElement[] newEntries, CPListElement[] existing, Set<CPListElement> modifiedSourceEntries) {
		for (CPListElement newEntry : newEntries) {
			addExclusionPatterns(newEntry, existing, modifiedSourceEntries);
		}
	}

	private static void addExclusionPatterns(CPListElement newEntry, CPListElement[] existing, Set<CPListElement> modifiedEntries) {
		IPath entryPath= newEntry.getPath();
		for (CPListElement curr : existing) {
			if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath currPath= curr.getPath();
				if (!currPath.equals(entryPath)) {
					if (currPath.isPrefixOf(entryPath)) {
						if (addToExclusions(entryPath, curr)) {
							modifiedEntries.add(curr);
						}
					} else if (entryPath.isPrefixOf(currPath) && newEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (addToExclusions(currPath, newEntry)) {
							modifiedEntries.add(curr);
						}
					}
				}
			}
		}
	}

	private static boolean addToExclusions(IPath entryPath, CPListElement curr) {
		IPath[] exclusionFilters= (IPath[]) curr.getAttribute(CPListElement.EXCLUSION);
		if (!JavaModelUtil.isExcludedPath(entryPath, exclusionFilters)) {
			IPath pathToExclude= entryPath.removeFirstSegments(curr.getPath().segmentCount()).addTrailingSeparator();
			IPath[] newExclusionFilters= new IPath[exclusionFilters.length + 1];
			System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0, exclusionFilters.length);
			newExclusionFilters[exclusionFilters.length]= pathToExclude;
			curr.setAttribute(CPListElement.EXCLUSION, newExclusionFilters);
			return true;
		}
		return false;
	}

	protected boolean containsOnlyTopLevelEntries(List<?> selElements) {
		if (selElements.isEmpty()) {
			return true;
		}
		for (Object elem : selElements) {
			if (elem instanceof CPListElement) {
				if (((CPListElement) elem).getParentContainer() != null) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public abstract void init(IJavaProject javaProject);

	public abstract Control getControl(Composite parent);

	public abstract void setFocus();


	protected boolean getRootExpansionState(TreeListDialogField<CPListElement> list, boolean isClassPathRoot) {
		for (CPListElement cpListElement : list.getElements()) {
			if (cpListElement.isClassPathRootNode() && isClassPathRoot) {
				return list.getTreeViewer().getExpandedState(cpListElement);
			}
			if (cpListElement.isModulePathRootNode() && !isClassPathRoot) {
				return list.getTreeViewer().getExpandedState(cpListElement);
			}
		}
		return false;
	}

	protected void setRootExpansionState(TreeListDialogField<CPListElement> list, boolean state, boolean isClassPathRoot) {
		for (CPListElement cpListElement : list.getElements()) {
			if (cpListElement.isClassPathRootNode() && isClassPathRoot) {
				list.getTreeViewer().setExpandedState(cpListElement, state);
			}
			if (cpListElement.isModulePathRootNode() && !isClassPathRoot) {
				list.getTreeViewer().setExpandedState(cpListElement, state);
			}
		}
	}


	/**
	 * @param listField the UI element holding the list of elements to be manipulated
	 * @param selElement is classpath element
	 * @param changeNodeDirection indicate in which direction the element should be moved
	 */
	protected void moveCPElementAcrossNode(TreeListDialogField<CPListElement> listField, CPListElement selElement, RootNodeChange changeNodeDirection) {
		boolean firedDialogFieldChanged= false;
		List<CPListElement> elements= listField.getElements();
		//remove from module node or classnode
		int indexOfSelElement = -1;
		for (CPListElement cpListElement : elements) {
			if (cpListElement.isRootNodeForPath()) {
				RootCPListElement rootElement= (RootCPListElement) cpListElement;
				if (rootElement.isSourceRootNode(changeNodeDirection) && rootElement.getChildren().contains(selElement)) {
					List<?> children = rootElement.getChildren();
					indexOfSelElement = children.indexOf(selElement);
					rootElement.removeCPListElement(selElement);
					listField.getTreeViewer().remove(selElement);
					break;
				}
			}
		}
		// add to classpath node or module and select the cpe
		for (CPListElement cpListElement : elements) {
			if (cpListElement.isRootNodeForPath()) {
				RootCPListElement rootCPListElement= (RootCPListElement) cpListElement;
				if (rootCPListElement.isTargetRootNode(changeNodeDirection)) {
					if (rootCPListElement.getChildren().contains(selElement))
						break;
					if (indexOfSelElement != -1) {
						rootCPListElement.getChildren().add(indexOfSelElement, selElement);
					} else {
						rootCPListElement.addCPListElement(selElement);
					}
					List<CPListElement> all= listField.getElements();
					listField.setElements(all);
					listField.refresh();
					listField.dialogFieldChanged();
					firedDialogFieldChanged = true;
					listField.getTreeViewer().expandToLevel(2);
					listField.postSetSelection(new StructuredSelection(selElement));
					break;
				}
			}
		}
		// we found no other root container to move to, fire a change event
		if (!firedDialogFieldChanged) {
			listField.dialogFieldChanged();
		}
	}

	protected void selectRootNode(TreeListDialogField<CPListElement> list, boolean modulePath) {
		for (CPListElement cpListElement : list.getElements()) {
			if (cpListElement instanceof RootCPListElement) {
				RootCPListElement root= (RootCPListElement) cpListElement;
				if (root.isModulePathRootNode() == modulePath) {
					list.selectElements(new StructuredSelection(root));
					return;
				}
			}
		}
	}

	protected abstract static class CPListAdapter implements IDialogFieldListener, ITreeListAdapter<CPListElement> {
		private final Object[] EMPTY_ARR= new Object[0];

		@Override
		public Object[] getChildren(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren(false);
			} else if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute) element;
				if (CPListElement.MODULE.equals(attribute.getKey())) {
					return (ModuleEncapsulationDetail[]) attribute.getValue();
				}
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
			Object[] children= getChildren(field, element);
			return children != null && children.length > 0;
		}
	}

	public BuildPathBasePage switchToTab(Class<? extends BuildPathBasePage> tabClass) {
		if (fSWTControl == null) {
			JavaPlugin.logErrorMessage("Page does not support tab switching: "+this.getClass()); //$NON-NLS-1$
			return null;
		}
		TabFolder tabFolder= (TabFolder) fSWTControl.getParent();
		for (TabItem tabItem : tabFolder.getItems()) {
			if (tabClass.isInstance(tabItem.getData())) {
				tabFolder.setSelection(tabItem);
				return (BuildPathBasePage) tabItem.getData();
			}
		}
		return null;
	}

	protected Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}

	protected void checkAttributeEffect(String key, IJavaProject javaProject) {
		if (IClasspathAttribute.EXTERNAL_ANNOTATION_PATH.equals(key)) {
			if (JavaCore.DISABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true))) {
				MessageDialog messageDialog= new MessageDialog(getShell(),
						NewWizardMessages.LibrariesWorkbookPage_externalAnnotationNeedsNullAnnotationEnabled_title,
						null,
						NewWizardMessages.LibrariesWorkbookPage_externalAnnotationNeedsNullAnnotationEnabled_message,
						MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL },
						0);
				messageDialog.open();
			}
		}
	}
}
