package org.eclipse.jdt.internal.ui.jarpackager;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.lang.reflect.InvocationTargetException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Standard workbench wizard for exporting resources from the workspace
 * to a Java Archive (JAR) file.
 * <p>
 * This class may be instantiated and used without further configuration;
 * this class is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * <pre>
 * IWizard wizard = new JarPackageWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog = new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * During the call to <code>open</code>, the wizard dialog is presented to the
 * user. When the user hits Finish, the user-selected workspace resources 
 * are exported to the user-specified zip file, the dialog closes, and the call
 * to <code>open</code> returns.
 * </p>
 */
public class JarPackageWizard extends Wizard implements IExportWizard {
	private IWorkbench fWorkbench;
	private IStructuredSelection fSelection;
	private JarPackageWizardPage fMainPage;
	private JarManifestWizardPage fManifestPage;	
	private JarPackage fJarPackage;
	private boolean fHasNewDialogSettings;
	
	/**
	 * Creates a wizard for exporting workspace resources to a JAR file.
	 */
	public JarPackageWizard() {
		IDialogSettings workbenchSettings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings section= workbenchSettings.getSection("JarPackageWizard");
		if (section == null)
			fHasNewDialogSettings= true;
		else {
			fHasNewDialogSettings= false;
			setDialogSettings(section);
		}
	}
	/*
	 * (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public void addPages() {
		super.addPages();
		fMainPage= new JarPackageWizardPage(fJarPackage, fSelection);
		fManifestPage = new JarManifestWizardPage(fJarPackage);
		addPage(fMainPage);
		addPage(fManifestPage);
	}
	/**
	 * Returns the image descriptor with the given relative path.
	 */
	private ImageDescriptor getImageDescriptor(String relativePath) {
		try {
			AbstractUIPlugin plugin= (AbstractUIPlugin) Platform.getPlugin(PlatformUI.PLUGIN_ID);
			URL installURL= plugin.getDescriptor().getInstallURL();
			URL url= new URL(installURL, "icons/basic/" + relativePath);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// Should not happen
			return null;
		}
	}
	/*
	 * (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public boolean canFinish() {
		for (int i= 0; i < getPageCount(); i++)
			if (!((IJarPackageWizardPage)getPages()[i]).computePageCompletion())
				return false;
		return true;
	}	
	/*
	 * (non-Javadoc)
	 * Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fWorkbench= workbench;
		fSelection= selection;
		
		if (isDescriptionSelected(fSelection))
			fJarPackage= JarPackage.readJarPackage(getDescriptionFile(fSelection));
		else {
			fJarPackage= new JarPackage();
		}
			
		setWindowTitle("JAR Packager");
		setDefaultPageImageDescriptor(getImageDescriptor("wizban/exportjar_wiz.gif"));
		setNeedsProgressMonitor(true);
	}
	/*
	 * (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public boolean performFinish() {
		
		if (!executeExportOperation(new JarFileExportOperation(fJarPackage, getShell())))
			return false;
		
		// Save the dialog settings
		if (fHasNewDialogSettings) {
			IDialogSettings workbenchSettings= JavaPlugin.getDefault().getDialogSettings();
			IDialogSettings section= workbenchSettings.getSection("JarPackageWizard");
			section= workbenchSettings.addNewSection("JarPackageWizard");
			setDialogSettings(section);
		}		
		IWizardPage[] pages= getPages();
		for (int i= 0; i < getPageCount(); i++) {
			IWizardPage page= pages[i];
			if (page instanceof IJarPackageWizardPage)
				((IJarPackageWizardPage)page).saveWidgetValues();
		}
		
		// Save the manifest
		if (fJarPackage.isManifestSaved()) {
			try {
				saveManifest();
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, getShell(), "JAR Package Wizard Error", "Saving manifest in workspace failed");
			} catch (IOException ex) {
				ExceptionHandler.showStackTraceDialog(ex, getShell(), "Saving manifest in workspace failed");
			}
		}
		
		// Save the description
		if (fJarPackage.isDescriptionSaved()) {
			try {
				saveDescription();
			} catch (CoreException ex) {
				ExceptionHandler.handle(ex, getShell(), "JAR Package Wizard Error", "Saving description in workspace failed");
			} catch (IOException ex) {
				ExceptionHandler.showStackTraceDialog(ex, getShell(), "Saving description in workspace failed");
			}
		}

		return true;
	}
	/**
	 * Exports the JAR package.
	 *
	 * @return	a boolean indicating success or failure
	 */
	protected boolean executeExportOperation(JarFileExportOperation op) {
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException ex) {
			if (ExceptionHandler.handle(ex, getShell(), "JAR Package Wizard Error", "Creation of JAR failed"))
				return false;
		}
		IStatus status= op.getStatus();
		if (!status.isOK()) {
			ErrorDialog.openError(getShell(), "JAR Export Problems", null, status);
			return false;
		}
		return true;
	}

	protected boolean isDescriptionSelected(IStructuredSelection selection) {
		if (selection == null || selection.size() != 1 || !(selection.getFirstElement() instanceof IFile))
			return false;
		IFile file= (IFile)selection.getFirstElement();
		String extension= file.getFileExtension();
		if (file.isAccessible() && extension != null && extension.equals(JarPackage.DESCRIPTION_EXTENSION))
			return true;
		return false;
	}

	protected IFile getDescriptionFile(IStructuredSelection selection) {
		Assert.isLegal(isDescriptionSelected(selection));
		return (IFile)selection.getFirstElement();
	}

	protected void saveManifest() throws CoreException, IOException {
		ByteArrayOutputStream manifestOutput= new ByteArrayOutputStream();
		ByteArrayInputStream fileInput= null;
		try {
			Manifest manifest= ManifestFactory.getInstance().create(fJarPackage);
			manifest.write(manifestOutput);
			fileInput= new ByteArrayInputStream(manifestOutput.toByteArray());
			if (fJarPackage.getManifestFile().isAccessible() && fJarPackage.allowOverwrite())
				fJarPackage.getManifestFile().setContents(fileInput, true, true, null);
			else {
				org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Should ask again");
				fJarPackage.getManifestFile().create(fileInput, true, null);
			}
		}
		finally {
			if (manifestOutput != null)
				manifestOutput.close();
			if (fileInput != null)
				fileInput.close();
		}
	}

	protected void saveDescription() throws CoreException, IOException {
		// Adjust JAR package attributes
		if (fJarPackage.isManifestReused())
			fJarPackage.setGenerateManifest(false);
		fJarPackage.setInitializeFromDialog(false);
		
		ByteArrayOutputStream objectStreamOutput= new ByteArrayOutputStream();
		JarPackageWriter objectStream= fJarPackage.getWriter(objectStreamOutput);;
		ByteArrayInputStream fileInput= null;
		try {
			objectStream.writeObject(fJarPackage);
			fileInput= new ByteArrayInputStream(objectStreamOutput.toByteArray());
			if (fJarPackage.getDescriptionFile().isAccessible() && fJarPackage.allowOverwrite())
				fJarPackage.getDescriptionFile().setContents(fileInput, true, true, null);
			else {
				org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Should ask again");
				fJarPackage.getDescriptionFile().create(fileInput, true, null);	
			}
		}
		finally {
			if (fileInput != null)
				fileInput.close();
			if (objectStream != null)
				objectStream.close();
		}
	}
}
