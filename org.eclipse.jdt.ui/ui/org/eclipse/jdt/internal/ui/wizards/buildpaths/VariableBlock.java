package org.eclipse.jdt.internal.ui.wizards.buildpaths;import java.util.ArrayList;import java.util.Arrays;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;


public class VariableBlock {
		
	private static final String PAGE_NAME= "VariableBlock";
	
	private static final String VARS= PAGE_NAME + ".vars";
	private static final String RESERVED= PAGE_NAME + ".reserved";
	
	private static final String ADD= VARS + ".add.button";
	private static final String EDIT= VARS + ".edit.button";
	
	private ListDialogField fVariablesList;
	private ListDialogField fReservedList;
	
	/**
	 * Constructor for VariableBlock
	 */
	public VariableBlock() {	
		String[] buttonLabels= new String[] { 
			JavaPlugin.getResourceString(ADD), JavaPlugin.getResourceString(EDIT)
		};			
		
		ArrayList reserved= new ArrayList(3);
		reserved.add(new CPVariableElement(ClasspathVariablesPreferencePage.JDKLIB_VARIABLE, null));
				
		VariablesAdapter adapter= new VariablesAdapter();
		
		CPVariableElementLabelProvider labelProvider= new CPVariableElementLabelProvider();
		
		fVariablesList= new ListDialogField(adapter, buttonLabels, labelProvider, 0);
		fVariablesList.setDialogFieldListener(adapter);
		fVariablesList.setLabelText(JavaPlugin.getResourceString(VARS + ".label"));
		fVariablesList.setRemoveButtonLabel(JavaPlugin.getResourceString(VARS + ".remove.button"));

		fReservedList= new ListDialogField(adapter, null, labelProvider, 0);
		fReservedList.setDialogFieldListener(adapter);
		fReservedList.setLabelText(JavaPlugin.getResourceString(RESERVED + ".label"));
		
		String[] entries= JavaCore.getClasspathVariableNames();
		ArrayList elements= new ArrayList(entries.length);
		for (int i= 0; i < entries.length; i++) {
			String name= entries[i];
			IPath entryPath= JavaCore.getClasspathVariable(name);
			if (entryPath != null) {
				CPVariableElement elem= new CPVariableElement(name, entryPath);
				if (!reserved.contains(elem)) {
					elements.add(elem);
				}
			} else {
				JavaPlugin.log(new Exception("classpath variable not found: " + name));
			}
		}
		fVariablesList.setElements(elements);
		fReservedList.setElements(reserved);
	}

	public Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fVariablesList, fReservedList }, true, 420, 0);
		return composite;
	}
	
	private Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	
	private class VariablesAdapter implements IDialogFieldListener, IListAdapter {
		
		// -------- IListAdapter --------
			
		public void customButtonPressed(DialogField field, int index) {
			switch (index) {
			case 0: /* add */
				editEntries(null);
				break;
			case 1: /* edit */
				List selected= fVariablesList.getSelectedElements();			
				editEntries((CPVariableElement)selected.get(0));
				break;
			}
		}
		
		public void selectionChanged(DialogField field) {
			List selected= fVariablesList.getSelectedElements();
			fVariablesList.enableCustomButton(1, selected.size() == 1);
		}				
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
		}
	}
	
	private void editEntries(CPVariableElement entry) {
		List existingEntries= fVariablesList.getElements();
		existingEntries.addAll(fReservedList.getElements());

		VariableCreationDialog dialog= new VariableCreationDialog(getShell(), entry, existingEntries);
		if (dialog.open() != dialog.OK) {
			return;
		}
		CPVariableElement newEntry= dialog.getClasspathElement();
		if (entry == null) {
			fVariablesList.addElement(newEntry);
		} else {
			entry.setName(newEntry.getName());
			entry.setPath(newEntry.getPath());
			fVariablesList.refresh();
		}
	}
	
	public void performDefaults() {
		fVariablesList.removeAllElements();
		IPath jdkPath= JavaBasePreferencePage.getJDKPath();
		if (jdkPath == null) {
			jdkPath= new Path("");
		}
		CPVariableElement entry= new CPVariableElement(ClasspathVariablesPreferencePage.JDKLIB_VARIABLE, jdkPath);
		fVariablesList.addElement(entry);
	}

	public boolean performOk() {
		try {
			List existing= new ArrayList();
			existing.addAll(Arrays.asList(JavaCore.getClasspathVariableNames()));
			
			List elements= fVariablesList.getElements();
			for (int i= 0; i < elements.size(); i++) {
				CPVariableElement curr= (CPVariableElement) elements.get(i);
				JavaCore.setClasspathVariable(curr.getName(), curr.getPath());
				existing.remove(curr.getName());
			}
			
			for (int i= 0; i < existing.size(); i++) {
				JavaCore.removeClasspathVariable((String) existing.get(i));
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			ErrorDialog.openError(getShell(), "Error", "", e.getStatus());
			return true;
		}
		
		return true;
	}	

}
