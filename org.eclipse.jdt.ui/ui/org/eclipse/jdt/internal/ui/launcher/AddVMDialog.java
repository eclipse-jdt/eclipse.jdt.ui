/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import java.io.IOException;import java.util.Enumeration;import java.util.zip.ZipEntry;import java.util.zip.ZipFile;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.LibraryLocation;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Combo;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.DirectoryDialog;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.FileDialog;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Shell;import org.eclipse.swt.widgets.Text;

public class AddVMDialog extends StatusDialog {
	private static final String JAVA_LANG_OBJECT= "java/lang/Object.java";

	protected IVMInstallType[] fVMTypes;
	protected IVMInstallType fSelectedVMType;
	
	protected Combo fVMTypeCombo;
	
	protected StringButtonDialogField fJDKRoot;
	protected StringDialogField fVMName;
	protected StringDialogField fDebuggerTimeout;
	protected StringButtonDialogField fSystemLibrary;
	protected StringButtonDialogField fSystemLibrarySource;
	
	protected Button fUseDefaultLibrary;
	
	protected IStatus[] fStati;
		
	public AddVMDialog(Shell shell, IVMInstallType[] vmInstallTypes, IVMInstallType initialVMType) {
		super(shell);
		fStati= new IStatus[5];
		
		fVMTypes= vmInstallTypes;
		fSelectedVMType= initialVMType;
	}
	
	protected void createDialogFields() {
		fVMName= new StringDialogField();
		fVMName.setLabelText("JRE name:");
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
		fJDKRoot.setLabelText("JRE home directory:");
		fJDKRoot.setButtonLabel("Browse...");
		fJDKRoot.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				fStati[1]= validateJDKLocation();
				updateStatusLine();
				if ((fStati[1] == null || fStati[1].isOK()) && !isCustomLibraryUsed()) {
					updateLibraryFieldDefaults();
				}
			}
		});
		
		fDebuggerTimeout= new StringDialogField();
		fDebuggerTimeout.setLabelText("Debugger timeout(ms):");
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
		fSystemLibrary.setLabelText("JRE JAR file:");
		fSystemLibrary.setButtonLabel("Browse...");
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
		
		fSystemLibrarySource.setLabelText("JRE source file:");
		fSystemLibrarySource.setButtonLabel("Browse...");
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
		l.setText("JRE types:");
		
		fVMTypeCombo= new Combo(parent, SWT.READ_ONLY);
		initVMTypeCombo();
		MGridData gd= new MGridData();
		gd.horizontalSpan= 2;
		gd.horizontalAlignment= gd.FILL;
		fVMTypeCombo.setLayoutData(gd);
		
		fVMTypeCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				fSelectedVMType= fVMTypes[fVMTypeCombo.getSelectionIndex()];
				vmTypeChanged();
			}
		});
								
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
		fUseDefaultLibrary.setText("Use default library");
		gd= new MGridData();
		gd.horizontalSpan=3;
		fUseDefaultLibrary.setLayoutData(gd);
		fUseDefaultLibrary.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				if (fUseDefaultLibrary.getSelection()) {
					useDefaultSystemLibrary();
				} else {
					useCustomSystemLibrary();
				}
			}
		});
		
		
		fSystemLibrary.doFillIntoGrid(parent, 3);
		fSystemLibrarySource.doFillIntoGrid(parent, 3);
		

		initializeFields();
		
		return parent;
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
		fVMName.setText("");
		fJDKRoot.setText("");
		fDebuggerTimeout.setText("3000");
		fUseDefaultLibrary.setSelection(true);
		useDefaultSystemLibrary();
	}
	
	protected IVMInstallType getVMType() {
		return fSelectedVMType;
	}
	
	protected void setSystemLibraryFields(LibraryLocation description) {
		fSystemLibrary.setText(description.getSystemLibrary().getAbsolutePath());
		fSystemLibrarySource.setText(description.getSystemLibrarySource().getAbsolutePath());
	}
	
	
	protected IStatus validateJDKLocation() {
		String locationName= fJDKRoot.getText();
		if (locationName == null || "".equals(locationName))
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "", null);
		return getVMType().validateInstallLocation(new File(locationName));
	}

	protected IStatus validateVMName() {
		String name= fVMName.getText();
		if (name == null || "".equals(name.trim())) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "", null);
		}
		IVMInstall[] vms= getVMType().getVMInstalls();
		for (int i= 0; i < vms.length; i++) {
			if (vms[i].getName().equals(name)) {
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The name is already ussed", null);
			}
		}
		return null;
	}
	
	protected IStatus validateDebuggerTimeout() {
		String timeoutText= fDebuggerTimeout.getText();
		long timeout= 0;
		try {
			timeout= Long.valueOf(timeoutText).longValue();
		} catch (NumberFormatException e) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be a number", e);
		}
		if (timeout < 0) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be >= 0", null);
		}
		if (timeout > Integer.MAX_VALUE) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "The timeout value must be <= "+Integer.MAX_VALUE, null);
		}
		return null;
			
	}
	
	protected void updateStatusLine() {
		int currentSeverity= IStatus.OK;
		String currentMessage= "";
		for (int i= 0; i < fStati.length; i++) {
			if (fStati[i] != null) {
				boolean isBiggerSeverity= fStati[i].getSeverity() > currentSeverity;
				boolean isBetterErrorMessage= fStati[i].getSeverity() == currentSeverity &&
												(currentMessage == null || "".equals(currentMessage));
				if (isBiggerSeverity) {
					updateStatus(fStati[i]);
					currentSeverity= fStati[i].getSeverity();
				}
			}
		}
		if (currentSeverity == IStatus.OK)
			updateStatus(new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null));
	}
	
	protected void updateLibraryFieldDefaults() {
		setSystemLibraryFields(fSelectedVMType.getDefaultLibraryLocation(getInstallLocation()));
	}
	
	protected IStatus validateSystemLibrary() {
		int flag= IStatus.ERROR;
		if (!isCustomLibraryUsed())
			flag= IStatus.WARNING;

		String locationName= fSystemLibrary.getText();
		if (locationName == null || "".equals(locationName))
			return new Status(flag, JavaPlugin.getPluginId(), 0, "", null);

		File f= new File(locationName);
		if (!f.isFile())
			return new Status(flag, JavaPlugin.getPluginId(), 0, "The selected JRE JAR doesn't exist", null);
		try {
			ZipFile zip= null;
			try {
				zip= new ZipFile(f);
				ZipEntry e= zip.getEntry("java/lang/Object.class");
				if (e == null)
					return new Status(flag, JavaPlugin.getPluginId(), 0, "The selected JRE JAR doesn't contain java.lang.Object.class", null);
			} catch (IOException e) {
				return new Status(flag, JavaPlugin.getPluginId(), 0, "Exception while accessing  JRE JAR file", e);
			} finally {
				if (zip != null)
					zip.close();
			}
		} catch (IOException e) {
		}
		return null;
	}
	
	protected boolean isCustomLibraryUsed() {
		return !fUseDefaultLibrary.getSelection();
	}
	
	protected IStatus validateSystemLibrarySource() {
		String locationName= fSystemLibrarySource.getText();
		if (locationName == null || "".equals(locationName))
			return new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, "", null);
			
		File f= new File(locationName);
		if (!f.isFile())
			return new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, "The selected JRE sources file doesn't exist", null);
		if (determinePackagePrefix(f) == null)
			return new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, "Could not find Object.java in the selected JRE sources file", null);
		return null;
	}
	
	/**
	 * try finding the package prefix
	 */
	private String determinePackagePrefix(File f) {
		try {
			ZipFile zip= null;
			try {
				zip= new ZipFile(f);
				Enumeration zipEntries= zip.entries();
				while (zipEntries.hasMoreElements()) {
					ZipEntry entry= (ZipEntry) zipEntries.nextElement();
					String name= entry.getName();
					if (name.endsWith(JAVA_LANG_OBJECT)) {
						String prefix= name.substring(0, name.length() - JAVA_LANG_OBJECT.length());
						if (prefix.endsWith("/"))
							prefix= prefix.substring(0, prefix.length() - 1);
						return prefix;
					}
				}
			} catch (IOException e) {
			} finally {
				if (zip != null)
					zip.close();
			}
		} catch (IOException e) {
		}
		return null;
	}
	
	private void browseForInstallDir() {
		DirectoryDialog dialog= new DirectoryDialog(getShell());
		dialog.setFilterPath(fJDKRoot.getText());
		dialog.setMessage("Select the root directory the JRE Installation");
		String newPath= dialog.open();
		if  (newPath != null)
			fJDKRoot.setText(newPath);
	}
	
	private void browseForSystemLibrary() {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setFilterPath(fSystemLibrary.getText());
		dialog.setText("Select the library JAR file for the JRE Installation");
		dialog.setFilterExtensions(new String[] { "*.jar", "*.zip"});
		String newPath= dialog.open();
		if  (newPath != null)
			fSystemLibrary.setText(newPath);
	}
	
	private void browseForSystemLibrarySource() {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setFilterPath(fSystemLibrarySource.getText());
		dialog.setText("Select the JRE library source file");
		dialog.setFilterExtensions(new String[] { "*.jar", "*.zip"});
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
		IVMInstall vm= getConcernedVM();
		if (JavaRuntime.getDefaultVMInstall() == null)
			JavaRuntime.setDefaultVMInstall(vm);
		vm.setInstallLocation(new File(fJDKRoot.getText()));
		vm.setName(fVMName.getText());
		vm.setDebuggerTimeout(getTimeout());
		if (isCustomLibraryUsed()) {
			File systemLibrary= new File(fSystemLibrary.getText());
			File source= new File(fSystemLibrarySource.getText());
			String pathString= determinePackagePrefix(source);
			if (pathString == null)
				pathString= "";
			IPath packageRoot= new Path(pathString);
			vm.setLibraryLocation(new LibraryLocation(systemLibrary, source, packageRoot));
		} else {
			vm.setLibraryLocation(null);
		}
	}
	
	protected IVMInstall getConcernedVM() {
		return fSelectedVMType.createVMInstall(createUniqueId(fSelectedVMType));
	}
	
	private String createUniqueId(IVMInstallType vmType) {
		String id= null;
		do {
			id= String.valueOf(System.currentTimeMillis());
		} while (vmType.findVMInstall(id) != null);
		return id;
	}
	
	private void vmTypeChanged() {
		useDefaultSystemLibrary();
	}

}
