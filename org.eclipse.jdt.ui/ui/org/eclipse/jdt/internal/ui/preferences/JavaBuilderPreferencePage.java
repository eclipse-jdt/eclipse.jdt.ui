/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class JavaBuilderPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private StringDialogField fResourceFilterField;
	private StatusInfo fResourceFilterStatus;
	
	private SelectionButtonDialogField fAbortInvalidClasspathField;
	
	private Hashtable fWorkingValues;

	private static final String PREF_RESOURCE_FILTER= JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER;
	private static final String PREF_BUILD_INVALID_CLASSPATH= JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH;

	private static final String ABORT= JavaCore.ABORT;
	private static final String IGNORE= JavaCore.IGNORE;

	private static String[] getAllKeys() {
		return new String[] {
			PREF_RESOURCE_FILTER, PREF_BUILD_INVALID_CLASSPATH
		};	
	}
	
	public JavaBuilderPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("JavaBuilderPreferencePage.description")); //$NON-NLS-1$
	
		fWorkingValues= JavaCore.getOptions();

		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				updateValues();
			}
		};
		
		fResourceFilterField= new StringDialogField();
		fResourceFilterField.setLabelText(JavaUIMessages.getString("JavaBuilderPreferencePage.filter.label")); //$NON-NLS-1$
		
		fAbortInvalidClasspathField= new SelectionButtonDialogField(SWT.CHECK);
		fAbortInvalidClasspathField.setDialogFieldListener(listener);
		fAbortInvalidClasspathField.setLabelText(JavaUIMessages.getString("JavaBuilderPreferencePage.abortinvalidprojects.label")); //$NON-NLS-1$
		
		updateControls();				
	
		fResourceFilterField.setDialogFieldListener(listener);
		fAbortInvalidClasspathField.setDialogFieldListener(listener);
	
	}
	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);

		DialogField resourceFilterLabel= new DialogField();
		resourceFilterLabel.setLabelText(JavaUIMessages.getString("JavaBuilderPreferencePage.filter.description")); //$NON-NLS-1$

		resourceFilterLabel.doFillIntoGrid(composite, 2);
		LayoutUtil.setWidthHint(resourceFilterLabel.getLabelControl(null), convertWidthInCharsToPixels(80));

		fResourceFilterField.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fResourceFilterField.getTextControl(null));
		LayoutUtil.setWidthHint(fResourceFilterField.getTextControl(null), convertWidthInCharsToPixels(50));

		fAbortInvalidClasspathField.doFillIntoGrid(composite, 2);

		return composite;
	}
	
	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore store) {
	}	
	

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}


	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		String[] allKeys= getAllKeys();
		// preserve other options
		// store in JCore and the preferences
		Hashtable actualOptions= JavaCore.getOptions();
		boolean hasChanges= false;
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String val=  (String) fWorkingValues.get(key);
			String oldVal= (String) actualOptions.get(key);
			hasChanges= hasChanges | !val.equals(oldVal);
			
			actualOptions.put(key, val);
		}
		
		if (hasChanges) {
			String title= JavaUIMessages.getString("JavaBuilderPreferencePage.needsbuild.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("JavaBuilderPreferencePage.needsbuild.message"); //$NON-NLS-1$
			MessageDialog dialog= new MessageDialog(getShell(), title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
			int res= dialog.open();
			if (res != 0 && res != 1) {
				JavaPlugin.getDefault().savePluginPreferences();
				return false;
			}
			
			JavaCore.setOptions(actualOptions);
			if (res == 0) {
				doFullBuild();
			}
		}
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}


	private void updateValues() {
		IStatus status= validateResourceFilters();
		fWorkingValues.put(PREF_BUILD_INVALID_CLASSPATH, fAbortInvalidClasspathField.isSelected() ? ABORT : IGNORE);
		
		updateStatus(status);
	}
	
	private String[] getFilters(String text) {
		StringTokenizer tok= new StringTokenizer(text, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}
	
	
	private IStatus validateResourceFilters() {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
				
		String text= fResourceFilterField.getText();
		String[] filters= getFilters(text);
		for (int i= 0; i < filters.length; i++) {
			String fileName= filters[i].replace('*', 'x');
			IStatus status= workspace.validateName(fileName, IResource.FILE);
			if (status.matches(IStatus.ERROR)) {
				String message= JavaUIMessages.getFormattedString("JavaBuilderPreferencePage.filter.invalidsegment.error", status.getMessage()); //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < filters.length; i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(filters[i]);
		}
		fWorkingValues.put(PREF_RESOURCE_FILTER, buf.toString());
		return new StatusInfo();
	}
	

	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	private void doFullBuild() {
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= JavaUIMessages.getString("JavaBuilderPreferencePage.builderror.title"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("JavaBuilderPreferencePage.builderror.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		}
	}		
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fWorkingValues= JavaCore.getDefaultOptions();
		updateControls();
		updateValues();
		super.performDefaults();
	}
	
	private void updateControls() {
		// update the UI
		String[] filters= getFilters((String) fWorkingValues.get(PREF_RESOURCE_FILTER));
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < filters.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(filters[i]);			
		}
		fResourceFilterField.setText(buf.toString());
		
		String build= (String) fWorkingValues.get(PREF_BUILD_INVALID_CLASSPATH);
		
		fAbortInvalidClasspathField.setSelection(ABORT.equals(build));
	}

}