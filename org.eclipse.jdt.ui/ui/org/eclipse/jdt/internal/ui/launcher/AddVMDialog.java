/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.LibraryLocation;

public class AddVMDialog extends StatusDialog {
	private static final String JAVA_LANG_OBJECT= "java/lang/Object.java"; //$NON-NLS-1$

	private VMPreferencePage fPreferencePage;

	protected IVMInstallType[] fVMTypes;
	protected IVMInstallType fSelectedVMType;
	
	protected Combo fVMTypeCombo;
	
	protected StringButtonDialogField fJDKRoot;
	protected StringDialogField fVMName;
	protected StringDialogField fDebuggerTimeout;
	protected StringButtonDialogField fSystemLibrary;
	protected StringButtonDialogField fSystemLibrarySource;
	
	protected Button fUseDefaultLibrary;
	
	private IDialogSettings fDialogSettings;
	
	protected IStatus[] fStati;
		
	public AddVMDialog(VMPreferencePage page, IVMInstallType[] vmInstallTypes, IVMInstallType initialVMType) {
		super(page.getShell());
		fPreferencePage= page;
		fStati= new IStatus[5];
		for (int i= 0; i < fStati.length; i++) {
			fStati[i]= new StatusInfo();
		}
		
		fVMTypes= vmInstallTypes;
		fSelectedVMType= initialVMType;
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
	}
	
	/**
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, new Object[] { IJavaHelpContextIds.EDIT_JRE_DIALOG });
	}		
	
	
	protected void createDialogFields() {
		fVMName= new StringDialogField();
		fVMName.setLabelText(LauncherMessages.getString("addVMDialog.jreName")); //$NON-NLS-1$
		fVMName.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[0]= validateVMName();
				updateStatusLine();
			}
		});
		
		fJDKRoot= new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForInstallDir();
			}
		});
		fJDKRoot.setLabelText(LauncherMessages.getString("addVMDialog.jreHome")); //$NON-NLS-1$
		fJDKRoot.setButtonLabel(LauncherMessages.getString("addVMDialog.browse1")); //$NON-NLS-1$
		fJDKRoot.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[1]= validateJDKLocation();
				updateStatusLine();
				if (!isCustomLibraryUsed()) {
					updateLibraryFieldDefaults();
				}
			}
		});
		
		fDebuggerTimeout= new StringDialogField();
		fDebuggerTimeout.setLabelText(LauncherMessages.getString("addVMDialog.dbgTimeout")); //$NON-NLS-1$
		fDebuggerTimeout.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[2]= validateDebuggerTimeout();
				updateStatusLine();
			}
		});
				
		fSystemLibrary= new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForSystemLibrary();
			}
		});
		fSystemLibrary.setLabelText(LauncherMessages.getString("addVMDialog.jreJar")); //$NON-NLS-1$
		fSystemLibrary.setButtonLabel(LauncherMessages.getString("addVMDialog.browse2")); //$NON-NLS-1$
		fSystemLibrary.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[3]= validateSystemLibrary();
				updateStatusLine();
			}
		});
		
		fSystemLibrarySource= new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				browseForSystemLibrarySource();
			}
		}); 
		
		fSystemLibrarySource.setLabelText(LauncherMessages.getString("addVMDialog.jreSource")); //$NON-NLS-1$
		fSystemLibrarySource.setButtonLabel(LauncherMessages.getString("addVMDialog.browse3")); //$NON-NLS-1$
		fSystemLibrarySource.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[4]= validateSystemLibrarySource();
				updateStatusLine();
			}
		});
	}
	
	protected String getVMName() {
		return fVMName.getText();
	}
		
	protected File getInstallLocation() {
		return new File(fJDKRoot.getText());
	}
	
	protected int getTimeout() {
		return Integer.valueOf(fDebuggerTimeout.getText()).intValue();
	}	
	
	protected Control createDialogArea(Composite ancestor) {
		createDialogFields();
		Composite parent= new Composite(ancestor, SWT.NULL);
		MGridLayout layout= new MGridLayout();
		layout.numColumns= 3;
		layout.minimumWidth= convertWidthInCharsToPixels(80);
		parent.setLayout(layout);
				
		Label l= new Label(parent, SWT.NULL);
		l.setLayoutData(new MGridData());		
		l.setText(LauncherMessages.getString("addVMDialog.jreType")); //$NON-NLS-1$
		
		fVMTypeCombo= new Combo(parent, SWT.READ_ONLY);
		initVMTypeCombo();
		MGridData gd= new MGridData();
		gd.horizontalSpan= 2;
		gd.horizontalAlignment= gd.FILL;
		fVMTypeCombo.setLayoutData(gd);
		
		SelectionListener listener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				buttonSelected(e.widget);
			}
		};
		
		fVMTypeCombo.addSelectionListener(listener);
								
		fVMName.doFillIntoGrid(parent, 3);
		fJDKRoot.doFillIntoGrid(parent, 3);
		fDebuggerTimeout.doFillIntoGrid(parent, 3);
		
		l= new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.heightHint= convertVerticalDLUsToPixels(12);
		gd.horizontalSpan= 3;
		l.setLayoutData(gd);
		
		fUseDefaultLibrary= new Button(parent, SWT.CHECK);
		fUseDefaultLibrary.setText(LauncherMessages.getString("addVMDialog.useDefault")); //$NON-NLS-1$
		gd= new MGridData();
		gd.horizontalSpan=3;
		fUseDefaultLibrary.setLayoutData(gd);
		fUseDefaultLibrary.addSelectionListener(listener);
		
		fSystemLibrary.doFillIntoGrid(parent, 3);
		fSystemLibrarySource.doFillIntoGrid(parent, 3);
		

		initializeFields();
		
		return parent;
	}
	
	private void buttonSelected(Widget widget) {
		if (widget == fVMTypeCombo) {
			fSelectedVMType= fVMTypes[fVMTypeCombo.getSelectionIndex()];
			vmTypeChanged();
		} else if (widget == fUseDefaultLibrary) {
			if (fUseDefaultLibrary.getSelection()) {
				useDefaultSystemLibrary();
			} else {
				useCustomSystemLibrary();
			}
		}
	}	
	
	
	public void create() {
		super.create();
		fVMName.setFocus();
		selectVMType();
	}
	
	private void initVMTypeCombo() {
		int index= 0;
		for (int i= 0; i < fVMTypes.length; i++) {
			fVMTypeCombo.add(fVMTypes[i].getName());
		}
	}
	
	private void selectVMType() {
		for (int i= 0; i < fVMTypes.length; i++) {
			if (fSelectedVMType == fVMTypes[i])
				fVMTypeCombo.select(i);
		}
	}
	
	protected void initializeFields() {
		fVMName.setText(""); //$NON-NLS-1$
		fJDKRoot.setText(""); //$NON-NLS-1$
		fDebuggerTimeout.setText("3000"); //$NON-NLS-1$
		fUseDefaultLibrary.setSelection(true);
		useDefaultSystemLibrary();
	}
	
	protected IVMInstallType getVMType() {
		return fSelectedVMType;
	}
	
	protected void setSystemLibraryFields(LibraryLocation description) {
		fSystemLibrary.setText(description.getSystemLibrary().getPath());
		fSystemLibrarySource.setText(description.getSystemLibrarySource().getPath());
	}
	
	protected void setSystemLibraryDefaults(LibraryLocation description) {
		File systemLibrary= description.getSystemLibrary();
		if (systemLibrary.isFile())
			fSystemLibrary.setText(systemLibrary.getPath());
		else 
			fSystemLibrary.setText(""); //$NON-NLS-1$
			
		File librarySource= description.getSystemLibrarySource();
		if (librarySource.isFile())
			fSystemLibrarySource.setText(librarySource.getPath());
		else 
			fSystemLibrarySource.setText(""); //$NON-NLS-1$
	}
	
	
	protected IStatus validateJDKLocation() {
		String locationName= fJDKRoot.getText();
		if (locationName.length() == 0) {//$NON-NLS-1$
			return new StatusInfo(IStatus.ERROR, LauncherMessages.getString("addVMDialog.enterLocation"));
		}
		File file= new File(locationName);
		if (!file.exists()) {
			return new StatusInfo(IStatus.ERROR, LauncherMessages.getString("addVMDialog.locationNotExists"));
		}
		return getVMType().validateInstallLocation(file);
	}

	protected IStatus validateVMName() {
		StatusInfo status= new StatusInfo();
		String name= fVMName.getText();
		if (name == null || "".equals(name.trim())) { //$NON-NLS-1$
			status.setError(LauncherMessages.getString("addVMDialog.enterName")); //$NON-NLS-1$
		} else {
			IVMInstallType type= getVMType();
			if (fPreferencePage.isDuplicateName(type, name)) {
				status.setError(LauncherMessages.getString("addVMDialog.duplicateName")); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	protected IStatus validateDebuggerTimeout() {
		StatusInfo status= new StatusInfo();
		String timeoutText= fDebuggerTimeout.getText();
		long timeout= 0;
		try {
			timeout= Long.valueOf(timeoutText).longValue();
			if (timeout < 0) {
				status.setError(LauncherMessages.getString("addVMDialog.timeoutSmall")); //$NON-NLS-1$
			} else if (timeout > Integer.MAX_VALUE) {
				status.setError(LauncherMessages.getFormattedString("addVMDialog.timeoutLarge", Integer.toString(Integer.MAX_VALUE))); //$NON-NLS-1$
			}			
		} catch (NumberFormatException e) {
			status.setError(LauncherMessages.getString("addVMDialog.timeoutNotANumber"));
		}
		return status;
			
	}
	
	protected void updateStatusLine() {
		updateStatus(StatusUtil.getMostSevere(fStati));
	}
	
	protected void updateLibraryFieldDefaults() {
		setSystemLibraryDefaults(fSelectedVMType.getDefaultLibraryLocation(getInstallLocation()));
	}
	
	protected IStatus validateSystemLibrary() {
		int flag= IStatus.ERROR;
		if (!isCustomLibraryUsed())
			flag= IStatus.WARNING;

		String locationName= fSystemLibrary.getText();
		if (locationName == null || "".equals(locationName)) //$NON-NLS-1$
			return new StatusInfo(flag, LauncherMessages.getString("addVMDialog.missingJREJar")); //$NON-NLS-1$

		File f= new File(locationName);
		if (!f.isFile())
			return new StatusInfo(flag, LauncherMessages.getString("addVMDialog.missingJREJar")); //$NON-NLS-1$
		try {
			ZipFile zip= null;
			try {
				zip= new ZipFile(f);
				ZipEntry e= zip.getEntry("java/lang/Object.class"); //$NON-NLS-1$
				if (e == null)
					return new StatusInfo(flag, LauncherMessages.getString("addVMDialog.noObjectClass")); //$NON-NLS-1$
			} catch (IOException e) {
				JavaPlugin.log(e);
				return new StatusInfo(flag, LauncherMessages.getString("addVMDialog.jreJarException")); //$NON-NLS-1$
			} finally {
				if (zip != null)
					zip.close();
			}
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		return new StatusInfo();
	}
	
	protected boolean isCustomLibraryUsed() {
		return !fUseDefaultLibrary.getSelection();
	}
	
	protected IStatus validateSystemLibrarySource() {
		StatusInfo status= new StatusInfo();
		
		String locationName= fSystemLibrarySource.getText();
		if (locationName == null || "".equals(locationName)) { //$NON-NLS-1$
			return status;
		}
			
		File f= new File(locationName);
		if (!f.isFile()) {
			status.setError(LauncherMessages.getString("addVMDialog.missingJRESource"));
		} else {
			if (determinePackagePrefix(f) == null) {
				status.setError(LauncherMessages.getString("addVMDialog.noObjectSource"));
			}
		}
		return status;
	}
	
	/**
	 * try finding the package prefix
	 */
	private String determinePackagePrefix(File f) {
		if (!f.isFile()) {
			return null;
		}
		ZipFile zip= null;
		try {
			zip= new ZipFile(f);
			Enumeration zipEntries= zip.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry= (ZipEntry) zipEntries.nextElement();
				String name= entry.getName();
				if (name.endsWith(JAVA_LANG_OBJECT)) {
					String prefix= name.substring(0, name.length() - JAVA_LANG_OBJECT.length());
					if (prefix.endsWith("/")) //$NON-NLS-1$
						prefix= prefix.substring(0, prefix.length() - 1);
					return prefix;
				}
			}
		} catch (IOException e) {
			JavaPlugin.log(e);
		} finally {
			if (zip != null) {
				try { zip.close(); } catch (IOException e) {};
			}
		}
		return null;
	}
	
	private void browseForInstallDir() {
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setFilterPath(fJDKRoot.getText());
		dialog.setMessage(LauncherMessages.getString("addVMDialog.pickJRERootDialog.message")); //$NON-NLS-1$
		String newPath= dialog.open();
		if  (newPath != null)
			fJDKRoot.setText(newPath);
	}
	
	private void browseForSystemLibrary() {
		String currPath= fSystemLibrary.getText();
		String lastUsedDir;	
		if (currPath.length() == 0) {
			lastUsedDir= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (lastUsedDir == null) {
				lastUsedDir= fJDKRoot.getText();
			}
		} else {
			IPath prevPath= new Path(currPath);
			String ext= prevPath.getFileExtension();
			if ("jar".equals(ext) || "zip".equals(ext)) {
				prevPath= prevPath.removeLastSegments(1);
			}
			lastUsedDir= prevPath.toOSString();
		}
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setFilterPath(lastUsedDir);
		dialog.setText(LauncherMessages.getString("addVMDialog.pickJREJarDialog.text")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] { "*.jar;*.zip"}); //$NON-NLS-2$ //$NON-NLS-1$
		String newPath= dialog.open();
		if  (newPath != null) {
			fSystemLibrary.setText(newPath);
			fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, dialog.getFilterPath());
		}
	}
	
	private void browseForSystemLibrarySource() {
		String currPath= fSystemLibrarySource.getText();
		String lastUsedDir;	
		if (currPath.length() == 0) {
			currPath= fSystemLibrary.getText();
		}
		if (currPath.length() == 0) {
			lastUsedDir= fJDKRoot.getText();
		} else {
			IPath prevPath= new Path(currPath);
			String ext= prevPath.getFileExtension();
			if ("jar".equals(ext) || "zip".equals(ext)) {
				prevPath= prevPath.removeLastSegments(1);
			}
			lastUsedDir= prevPath.toOSString();
		}		
		
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setFilterPath(lastUsedDir);
		dialog.setText(LauncherMessages.getString("addVMDialog.pickJRESourceDialog.text")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] { "*.jar;*.zip"}); //$NON-NLS-1$
		String newPath= dialog.open();
		if  (newPath != null)
			fSystemLibrarySource.setText(newPath);
	}

	protected void useDefaultSystemLibrary() {
		updateLibraryFieldDefaults();
		fSystemLibrary.setEnabled(false);
		fSystemLibrarySource.setEnabled(false);
		fStati[3]= validateSystemLibrary();
		fStati[4]= validateSystemLibrarySource();
		updateStatusLine();
	}
	
	protected void useCustomSystemLibrary() {
		fSystemLibrary.setEnabled(true);
		fSystemLibrarySource.setEnabled(true);
		fStati[3]= validateSystemLibrary();
		fStati[4]= validateSystemLibrarySource();
		updateStatusLine();
	}

	protected void okPressed() {
		doOkPressed();
		super.okPressed();
	}
	
	protected void doOkPressed() {
		IVMInstall vm= new VMStandin(fSelectedVMType, createUniqueId(fSelectedVMType));
		setFieldValuesToVM(vm);
		fPreferencePage.vmAdded(vm);
	}
	
	private String createUniqueId(IVMInstallType vmType) {
		String id= null;
		do {
			id= String.valueOf(System.currentTimeMillis());
		} while (vmType.findVMInstall(id) != null);
		return id;
	}
	
	private void vmTypeChanged() {
		fStati[1]= validateJDKLocation();
		useDefaultSystemLibrary();
		updateStatusLine();
	}

	protected void setFieldValuesToVM(IVMInstall vm) {
		vm.setInstallLocation(new File(fJDKRoot.getText()).getAbsoluteFile());
		vm.setName(fVMName.getText());
		vm.setDebuggerTimeout(getTimeout());
		if (isCustomLibraryUsed()) {
			File systemLibrary= getAbsoluteFileOrEmpty(fSystemLibrary.getText());
			File source= getAbsoluteFileOrEmpty(fSystemLibrarySource.getText());
			String pathString= determinePackagePrefix(source);
			if (pathString == null)
				pathString= ""; //$NON-NLS-1$
			IPath packageRoot= new Path(pathString);
			vm.setLibraryLocation(new LibraryLocation(systemLibrary, source, packageRoot));
		} else {
			vm.setLibraryLocation(null);
		}
	}
	
	protected File getAbsoluteFileOrEmpty(String path) {
		if (path == null || "".equals(path)) //$NON-NLS-1$
			return new File(""); //$NON-NLS-1$
		return new File(path).getAbsoluteFile();
	}
}
