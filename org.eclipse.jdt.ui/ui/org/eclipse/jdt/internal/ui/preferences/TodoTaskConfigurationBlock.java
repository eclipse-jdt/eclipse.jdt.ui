package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

/**
  */
public class TodoTaskConfigurationBlock {

	private static final String PREF_COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;
	private static final String PREF_COMPILER_TASK_PRIORITIES= JavaCore.COMPILER_TASK_PRIORITIES;
	
	private static final String PRIORITY_HIGH= JavaCore.COMPILER_TASK_PRIORITY_HIGH;
	private static final String PRIORITY_NORMAL= JavaCore.COMPILER_TASK_PRIORITY_NORMAL;
	private static final String PRIORITY_LOW= JavaCore.COMPILER_TASK_PRIORITY_LOW;		
	
	private static final String DEFAULT= "default"; //$NON-NLS-1$
	private static final String USER= "user";	 //$NON-NLS-1$

	private static String[] getAllKeys() {
		return new String[] {
			PREF_COMPILER_TASK_TAGS, PREF_COMPILER_TASK_PRIORITIES
		};	
	}

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
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.high.priority");
				} else if (PRIORITY_NORMAL.equals(task.priority)) {
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.normal.priority");
				} else {
					return PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.low.priority");
				}
			}	
		}

	}

	private Map fWorkingValues;
	
	private PixelConverter fPixelConverter;

	private IStatus fTaskTagsStatus;
	private IStatusChangeListener fContext;
	private Shell fShell;
	private IJavaProject fProject; // project or null
	
	private ListDialogField fTodoTasksList;
	

	public TodoTaskConfigurationBlock(IStatusChangeListener context, IJavaProject project) {
		fContext= context;
		fProject= project;
		
		fWorkingValues= getOptions(true);
						
		TaskTagAdapter adapter=  new TaskTagAdapter();
		String[] buttons= new String[] {
			/* 0 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.add.button"),
			/* 1 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.remove.button"),
			null,
			/* 3 */ PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.edit.button"),
		};
		fTodoTasksList= new ListDialogField(adapter, buttons, new TodoTaskLabelProvider());
		fTodoTasksList.setDialogFieldListener(adapter);
		fTodoTasksList.setLabelText(PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.label"));
		fTodoTasksList.setRemoveButtonIndex(1);
		
		String[] columnsHeaders= new String[] {
			PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.name.column"),
			PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.tasks.priority.column"),
		};
		
		fTodoTasksList.setTableColumns(new ListDialogField.ColumnsDescription(columnsHeaders, false));
		unpackTodoTasks();
		if (fTodoTasksList.getSize() > 0) {
			fTodoTasksList.selectFirstElement();
		} else {
			fTodoTasksList.enableButton(3, false);
		}
		
		fTaskTagsStatus= new StatusInfo();		
	}
	
	public class TaskTagAdapter implements IListAdapter, IDialogFieldListener {

		public void customButtonPressed(ListDialogField field, int index) {
			doTodoButtonPressed(index);
		}

		public void selectionChanged(ListDialogField field) {
			field.enableButton(3, field.getSelectedElements().size() == 1);
		}
			
		public void doubleClicked(ListDialogField field) {
		}

		public void dialogFieldChanged(DialogField field) {
			validateSettings(PREF_COMPILER_TASK_TAGS);
			
			
		}			
		
	}
	
	
	private Map getOptions(boolean inheritJavaCoreOptions) {
		if (fProject != null) {
			return fProject.getOptions(inheritJavaCoreOptions);
		} else {
			return JavaCore.getOptions();
		}	
	}
	
	public boolean hasProjectSpecificOptions() {
		if (fProject != null) {
			Map settings= fProject.getOptions(false);
			String[] allKeys= getAllKeys();
			for (int i= 0; i < allKeys.length; i++) {
				if (settings.get(allKeys[i]) != null) {
					return true;
				}
			}
		}
		return false;
	}	
		
	private void setOptions(Map map) {
		if (fProject != null) {
			fProject.setOptions(map);
		} else {
			JavaCore.setOptions((Hashtable) map);
		}	
	} 
	
	/**
	 * Returns the shell.
	 * @return Shell
	 */
	public Shell getShell() {
		return fShell;
	}	
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		fShell= parent.getShell();
		
		Composite markersComposite= createMarkersTabContent(parent);
		
		validateSettings(null);
	
		return markersComposite;
	}

	private Composite createMarkersTabContent(Composite folder) {
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		
		Composite markersComposite= new Composite(folder, SWT.NULL);
		markersComposite.setLayout(layout);

		layout= new GridLayout();
		layout.numColumns= 2;

		Group group= new Group(markersComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("TodoTaskConfigurationBlock.markers.taskmarkers.label"));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);
		
		fTodoTasksList.doFillIntoGrid(group, 3);
		LayoutUtil.setHorizontalSpan(fTodoTasksList.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fTodoTasksList.getListControl(null));

		return markersComposite;
	}

	private boolean checkValue(String key, String value) {
		return value.equals(fWorkingValues.get(key));
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	private void validateSettings(String changedKey) {
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
	
	public boolean performOk(boolean enabled) {
		packTodoTasks();

		String[] allKeys= getAllKeys();
		Map actualOptions= getOptions(false);
		
		// preserve other options
		boolean hasChanges= false;
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String oldVal= (String) actualOptions.get(key);
			String val= null;
			if (enabled) {
				val= (String) fWorkingValues.get(key);
				if (!val.equals(oldVal)) {
					hasChanges= true;
					actualOptions.put(key, val);
				}
			} else {
				if (oldVal != null) {
					actualOptions.remove(key);
					hasChanges= true;
				}
			}
		}
		
		
		if (hasChanges) {
			String title= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
			String message;
			if (fProject == null) {
				message= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
			} else {
				message= PreferencesMessages.getString("TodoTaskConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
			}				
			
			MessageDialog dialog= new MessageDialog(getShell(), title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
			int res= dialog.open();
			if (res != 0 && res != 1) {
				return false;
			}
			
			setOptions(actualOptions);
			if (res == 0) {
				doFullBuild();
			}
		}
		return true;
	}
	
	private static boolean openQuestion(Shell parent, String title, String message) {
		MessageDialog dialog= new MessageDialog(parent, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
		return dialog.open() == 0;
	}
	
	private void doFullBuild() {
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						if (fProject != null) {
							fProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						} else {
							JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= PreferencesMessages.getString("TodoTaskConfigurationBlock.builderror.title"); //$NON-NLS-1$
			String message= PreferencesMessages.getString("TodoTaskConfigurationBlock.builderror.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		}
	}		
	
	public void performDefaults() {
		fWorkingValues= JavaCore.getDefaultOptions();
		fWorkingValues.put(PREF_COMPILER_TASK_TAGS, "TODO");
		fWorkingValues.put(PREF_COMPILER_TASK_PRIORITIES, PRIORITY_NORMAL);
		updateControls();
		validateSettings(null);
	}
	
	private void updateControls() {
		unpackTodoTasks();
	}
	
	private String[] getTokens(String text) {
		StringTokenizer tok= new StringTokenizer(text, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}	
	
	
	private void unpackTodoTasks() {
		String currTags= (String) fWorkingValues.get(PREF_COMPILER_TASK_TAGS);	
		String currPrios= (String) fWorkingValues.get(PREF_COMPILER_TASK_PRIORITIES);
		String[] tags= getTokens(currTags);
		String[] prios= getTokens(currPrios);
		ArrayList elements= new ArrayList(tags.length);
		for (int i= 0; i < tags.length; i++) {
			TodoTask task= new TodoTask();
			task.name= tags[i].trim();
			task.priority= (i < prios.length) ? prios[i] : PRIORITY_NORMAL;
			elements.add(task);
		}
		fTodoTasksList.setElements(elements);
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
	}
		
	private void doTodoButtonPressed(int index) {
		TodoTask edited= null;
		if (index != 0) {
			edited= (TodoTask) fTodoTasksList.getSelectedElements().get(0);
		}
		
		TodoTaskInputDialog dialog= new TodoTaskInputDialog(getShell(), edited, fTodoTasksList.getElements());
		if (dialog.open() == TodoTaskInputDialog.OK) {
			if (edited != null) {
				fTodoTasksList.replaceElement(edited, dialog.getResult());
			} else {
				fTodoTasksList.addElement(dialog.getResult());
			}
		}
	}
	



}
