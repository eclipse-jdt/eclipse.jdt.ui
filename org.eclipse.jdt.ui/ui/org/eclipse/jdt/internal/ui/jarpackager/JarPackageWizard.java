/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;import java.net.MalformedURLException;import java.net.URL;import java.util.ArrayList;import java.util.Iterator;import java.util.List;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.resource.ImageDescriptor;import org.eclipse.jface.util.Assert;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.Wizard;import org.eclipse.ui.IExportWizard;import org.eclipse.ui.IWorkbench;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

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
		fJarPackage= new JarPackage();
		fJarPackage.setIsUsedToInitialize(false);
		setWindowTitle("JAR Packager");
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAR_PACKAGER);
		setNeedsProgressMonitor(true);
	}
	/**
	 * Initializes this wizard from the given JAR package description.
	 * 
	 * @param workbench	the workbench which launched this wizard
	 * @param jarPackage the JAR package description used to initialize this wizard
	 */
	public void init(IWorkbench workbench, JarPackage jarPackage) {
		Assert.isNotNull(workbench);
		Assert.isNotNull(jarPackage);		
		fWorkbench= workbench;
		fJarPackage= jarPackage;
		fJarPackage.setIsUsedToInitialize(true);
		fSelection= new StructuredSelection(fJarPackage.getSelectedElements());
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
			return !(status.getSeverity() == IStatus.ERROR);
		}
		return true;
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
						IProject project= (IProject)selectedElement;
						if (project.hasNature(JavaCore.NATURE_ID))
							selectedElements.add(JavaCore.create(project));
					} catch (CoreException ex) {
						// ignore selected element
						continue;
					}
				}
				else if (selectedElement instanceof IResource) {
					IJavaElement je= JavaCore.create((IResource)selectedElement);
					if (je != null && je.exists() && je.getElementType() == IJavaElement.COMPILATION_UNIT)
						selectedElements.add(je);
					else
						selectedElements.add(selectedElement);
				}
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
							continue;
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
