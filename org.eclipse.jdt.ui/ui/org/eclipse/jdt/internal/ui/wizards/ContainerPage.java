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
import org.eclipse.core.runtime.Path;

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
import org.eclipse.jdt.internal.ui.dialogs.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;



/**
 * Wizard pages constist of UI for the input fields, code that checks the validity of
 * the input, and code that creates the element from the input.<br>
 * In the case of the class / interface and package wizards, code can be shared, therefore
 * a hierarchy is build where each type adds more fields and verification code to the base class.
 * A framework is defined to specify how to extend functionality.<br>
 * ContainerPage contains a field / browse button to enter a container for a new java element.
 * The ContainerPage is extended by ContainerPackagePage and NewPackageCreationWizard <br>
 * The page defines a framework of methods that are overridden (extended) by base classes:
 * <dl>
 * <li><code>initFields</code> is called with a input element (java element)</li>
 * <li><code>setDefaultAttributes</code> is called when the input element could not be mapped to a java element</li>
 * <li><code>fieldsUpdated</code> is called when a dialog field changed. Every subtype is required
 * to call the method itself when a own field changed</li>
 * </dl>
 * ContainerPage offers the following methods to access to in sub types
 * <dl>
 * <li><code>getPackageFragmentRoot</code></li>
 * <li><code>createContainer</code></li>
 * </dl>
 */
public abstract class ContainerPage extends NewElementWizardPage {
		
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

	// the text field
	private StringButtonDialogField fContainerDialogField;
	
	// status of the current input text
	private StatusInfo fContainerStatus;
	
	// package fragment root corresponding to the inout type (can be null)
	private IPackageFragmentRoot fCurrRoot;
	
	private IWorkspaceRoot fWorkspaceRoot;
	
	public ContainerPage(String name, IWorkspaceRoot root) {
		super(name, JavaPlugin.getResourceBundle());
		fWorkspaceRoot= root;	
		ContainerFieldAdapter adapter= new ContainerFieldAdapter();
		
		fContainerDialogField= new StringButtonDialogField(adapter);
		fContainerDialogField.setDialogFieldListener(adapter);
		fContainerDialogField.setLabelText(getResourceString(CONTAINER + ".label"));
		fContainerDialogField.setButtonLabel(getResourceString(CONTAINER + ".button"));
		
		fContainerStatus= new StatusInfo();
		fContainerStatus.setError(getResourceString(ERROR_CONTAINER_ENTERPATH));	
	
		fCurrRoot= null;
	}
			
	// -------- Initialization ---------
	
	/**
	 * Should be called from the wizard with the input element.
	 */
	public final void init(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty()) {
			setDefaultAttributes();
			return;
		}
		
		Object selectedElement= selection.getFirstElement();
		if (selectedElement instanceof IAdaptable) {
			IAdaptable adaptable= (IAdaptable) selectedElement;			
			
			IJavaElement jelem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
			if (jelem != null) {
				initFields(jelem);
			} else {
				IResource resource= (IResource) adaptable.getAdapter(IResource.class);
				IProject proj= resource.getProject();
				if (proj != null) {
					IJavaProject jproject= JavaCore.create(proj);
					IJavaElement root= findFirstPackageFragmentRoot(jproject);
					if (root != null) {
						initFields(root);
					} else {
						setDefaultAttributes();
						fContainerDialogField.setText(proj.getFullPath().toString());
					}
				} else {
					setDefaultAttributes();
				}
			}
		} else {			
			setDefaultAttributes();
		}
	}
	
	private IPackageFragmentRoot findFirstPackageFragmentRoot(IJavaProject jproject) {
		try {
			IPackageFragmentRoot[] roots= jproject.getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
					return roots[i];
				}
			}
		} catch (JavaModelException e) {
		}
		return null;
	}
			
	
	/**
	 * Framework method: Called with the pages input element maopped to a java element
	 * Override in subtypes to initialize the subtypes fields
	 */
	protected void initFields(IJavaElement selection) {
		IJavaProject jproject= selection.getJavaProject();
		IPackageFragmentRoot root= JavaModelUtility.getPackageFragmentRoot(selection);
		if ((root == null || root.isArchive()) && jproject != null) {
			root= findFirstPackageFragmentRoot(jproject);
		}
		
		if (root == null) {
			if (jproject != null) {
				fContainerDialogField.setText(jproject.getProject().getFullPath().toString());
			} else {
				fContainerDialogField.setText("");
			}
		} else {
			fContainerDialogField.setText(root.getPath().toString());
		}
	}	

	/**
	 * Framework method: Called when the page's input element could not be mapped to a java element
	 * Override in subtypes to initialize the subtypes fields
	 */
	protected void setDefaultAttributes() {
		fContainerDialogField.setText("");
	}
	
	// -------- UI creation --------
	
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
				updateContainerStatus();
			}
			// tell all others
			fieldUpdated(CONTAINER);
		}
	}
	
	// ----------- validation ----------
	
	protected StatusInfo getContainerStatus() {
		return fContainerStatus;
	}
		
	/**
	 * Verify if the container input field is valid
	 */
	private void updateContainerStatus() {
		fCurrRoot= null;
		fContainerStatus.setError("UNDEF");
		String str= fContainerDialogField.getText();
		if ("".equals(str)) {
			fContainerStatus.setError(getResourceString(ERROR_CONTAINER_ENTERPATH));
		} else {
			IPath path= new Path(str);
			IResource res= fWorkspaceRoot.findMember(path);
			if (res != null) {
				int resType= res.getType();
				if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
					IProject proj= res.getProject();
					if (!proj.isOpen()) {
						fContainerStatus.setError(getFormattedString(ERROR_CONTAINER_CLOSEDPROJ, proj.getFullPath().toString()));
						return;
					}				
					IJavaProject jproject= JavaCore.create(proj);
					fCurrRoot= jproject.getPackageFragmentRoot(res);
					if (fCurrRoot.exists()) {
						try {
							if (!proj.hasNature(JavaCore.NATURE_ID)) {
								if (resType == IResource.PROJECT) {
									fContainerStatus.setWarning(getResourceString(WARNING_CONTAINER_NOJPROJECT));
								} else {
									fContainerStatus.setWarning(getResourceString(WARNING_CONTAINER_NOTINJPROJ));
								}
								return;
							}
						} catch (CoreException e) {
							fContainerStatus.setWarning(getResourceString(WARNING_CONTAINER_NOJPROJECT));
							return;
						}
						try {
							if (!JavaModelUtility.isOnBuildPath(fCurrRoot)) {
								fContainerStatus.setWarning(getFormattedString(WARNING_CONTAINER_NOTONCP, str));
								return;
							}		
						} catch (JavaModelException e) {
							fContainerStatus.setWarning(getFormattedString(WARNING_CONTAINER_NOTONCP, str));
							return;
						}					
						if (fCurrRoot.isArchive()) {
							fContainerStatus.setError(getFormattedString(ERROR_CONTAINER_ISBINARY, str));
							return;
						}
					}
					fContainerStatus.setOK();
				} else {
					fContainerStatus.setError(getFormattedString(ERROR_CONTAINER_NOTAFOLDER, str));
				}
			} else {
				fContainerStatus.setError(getFormattedString(ERROR_CONTAINER_NOTEXISTS, str)); 
			}
		}
	}	
	
		
	// -------- update message ----------------
	
	/**
	 * Framework method: Called when a field on the page changed
	 * Every sub type is responsible to class this method when a own field is changed.
	 * Subtypes override (extend) the method to add verification when own field has a
	 * dependency to an other field. (for example the class name input must be verified
	 * again, when the package field changes (check for duplicated class names))
	 */
	protected void fieldUpdated(String fieldName) {
	}	
	
	
	// ---- get ----------------
	
	/**
	 * Returns the packagefragmentroot corresponding to the current input
	 * Can be null
	 */ 
	protected IWorkspaceRoot getWorkspaceRoot() {
		return fWorkspaceRoot;
	}	
	
	/**
	 * Returns the packagefragmentroot corresponding to the current input
	 * Can be null
	 */ 
	protected IPackageFragmentRoot getPackageFragmentRoot() {
		return fCurrRoot;
	}
	
	/**
	 * Sets the current packagefragmentroot
	 */ 
	protected void setPackageFragmentRoot(IPackageFragmentRoot root, boolean canBeModified) {
		fCurrRoot= root;
		String str= (root == null) ? "" : root.getPath().toString();
		fContainerDialogField.setText(str);
		fContainerDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Creates the container when not existing
	 */
	protected IPackageFragmentRoot createContainer(IProgressMonitor monitor) throws CoreException, InterruptedException {
		// the resource already exists
		return getPackageFragmentRoot();
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