package org.eclipse.jdt.internal.ui.jarpackager;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

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
		addPage(new JarPackageWizardPage(fJarPackage, fSelection));
		addPage(new JarOptionsPage(fJarPackage));
		addPage(new JarManifestWizardPage(fJarPackage));
	}
	/**
	 * Returns the image descriptor with the given relative path.
	 */
	private ImageDescriptor getImageDescriptor(String relativePath) {
		try {
			URL installURL= JavaPlugin.getDefault().getDescriptor().getInstallURL();
			URL url= new URL(installURL, "icons/full/" + relativePath);
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
		// ignore the selection argument since the main export wizard changed it
		fSelection= getValidSelection();
		if (isDescriptionSelected(fSelection))
			fJarPackage= JarPackage.readJarPackage(getDescriptionFile(fSelection));
		else {
			fJarPackage= new JarPackage();
		}
			
		setWindowTitle("JAR Packager");
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAR_PACKAGER);
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
			if (ExceptionHandler.handle(ex, getShell(), "JAR Export Error", "Creation of JAR failed"))
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
		fJarPackage.setIsUsedToInitialize(false);
		
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

	/**
	 * Gets the current workspace page selection and converts it to a valid
	 * selection for this wizard:
	 * - resources and projects are OK
	 * - CUs are OK
	 * - Java projects are OK
	 * - Source package fragments and source packages fragement roots are ok
	 * - Java elements below a CU are converted to their CU
	 * - all other input elements are ignored
	 * 
	 * @return a valid structured selection based on the current selection
	 */
	protected IStructuredSelection getValidSelection() {
		ISelection currentSelection= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)currentSelection;
			List selectedElements= new ArrayList(structuredSelection.size());
			Iterator iter= structuredSelection.iterator();
			while (iter.hasNext()) {
				Object selectedElement=  iter.next();
				if (selectedElement instanceof IProject) {
					try {
						selectedElements.addAll(Arrays.asList(((IProject)selectedElement).members()));
					} catch (CoreException ex) {
						// // ignore selected element
					}
				}
				else if (selectedElement instanceof IResource)
					selectedElements.add(selectedElement);
				else if (selectedElement instanceof IJavaElement) {
					IJavaElement je= (IJavaElement)selectedElement;
					if (je.getElementType() == IJavaElement.COMPILATION_UNIT)
						selectedElements.add(je);
					else if (je.getElementType() == IJavaElement.JAVA_PROJECT)
						selectedElements.add(je);
					else if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						try {
							if (((IPackageFragment)je).getKind() == IPackageFragmentRoot.K_SOURCE)
								selectedElements.add(je);
						} catch (JavaModelException ex) {
							// ignore selected element
						}
					}
					else if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
						try {
							if (((IPackageFragmentRoot)je).getKind() == IPackageFragmentRoot.K_SOURCE)
								selectedElements.add(je);
						} catch (JavaModelException ex) {
							// ignore selected element
						}
					}
					else {
						IJavaElement jcu= JavaModelUtility.getParent(je, IJavaElement.COMPILATION_UNIT);
						if (jcu != null) {
							ICompilationUnit cu= (ICompilationUnit)jcu;
							if (!cu.isWorkingCopy() || (cu= (ICompilationUnit)cu.getOriginalElement()) != null)
								selectedElements.add(cu);
						}
					}
				}
			}
			return new StructuredSelection(selectedElements);
		}
		else
			return StructuredSelection.EMPTY;
	}
}
