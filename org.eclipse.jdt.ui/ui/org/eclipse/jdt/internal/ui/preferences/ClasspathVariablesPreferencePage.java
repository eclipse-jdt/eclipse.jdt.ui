package org.eclipse.jdt.internal.ui.preferences;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.wizards.buildpaths.VariableBlock;

public class ClasspathVariablesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String JRELIB_VARIABLE= "JRE_LIB";
	public static final String JRESRC_VARIABLE= "JRE_SRC";
	
	private VariableBlock fVariableBlock;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public ClasspathVariablesPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};
		fVariableBlock= new VariableBlock(listener, false, null);
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
	
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusTool.applyToStatusLine(this, status);
	}		
	
	
	/**
	 * Initializes the default values of this page in the preference bundle.
	 * Will be called on startup of the JavaPlugin
	 */
	public static void initDefaults(IPreferenceStore prefs) {
	}	
		
	// ------------- shared jdk variable
	
	
	public static void initJREVariables() {
		try {
			updateJRELibEntry();
			updateJRESrcEntry();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, "Error", "");
		}				
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				try {
					String prop= event.getProperty();
					if (prop.equals(JavaBasePreferencePage.PROP_JDK)) {
						updateJRELibEntry();
					} else if (prop.equals(JDKZipFieldEditor.PROP_SOURCE) 
						|| prop.equals(JDKZipFieldEditor.PROP_PREFIX)) {
						updateJRESrcEntry();
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
				}
			}				
		});
	}
	
	private static void updateJRELibEntry() throws JavaModelException {
			IPath jrePath= JavaBasePreferencePage.getJDKPath();
			if (jrePath == null) {
				jrePath= new Path("");
			}
			JavaCore.setClasspathVariable(JRELIB_VARIABLE, jrePath);
	}
	
	private static void updateJRESrcEntry() throws JavaModelException {
		IPath[] srcAttachPath= JavaBasePreferencePage.getJDKSourceAttachment();
		IPath srcPath;
		if (srcAttachPath == null) {
			srcPath= new Path("");
		} else {
			srcPath= srcAttachPath[0];
		}
		JavaCore.setClasspathVariable(JRESRC_VARIABLE, srcPath);		
	}

}
