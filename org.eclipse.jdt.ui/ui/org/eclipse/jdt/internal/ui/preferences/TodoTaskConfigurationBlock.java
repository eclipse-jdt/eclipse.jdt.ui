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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
  */
public class TodoTaskConfigurationBlock extends OptionsConfigurationBlock {

	private static final String PREF_COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;
	private static final String PREF_COMPILER_TASK_PRIORITIES= JavaCore.COMPILER_TASK_PRIORITIES;
	
	private static final String PREF_COMPILER_TASK_CASE_SENSITIVE= JavaCore.COMPILER_TASK_CASE_SENSITIVE;	
	
	private static final String PRIORITY_HIGH= JavaCore.COMPILER_TASK_PRIORITY_HIGH;
	private static final String PRIORITY_NORMAL= JavaCore.COMPILER_TASK_PRIORITY_NORMAL;
	private static final String PRIORITY_LOW= JavaCore.COMPILER_TASK_PRIORITY_LOW;
	
	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;	
	
	public static class TodoTask {
		public String name;
		public String priority;
	}
	
	private static class TodoTaskLabelProvider extends LabelProvider implements ITableLabelProvider {
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return null; // JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return getColumnText(element, 0);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			TodoTask task= (TodoTask) element;
			if (columnIndex == 0) {
				return task.name;
			} else {
				if (PRIORITY_HIGH.equals(task.priority)) {
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.high.priority"); //$NON-NLS-1$
				} else if (PRIORITY_NORMAL.equals(task.priority)) {
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.normal.priority"); //$NON-NLS-1$
				} else if (PRIORITY_LOW.equals(task.priority)) {
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.low.priority"); //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}	
		}

	}
	
	private static final int IDX_ADD= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 2;
	
	private IStatus fTaskTagsStatus;
	private ListDialogField fTodoTasksList;
	private SelectionButtonDialogField fCaseSensitiveCheckBox;

	public TodoTaskConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		super(context, project, getKeys());
						
		TaskTagAdapter adapter=  new TaskTagAdapter();
		String[] buttons= new String[] {
			/* 0 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.add.button"), //$NON-NLS-1$
			/* 1 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.edit.button"), //$NON-NLS-1$
			/* 2 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.remove.button"), //$NON-NLS-1$
			
		};
		fTodoTasksList= new ListDialogField(adapter, buttons, new TodoTaskLabelProvider());
		fTodoTasksList.setDialogFieldListener(adapter);
		fTodoTasksList.setLabelText(PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.label")); //$NON-NLS-1$
		fTodoTasksList.setRemoveButtonIndex(IDX_REMOVE);
		
		String[] columnsHeaders= new String[] {
			PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.name.column"), //$NON-NLS-1$
			PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.priority.column"), //$NON-NLS-1$
		};
		
		fTodoTasksList.setTableColumns(new ListDialogField.ColumnsDescription(columnsHeaders, true));
		
		fCaseSensitiveCheckBox= new SelectionButtonDialogField(SWT.CHECK);
		fCaseSensitiveCheckBox.setLabelText(PreferencesMessages.getString("TodoTaskConfigurationBlock.casesensitive.label")); //$NON-NLS-1$
		
		unpackTodoTasks();
		if (fTodoTasksList.getSize() > 0) {
			fTodoTasksList.selectFirstElement();
		} else {
			fTodoTasksList.enableButton(IDX_EDIT, false);
		}
		
		fTaskTagsStatus= new StatusInfo();		
	}
	
	public void setEnabled(boolean isEnabled) {
		fTodoTasksList.setEnabled(isEnabled);
		fCaseSensitiveCheckBox.setEnabled(isEnabled);
		
	}
	
	private static String[] getKeys() {
		return new String[] {
			PREF_COMPILER_TASK_TAGS, PREF_COMPILER_TASK_PRIORITIES, PREF_COMPILER_TASK_CASE_SENSITIVE
		};	
	}	
	
	public class TaskTagAdapter implements IListAdapter, IDialogFieldListener {

		private boolean canEdit(ListDialogField field) {
			return field.getSelectedElements().size() == 1;
		}

		public void customButtonPressed(ListDialogField field, int index) {
			doTodoButtonPressed(index);
		}

		public void selectionChanged(ListDialogField field) {
			field.enableButton(IDX_EDIT, canEdit(field));
		}
			
		public void doubleClicked(ListDialogField field) {
			if (canEdit(field)) {
				doTodoButtonPressed(IDX_EDIT);
			}
		}

		public void dialogFieldChanged(DialogField field) {
			validateSettings(PREF_COMPILER_TASK_TAGS, null);
		}			
		
	}
		
	protected Control createContents(Composite parent) {
		setShell(parent.getShell());
		
		Composite markersComposite= createMarkersTabContent(parent);
		
		validateSettings(null, null);
	
		return markersComposite;
	}

	private Composite createMarkersTabContent(Composite folder) {
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		
		Composite markersComposite= new Composite(folder, SWT.NULL);
		markersComposite.setLayout(layout);
		
		fTodoTasksList.doFillIntoGrid(markersComposite, 3);
		LayoutUtil.setHorizontalSpan(fTodoTasksList.getLabelControl(null), 2);
		
		GridData data= (GridData)fTodoTasksList.getListControl(null).getLayoutData();
		data.grabExcessHorizontalSpace= true;
		data.grabExcessVerticalSpace= true;
		data.verticalAlignment= GridData.FILL;
		//data.heightHint= SWTUtil.getTableHeightHint(table, 6);
		
		fCaseSensitiveCheckBox.doFillIntoGrid(markersComposite, 2);

		return markersComposite;
	}

	protected void validateSettings(String changedKey, String newValue) {
		if (changedKey != null) {
			if (PREF_COMPILER_TASK_TAGS.equals(changedKey)) {
				fTaskTagsStatus= validateTaskTags();
			} else {
				return;
			}
		} else {
			fTaskTagsStatus= validateTaskTags();
		}		
		IStatus status= fTaskTagsStatus; //StatusUtil.getMostSevere(new IStatus[] { fTaskTagsStatus });
		fContext.statusChanged(status);
	}
	
	private IStatus validateTaskTags() {
		return new StatusInfo();
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#performOk(boolean)
	 */
	public boolean performOk(boolean enabled) {
		packTodoTasks();
		return super.performOk(enabled);
	}

	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
		String message;
		if (fProject == null) {
			message= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
		} else {
			message= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
		}	
		return new String[] { title, message };
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#updateControls()
	 */
	protected void updateControls() {
		unpackTodoTasks();
	}
	
	private void unpackTodoTasks() {
		String currTags= (String) fWorkingValues.get(PREF_COMPILER_TASK_TAGS);	
		String currPrios= (String) fWorkingValues.get(PREF_COMPILER_TASK_PRIORITIES);
		String[] tags= getTokens(currTags, ","); //$NON-NLS-1$
		String[] prios= getTokens(currPrios, ","); //$NON-NLS-1$
		ArrayList elements= new ArrayList(tags.length);
		for (int i= 0; i < tags.length; i++) {
			TodoTask task= new TodoTask();
			task.name= tags[i].trim();
			task.priority= (i < prios.length) ? prios[i] : PRIORITY_NORMAL;
			elements.add(task);
		}
		fTodoTasksList.setElements(elements);
		
		boolean isCaseSensitive= ENABLED.equals(fWorkingValues.get(PREF_COMPILER_TASK_CASE_SENSITIVE));
		fCaseSensitiveCheckBox.setSelection(isCaseSensitive);
	}
	
	private void packTodoTasks() {
		StringBuffer tags= new StringBuffer();
		StringBuffer prios= new StringBuffer();
		List list= fTodoTasksList.getElements();
		for (int i= 0; i < list.size(); i++) {
			if (i > 0) {
				tags.append(',');
				prios.append(',');
			}
			TodoTask elem= (TodoTask) list.get(i);
			tags.append(elem.name);
			prios.append(elem.priority);
		}
		fWorkingValues.put(PREF_COMPILER_TASK_TAGS, tags.toString());
		fWorkingValues.put(PREF_COMPILER_TASK_PRIORITIES, prios.toString());
		
		String state= fCaseSensitiveCheckBox.isSelected() ? ENABLED : DISABLED;
		fWorkingValues.put(PREF_COMPILER_TASK_CASE_SENSITIVE, state);
		
	}
		
	private void doTodoButtonPressed(int index) {
		TodoTask edited= null;
		if (index != IDX_ADD) {
			edited= (TodoTask) fTodoTasksList.getSelectedElements().get(0);
		}
		
		TodoTaskInputDialog dialog= new TodoTaskInputDialog(getShell(), edited, fTodoTasksList.getElements());
		if (dialog.open() == Window.OK) {
			if (edited != null) {
				fTodoTasksList.replaceElement(edited, dialog.getResult());
			} else {
				fTodoTasksList.addElement(dialog.getResult());
			}
		}
	}

}
