package org.eclipse.jdt.internal.ui.preferences;import java.util.ArrayList;import java.util.Arrays;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String JDKLIB_VARIABLE= "JDK_LIBRARY";
	
	private static final String PREF_CPVARIABLES= JavaUI.ID_PLUGIN + ".cpvariables";
	private static final String PAGE_NAME= "ClasspathVariablesPreferencePage";
	
	private static final String VARS= PAGE_NAME + ".vars";
	
	private static final String ADD= VARS + ".add.button";
	private static final String EDIT= VARS + ".edit.button";
	
	private ListDialogField fVariablesList;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		
		String[] buttonLabels= new String[] { 
			JavaPlugin.getResourceString(ADD), JavaPlugin.getResourceString(EDIT)
		};			
		
		VariablesAdapter adapter= new VariablesAdapter();
		
		fVariablesList= new ListDialogField(adapter, buttonLabels, new CPVariableElementLabelProvider(), 0);
		fVariablesList.setDialogFieldListener(adapter);
		fVariablesList.setLabelText(JavaPlugin.getResourceString(VARS + ".label"));
		fVariablesList.setRemoveButtonLabel(JavaPlugin.getResourceString(VARS + ".remove.button"));
		
		String[] entries= JavaCore.getClasspathVariableNames();
		ArrayList elements= new ArrayList(entries.length);
		for (int i= 0; i < entries.length; i++) {
			String name= entries[i];
			IClasspathEntry entry= JavaCore.getClasspathVariable(name);
			if (entry != null) {
				CPVariableElement elem= new CPVariableElement(name, entry.getPath());
				if (!JDKLIB_VARIABLE.equals(name)) {
					elements.add(elem);
				}
			} else {
				JavaPlugin.log(new Exception("classpath variable not found: " + name));
			}
		}
		fVariablesList.addElements(elements);
		fVariablesList.enableCustomButton(1, false);
		
	}

	/**
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fVariablesList }, true);
		return composite;
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

		NewVariableDialog dialog= new NewVariableDialog(getShell(), entry, existingEntries);
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
	
	
	/**
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/**
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fVariablesList.removeAllElements();
		IPath jdkPath= JavaBasePreferencePage.getJDKPath();
		if (jdkPath == null) {
			jdkPath= new Path("");
		}
		CPVariableElement entry= new CPVariableElement(JDKLIB_VARIABLE, jdkPath);
		fVariablesList.addElement(entry);
	}

	/**
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		try {
			List existing= new ArrayList();
			existing.addAll(Arrays.asList(JavaCore.getClasspathVariableNames()));
			
			List elements= fVariablesList.getElements();
			for (int i= 0; i < elements.size(); i++) {
				CPVariableElement curr= (CPVariableElement) elements.get(0);
				setVariableEntry(curr.getName(), curr.getPath());
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
	


	private static void setVariableEntry(String name, IPath path) throws JavaModelException {
		IClasspathEntry libEntry;
		if (path == null) {
			libEntry= null;
		} else {
			libEntry= JavaCore.newLibraryEntry(path);
		}
		JavaCore.setClasspathVariable(name, libEntry);
	}

	
	/**
	 * Initializes the default values of this page in the preference bundle.
	 * Will be called on startup of the JavaPlugin
	 */
	public static void initDefaults(IPreferenceStore prefs) {
	}	
	
	public static void initClasspathVariables() throws JavaModelException {
		initSharedJDKVariable();
	}
	
	// ------------- shared jdk variable
	
	
	private static void initSharedJDKVariable() throws JavaModelException {
		updateJDKLibraryEntry();
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(JavaBasePreferencePage.PROP_JDK)) {
					try {
						updateJDKLibraryEntry();
					} catch (JavaModelException e) {
						JavaPlugin.log(e.getStatus());
					}
				}
			}
		});
	}
	
	private static void updateJDKLibraryEntry() throws JavaModelException {
		IPath jdkPath= JavaBasePreferencePage.getJDKPath();
		if (jdkPath == null) {
			jdkPath= new Path("");
		}
		JavaCore.setClasspathVariable(JDKLIB_VARIABLE, JavaCore.newLibraryEntry(jdkPath));
	}	
	



}
