/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

public abstract class ContainerPage extends NewElementWizardPage {
	
	/**
	 * container field id
	 */
	protected static final String CONTAINER= "ContainerPage.container";
		
	private static final String CONTAINER_DIALOG= "ContainerPage.ChooseSourceContainerDialog";
	
	private static final String ERROR_CONTAINER_ENTERPATH= "ContainerPage.error.EnterContainerName";
	private static final String ERROR_CONTAINER_NOTEXISTS= "ContainerPage.error.ContainerDoesNotExist";	
	private static final String ERROR_CONTAINER_ISBINARY= "ContainerPage.error.ContainerIsBinary";
	private static final String ERROR_CONTAINER_NOTAFOLDER= "ContainerPage.error.NotAFolder";
	private static final String ERROR_CONTAINER_CLOSEDPROJ= "ContainerPage.error.ProjectClosed";

	private static final String WARNING_CONTAINER_NOJPROJECT= "ContainerPage.warning.NotAJavaProject";
	private static final String WARNING_CONTAINER_NOTINJPROJ= "ContainerPage.warning.NotInAJavaProject";
	private static final String WARNING_CONTAINER_NOTONCP= "ContainerPage.warning.NotOnClassPath";	

	/**
	 * Status of last validation
	 */
	protected IStatus fContainerStatus;

	private StringButtonDialogField fContainerDialogField;
	
	/*
	 * package fragment root corresponding to the inout type (can be null)
	 */
	private IPackageFragmentRoot fCurrRoot;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	public ContainerPage(String name, IWorkspaceRoot root) {
		super(name);
		fWorkspaceRoot= root;	
		ContainerFieldAdapter adapter= new ContainerFieldAdapter();
		
		fContainerDialogField= new StringButtonDialogField(adapter);
		fContainerDialogField.setDialogFieldListener(adapter);
		fContainerDialogField.setLabelText(getResourceString(CONTAINER + ".label"));
		fContainerDialogField.setButtonLabel(getResourceString(CONTAINER + ".button"));
		
		fContainerStatus= new StatusInfo();
		fCurrRoot= null;
	}
			
	/**
	 * Initializes the fields provided by the container page with a given
	 * java element as selection.
	 * @param elem The initial selection of this page or null if no
	 *             selection was available
	 */
	protected void initContainerPage(IJavaElement elem) {
		IPackageFragmentRoot initRoot= null;
		if (elem != null) {
			initRoot= JavaModelUtility.getPackageFragmentRoot(elem);
			if (initRoot == null || initRoot.isArchive()) {
				IJavaProject jproject= elem.getJavaProject();
				try {
					initRoot= null;
					IPackageFragmentRoot[] roots= jproject.getPackageFragmentRoots();
					for (int i= 0; i < roots.length; i++) {
						if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
							initRoot= roots[i];
							break;
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
				}
				if (initRoot == null) {
					initRoot= jproject.getPackageFragmentRoot("");
				}
			}
		}	
		setPackageFragmentRoot(initRoot, true);
	}	

	/**
	 * Creates the controls for the container field
	 * @param parent The parent composite
	 * @param nColumns The number of columns to span
	 */
	protected void createContainerControls(Composite parent, int nColumns) {
		fContainerDialogField.doFillIntoGrid(parent, nColumns);
	}
	
	// -------- ContainerFieldAdapter --------

	private class ContainerFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			// take the current jproject as init element of the dialog
			IPackageFragmentRoot root= getPackageFragmentRoot();
			IJavaProject jproject= (root != null) ? root.getJavaProject() : null; 
			root= chooseSourceContainer(jproject);
			if (root != null) {
				fContainerDialogField.setText(root.getPath().toString());
			}	
		}
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			if (field == fContainerDialogField) {
				fContainerStatus= containerChanged();
			}
			// tell all others
			handleFieldChanged(CONTAINER);
		}
	}
	
	// ----------- validation ----------
			
	/**
	 * Called after the container field has changed.
	 * Updates the model and returns the status.
	 * Model is only valid if returned status is OK
	 */
	protected IStatus containerChanged() {
		StatusInfo status= new StatusInfo();
		
		fCurrRoot= null;
		String str= getContainerText();
		if ("".equals(str)) {
			status.setError(getResourceString(ERROR_CONTAINER_ENTERPATH));
			return status;
		}
		IPath path= new Path(str);
		IResource res= fWorkspaceRoot.findMember(path);
		if (res != null) {
			int resType= res.getType();
			if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
				IProject proj= res.getProject();
				if (!proj.isOpen()) {
					status.setError(getFormattedString(ERROR_CONTAINER_CLOSEDPROJ, proj.getFullPath().toString()));
					return status;
				}				
				IJavaProject jproject= JavaCore.create(proj);
				fCurrRoot= jproject.getPackageFragmentRoot(res);
				if (fCurrRoot.exists()) {
					try {
						if (!proj.hasNature(JavaCore.NATURE_ID)) {
							if (resType == IResource.PROJECT) {
								status.setWarning(getResourceString(WARNING_CONTAINER_NOJPROJECT));
							} else {
								status.setWarning(getResourceString(WARNING_CONTAINER_NOTINJPROJ));
							}
							return status;
						}
					} catch (CoreException e) {
						status.setWarning(getResourceString(WARNING_CONTAINER_NOJPROJECT));
					}
					try {
						if (!JavaModelUtility.isOnBuildPath(fCurrRoot)) {
							status.setWarning(getFormattedString(WARNING_CONTAINER_NOTONCP, str));
						}		
					} catch (JavaModelException e) {
						status.setWarning(getFormattedString(WARNING_CONTAINER_NOTONCP, str));
					}					
					if (fCurrRoot.isArchive()) {
						status.setError(getFormattedString(ERROR_CONTAINER_ISBINARY, str));
						return status;
					}
				}
				return status;
			} else {
				status.setError(getFormattedString(ERROR_CONTAINER_NOTAFOLDER, str));
				return status;
			}
		} else {
			status.setError(getFormattedString(ERROR_CONTAINER_NOTEXISTS, str));
			return status;
		}
	}
		
	// -------- update message ----------------
	
	/**
	 * Called when a field on a page changed. Every sub type is responsible to
	 * call this method when a field on its page has changed.
	 * Subtypes override (extend) the method to add verification when own field has a
	 * dependency to an other field. (for example the class name input must be verified
	 * again, when the package field changes (check for duplicated class names))
	 * @param fieldName The name of the field that has changed (field id)
	 */
	protected void handleFieldChanged(String fieldName) {
	}	
	
	
	// ---- get ----------------
	
	/**
	 * Returns the PackageFragmentRoot corresponding to the current input.
	 * Can be null
	 */ 
	public IWorkspaceRoot getWorkspaceRoot() {
		return fWorkspaceRoot;
	}	
	
	/**
	 * Returns the PackageFragmentRoot corresponding to the current input.
	 * Can be null
	 */ 
	public IPackageFragmentRoot getPackageFragmentRoot() {
		return fCurrRoot;
	}

	/**
	 * Returns the text of the container field
	 */ 	
	public String getContainerText() {
		return fContainerDialogField.getText();
	}
	
	
	/**
	 * Sets the current PackageFragmentRoot (model and text field)
	 * @param canBeModified Selects if the container field can be changed by the user
	 */ 
	public void setPackageFragmentRoot(IPackageFragmentRoot root, boolean canBeModified) {
		fCurrRoot= root;
		String str= (root == null) ? "" : root.getPath().toString();
		fContainerDialogField.setText(str);
		fContainerDialogField.setEnabled(canBeModified);
	}	
		
	// ------------- choose source container dialog
	
	private IPackageFragmentRoot chooseSourceContainer(IJavaElement initElement) {
		Class[] acceptedClasses= new Class[] { IPackageFragmentRoot.class, IJavaProject.class };
		ISelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, false) {
			public boolean isSelectedValid(Object element) {
				try {
					if (element instanceof IJavaProject) {
						IJavaProject jproject= (IJavaProject)element;
						IPath path= jproject.getProject().getFullPath();
						return (jproject.findPackageFragmentRoot(path) != null);
					} else if (element instanceof IPackageFragmentRoot) {
						return (((IPackageFragmentRoot)element).getKind() == IPackageFragmentRoot.K_SOURCE);
					}
					return true;
				} catch (JavaModelException e) {
					ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
				}
				return false;
			}
		};
		
		acceptedClasses= new Class[] { IJavaModel.class, IPackageFragmentRoot.class, IJavaProject.class };
		ViewerFilter filter= new TypedViewerFilter(acceptedClasses) {
			public boolean select(Viewer viewer, Object parent, Object element) {
				if (element instanceof IPackageFragmentRoot) {
					try {
						return (((IPackageFragmentRoot)element).getKind() == IPackageFragmentRoot.K_SOURCE);
					} catch (JavaModelException e) {
						ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
						return false;
					}
				}
				return super.select(viewer, parent, element);
			}
		};		

		JavaElementContentProvider provider= new JavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT); 
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setValidator(validator);
		dialog.setSorter(new PackageViewerSorter());
		dialog.setTitle(getResourceString(CONTAINER_DIALOG + ".title"));
		dialog.setMessage(getResourceString(CONTAINER_DIALOG + ".description"));
		dialog.addFilter(filter);
		
		IJavaModel root= JavaCore.create(fWorkspaceRoot);
		if (dialog.open(root, initElement) == dialog.OK) {
			Object element= dialog.getPrimaryResult();
			if (element instanceof IJavaProject) {
				IJavaProject jproject= (IJavaProject)element;
				return jproject.getPackageFragmentRoot(jproject.getProject());
			} else if (element instanceof IPackageFragmentRoot) {
				return (IPackageFragmentRoot)element;
			}
			return null;
		}
		return null;
	}	
	
}