package org.eclipse.jdt.internal.ui.preferences;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.wizards.buildpaths.VariableBlock;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String JDKLIB_VARIABLE= "JDK_LIBRARY";
	
	private VariableBlock fVariableBlock;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fVariableBlock= new VariableBlock();
	}

	/**
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		return fVariableBlock.createContents(parent);
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
		fVariableBlock.performDefaults();
	}

	/**
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		return fVariableBlock.performOk();
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
		IClasspathEntry libEntry= JavaCore.newLibraryEntry(jdkPath);
		JavaCore.setClasspathVariable(JDKLIB_VARIABLE, libEntry);
	}	
	



}
