/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

public abstract class ContainerPage extends NewElementWizardPage {
	
	/**
	 * container field id
	 */
	protected static final String CONTAINER= "ContainerPage.container"; //$NON-NLS-1$

	/**
	 * Status of last validation
	 */
	protected IStatus fContainerStatus;

	private StringButtonDialogField fContainerDialogField;
	
	/*
	 * package fragment root corresponding to the input type (can be null)
	 */
	private IPackageFragmentRoot fCurrRoot;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	public ContainerPage(String name, IWorkspaceRoot root) {
		super(name);
		fWorkspaceRoot= root;	
		ContainerFieldAdapter adapter= new ContainerFieldAdapter();
		
		fContainerDialogField= new StringButtonDialogField(adapter);
		fContainerDialogField.setDialogFieldListener(adapter);
		fContainerDialogField.setLabelText(NewWizardMessages.getString("ContainerPage.container.label")); //$NON-NLS-1$
		fContainerDialogField.setButtonLabel(NewWizardMessages.getString("ContainerPage.container.button")); //$NON-NLS-1$
		
		fContainerStatus= new StatusInfo();
		fCurrRoot= null;
	}
			
	/**
	 * Initializes the fields provided by the container page with a given
	 * Java element as selection.
	 * @param elem The initial selection of this page or null if no
	 *             selection was available
	 */
	protected void initContainerPage(IJavaElement elem) {
		IPackageFragmentRoot initRoot= null;
		if (elem != null) {
			initRoot= JavaModelUtil.getPackageFragmentRoot(elem);
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
					initRoot= jproject.getPackageFragmentRoot(""); //$NON-NLS-1$
				}
			}
		}	
		setPackageFragmentRoot(initRoot, true);
	}
	
	/**
	 * Creates the controls for the container field.
	 * @param parent The parent composite
	 * @param nColumns The number of columns to span
	 */
	protected void createContainerControls(Composite parent, int nColumns) {
		fContainerDialogField.doFillIntoGrid(parent, nColumns);
	}
	
	protected void setFocusOnContainer() {
		fContainerDialogField.setFocus();
	}

	// -------- ContainerFieldAdapter --------

	private class ContainerFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			containerChangeControlPressed(field);
		}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			containerDialogFieldChanged(field);
		}
	}
	
	private void containerChangeControlPressed(DialogField field) {
		// take the current jproject as init element of the dialog
		IPackageFragmentRoot root= getPackageFragmentRoot();
		root= chooseSourceContainer(root);
		if (root != null) {
			setPackageFragmentRoot(root, true);
		}
	}
	
	private void containerDialogFieldChanged(DialogField field) {
		if (field == fContainerDialogField) {
			fContainerStatus= containerChanged();
		}
		// tell all others
		handleFieldChanged(CONTAINER);
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
		if ("".equals(str)) { //$NON-NLS-1$
			status.setError(NewWizardMessages.getString("ContainerPage.error.EnterContainerName")); //$NON-NLS-1$
			return status;
		}
		IPath path= new Path(str);
		IResource res= fWorkspaceRoot.findMember(path);
		if (res != null) {
			int resType= res.getType();
			if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
				IProject proj= res.getProject();
				if (!proj.isOpen()) {
					status.setError(NewWizardMessages.getFormattedString("ContainerPage.error.ProjectClosed", proj.getFullPath().toString())); //$NON-NLS-1$
					return status;
				}				
				IJavaProject jproject= JavaCore.create(proj);
				fCurrRoot= jproject.getPackageFragmentRoot(res);
				if (fCurrRoot.exists()) {
					try {
						if (!proj.hasNature(JavaCore.NATURE_ID)) {
							if (resType == IResource.PROJECT) {
								status.setWarning(NewWizardMessages.getString("ContainerPage.warning.NotAJavaProject")); //$NON-NLS-1$
							} else {
								status.setWarning(NewWizardMessages.getString("ContainerPage.warning.NotInAJavaProject")); //$NON-NLS-1$
							}
							return status;
						}
					} catch (CoreException e) {
						status.setWarning(NewWizardMessages.getString("ContainerPage.warning.NotAJavaProject")); //$NON-NLS-1$
					}
					try {
						if (!JavaModelUtil.isOnBuildPath(jproject, fCurrRoot)) {
							status.setWarning(NewWizardMessages.getFormattedString("ContainerPage.warning.NotOnClassPath", str)); //$NON-NLS-1$
						}		
					} catch (JavaModelException e) {
						status.setWarning(NewWizardMessages.getFormattedString("ContainerPage.warning.NotOnClassPath", str)); //$NON-NLS-1$
					}					
					if (fCurrRoot.isArchive()) {
						status.setError(NewWizardMessages.getFormattedString("ContainerPage.error.ContainerIsBinary", str)); //$NON-NLS-1$
						return status;
					}
				}
				return status;
			} else {
				status.setError(NewWizardMessages.getFormattedString("ContainerPage.error.NotAFolder", str)); //$NON-NLS-1$
				return status;
			}
		} else {
			status.setError(NewWizardMessages.getFormattedString("ContainerPage.error.ContainerDoesNotExist", str)); //$NON-NLS-1$
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
	 * Returns the workspace root.
	 */ 
	public IWorkspaceRoot getWorkspaceRoot() {
		return fWorkspaceRoot;
	}	
	
	/**
	 * Returns the PackageFragmentRoot corresponding to the current input.
	 * @return the PackageFragmentRoot or <code>null</code> if the current
	 * input is not a valid source folder
	 */ 
	public IPackageFragmentRoot getPackageFragmentRoot() {
		return fCurrRoot;
	}

	/**
	 * Returns the text of the container field.
	 */ 	
	public String getContainerText() {
		return fContainerDialogField.getText();
	}
	
	
	/**
	 * Sets the current PackageFragmentRoot (model and text field).
	 * @param canBeModified Selects if the container field can be changed by the user
	 */ 
	public void setPackageFragmentRoot(IPackageFragmentRoot root, boolean canBeModified) {
		fCurrRoot= root;
		String str= (root == null) ? "" : root.getPath().makeRelative().toString(); //$NON-NLS-1$
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
					JavaPlugin.log(e.getStatus()); // just log, no ui in validation
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
						JavaPlugin.log(e.getStatus()); // just log, no ui in validation
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
		dialog.setTitle(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(JavaCore.create(fWorkspaceRoot));
		dialog.setInitialSelection(initElement);
		
		if (dialog.open() == dialog.OK) {
			Object element= dialog.getFirstResult();
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