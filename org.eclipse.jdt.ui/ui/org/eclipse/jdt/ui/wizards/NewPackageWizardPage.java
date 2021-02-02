/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Philippe Marschall <philippe.marschall@netcetera.ch> - [type wizards] Allow the creation of a compilation unit called package-info.java - https://bugs.eclipse.org/86168
 *     Michael Pellaton <michael.pellaton@netcetera.ch> - [type wizards] Allow the creation of a compilation unit called package-info.java - https://bugs.eclipse.org/86168
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocCommentReader;
import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Wizard page to create a new package.
 *
 * <p>
 * Note: This class is not intended to be subclassed, but clients can instantiate.
 * To implement a different kind of a new package wizard page, extend <code>NewContainerWizardPage</code>.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NewPackageWizardPage extends NewTypeWizardPage {

	private static final String PACKAGE_INFO_JAVA_FILENAME= JavaModelUtil.PACKAGE_INFO_JAVA;

	private static final String PACKAGE_HTML_FILENAME= JavaModelUtil.PACKAGE_HTML;

	private static final String PAGE_NAME= "NewPackageWizardPage"; //$NON-NLS-1$

	private static final String PACKAGE_NAME= "NewPackageWizardPage.package"; //$NON-NLS-1$

	private final static String SETTINGS_CREATEPACKAGE_INFO_JAVA= "create_package_info_java"; //$NON-NLS-1$

	private StringDialogField fPackageDialogField;

	private SelectionButtonDialogField fCreatePackageInfoJavaDialogField;

	private boolean fInitialCommentState;

	/*
	 * Status of last validation of the package field
	 */
	private IStatus fPackageNameStatus;

	private IPackageFragment fCreatedPackageFragment;

	private Link fLinkControl;

	/**
	 * Creates a new <code>NewPackageWizardPage</code>
	 */
	public NewPackageWizardPage() {
		super(false, PAGE_NAME);

		setTitle(NewWizardMessages.NewPackageWizardPage_title);
		setDescription(NewWizardMessages.NewPackageWizardPage_description);

		fCreatedPackageFragment= null;

		PackageFieldAdapter adapter= new PackageFieldAdapter();

		fPackageDialogField= new StringDialogField();
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.NewPackageWizardPage_package_label);

		fCreatePackageInfoJavaDialogField= new SelectionButtonDialogField(SWT.CHECK);
		fCreatePackageInfoJavaDialogField.setDialogFieldListener(adapter);
		fCreatePackageInfoJavaDialogField.setLabelText(NewWizardMessages.NewPackageWizardPage_package_CreatePackageInfoJava);

		fPackageNameStatus= new StatusInfo();
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection. The selection is used to initialize the fields of the wizard
	 * page.
	 *
	 * @param selection used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);

		initContainerPage(jelem);
		String pName= ""; //$NON-NLS-1$
		if (jelem != null) {
			IPackageFragment pf= (IPackageFragment) jelem.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			if (pf != null && !pf.isDefaultPackage()) {
				pName= pf.getElementName();
			} else {
				if (jelem.getJavaProject() != null) {
					final IPackageFragmentRoot pkgFragmentRoot= getPackageFragmentRoot();
					if (pkgFragmentRoot != null && pkgFragmentRoot.exists()) {
						try {
							IJavaElement[] packages= pkgFragmentRoot.getChildren();
							if (packages.length == 1) { // only default package
								String prName= jelem.getJavaProject().getElementName();
								IStatus status= getPackageStatus(prName);
								if (status.getSeverity() == IStatus.OK) {
									pName= prName;
								}
							}
						} catch (JavaModelException e) {
							// fall through
						}
					}
				}
			}
		}
		setPackageText(pName, true);

		IDialogSettings dialogSettings= getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section= dialogSettings.getSection(PAGE_NAME);
			if (section != null) {
				boolean createPackageDocumentation= section.getBoolean(SETTINGS_CREATEPACKAGE_INFO_JAVA);
				fCreatePackageInfoJavaDialogField.setSelection(createPackageDocumentation);
			}
		}

		updateStatus(new IStatus[] { fContainerStatus, fPackageNameStatus });
		setAddComments(StubUtility.doAddComments(getJavaProject()), true); // from project or workspace
	}

	// -------- UI Creation ---------

	/*
	 * @see WizardPage#createControl
	 */
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		int nColumns= 3;

		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		composite.setLayout(layout);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(NewWizardMessages.NewPackageWizardPage_info);
		GridData gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(60);
		gd.horizontalSpan= 3;
		label.setLayoutData(gd);

		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		fLinkControl= createCommentWithLinkControls(composite, nColumns, false);
		if(fAddCommentButton!=null)
			fAddCommentButton.setEnabled(fInitialCommentState);
		if(fLinkControl!=null)
			fLinkControl.setEnabled(fInitialCommentState);
		enableCommentControl(true);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_PACKAGE_WIZARD_PAGE);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	/**
	 * Sets the focus to the package name input field.
	 */
	@Override
	protected void setFocus() {
		fPackageDialogField.setFocus(false);
	}


	@Override
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns - 1);
		Text text= fPackageDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(text);
		DialogField.createEmptySpace(composite);

		TextFieldNavigationHandler.install(text);
		Control[] doFillIntoGrid= fCreatePackageInfoJavaDialogField.doFillIntoGrid(composite, nColumns);
		Button createPackageButton= (Button) doFillIntoGrid[0];
		createPackageButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fAddCommentButton != null)
					fAddCommentButton.setEnabled(createPackageButton.getSelection());
				if (fLinkControl != null)
					fLinkControl.setEnabled(createPackageButton.getSelection());

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//nothing to do
			}
		});
		fInitialCommentState = createPackageButton.getSelection();
		BidiUtils.applyBidiProcessing(fPackageDialogField.getTextControl(null), StructuredTextTypeHandlerFactory.JAVA);
	}

	// -------- PackageFieldAdapter --------

	private class PackageFieldAdapter implements IDialogFieldListener {

		// --------- IDialogFieldListener

		@Override
		public void dialogFieldChanged(DialogField field) {
			fPackageNameStatus= getPackageStatus(getPackageText());
			// tell all others
			handleFieldChanged(PACKAGE_NAME);
		}
	}

	// -------- update message ----------------

	/*
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (CONTAINER.equals(fieldName)) {
			fPackageNameStatus= getPackageStatus(getPackageText());
		}
		// do status line update
		updateStatus(new IStatus[] { fContainerStatus, fPackageNameStatus });
	}

	// ----------- validation ----------

	private IStatus validatePackageName(String text) {
		IJavaProject project= getJavaProject();
		if (project == null || !project.exists()) {
			return JavaConventions.validatePackageName(text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3);
		}
		return JavaConventionsUtil.validatePackageName(text, project);
	}

	/**
	 * Validates the package name and returns the status of the validation.
	 *
	 * @param packName the package name
	 *
	 * @return the status of the validation
	 */
	private IStatus getPackageStatus(String packName) {
		StatusInfo status= new StatusInfo();
		if (packName.length() > 0) {
			IStatus val= validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(Messages.format(NewWizardMessages.NewPackageWizardPage_error_InvalidPackageName, val.getMessage()));
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(Messages.format(NewWizardMessages.NewPackageWizardPage_warning_DiscouragedPackageName, val.getMessage()));
			}
		} else {
			status.setError(NewWizardMessages.NewPackageWizardPage_error_EnterName);
			return status;
		}

		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null && root.getJavaProject().exists()) {
			IPackageFragment pack= root.getPackageFragment(packName);
			try {
				IPath rootPath= root.getPath();
				IPath outputPath= root.getJavaProject().getOutputLocation();
				if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
					// if the bin folder is inside of our root, don't allow to name a package
					// like the bin folder
					IPath packagePath= pack.getPath();
					if (outputPath.isPrefixOf(packagePath)) {
						status.setError(NewWizardMessages.NewPackageWizardPage_error_IsOutputFolder);
						return status;
					}
				}
				if (pack.exists()) {
					// it's ok for the package to exist as long we want to create package level documentation
					// and the package level documentation doesn't exist
					if (!isCreatePackageDocumentation() || packageDocumentationAlreadyExists(pack)) {
						if (pack.containsJavaResources() || !pack.hasSubpackages()) {
							status.setError(NewWizardMessages.NewPackageWizardPage_error_PackageExists);
						} else {
							status.setError(NewWizardMessages.NewPackageWizardPage_error_PackageNotShown);
						}
					}
				} else {
					IResource resource= pack.getResource();
					if (resource != null && !ResourcesPlugin.getWorkspace().validateFiltered(resource).isOK()) {
						status.setError(NewWizardMessages.NewPackageWizardPage_error_PackageNameFiltered);
						return status;
					}
					URI location= pack.getResource().getLocationURI();
					if (location != null) {
						IFileStore store= EFS.getStore(location);
						if (store.fetchInfo().exists()) {
							status.setError(NewWizardMessages.NewPackageWizardPage_error_PackageExistsDifferentCase);
						}
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return status;
	}

	private boolean packageDocumentationAlreadyExists(IPackageFragment pack) throws JavaModelException {
		ICompilationUnit packageInfoJava= pack.getCompilationUnit(PACKAGE_INFO_JAVA_FILENAME);
		if (packageInfoJava.exists()) {
			return true;
		}
		for (Object resource : pack.getNonJavaResources()) {
			if (resource instanceof IFile) {
				IFile file= (IFile) resource;
				String fileName= file.getName();
				if (PACKAGE_HTML_FILENAME.equals(fileName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the content of the package input field.
	 *
	 * @return the content of the package input field
	 */
	@Override
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Returns the content of the create package documentation input field.
	 *
	 * @return the content of the create package documentation input field
	 * @since 3.9
	 */
	public boolean isCreatePackageDocumentation() {
		return fCreatePackageInfoJavaDialogField.isSelected();
	}

	/**
	 * Sets the content of the package input field to the given value.
	 *
	 * @param str the new package input field text
	 * @param canBeModified if <code>true</code> the package input
	 * field can be modified; otherwise it is read-only.
	 */
	public void setPackageText(String str, boolean canBeModified) {
		fPackageDialogField.setText(str);

		fPackageDialogField.setEnabled(canBeModified);
	}

	/**
	 * Returns the resource handle that corresponds to the element that was created or
	 * will be created.
	 * @return A resource or null if the page contains illegal values.
	 * @since 3.0
	 */
	@Override
	public IResource getModifiedResource() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			IPackageFragment pack= root.getPackageFragment(getPackageText());
			IResource packRes= pack.getResource();
			if (isCreatePackageDocumentation()) {
				if (JavaModelUtil.is50OrHigher(getJavaProject())) {
					return pack.getCompilationUnit(PACKAGE_INFO_JAVA_FILENAME).getResource();
				} else if (packRes instanceof IFolder){
					return ((IFolder) packRes).getFile(PACKAGE_HTML_FILENAME);
				}
			}

			return packRes;
		}
		return null;
	}


	// ---- creation ----------------

	/**
	 * Returns a runnable that creates a package using the current settings.
	 *
	 * @return the runnable that creates the new package
	 */
	@Override
	public IRunnableWithProgress getRunnable() {
		return monitor -> {
			try {
				createPackage(monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
	}

	/**
	 * Returns the created package fragment. This method only returns a valid value
	 * after <code>getRunnable</code> or <code>createPackage</code> have been
	 * executed.
	 *
	 * @return the created package fragment
	 */
	public IPackageFragment getNewPackageFragment() {
		return fCreatedPackageFragment;
	}

	/**
	 * Creates the new package using the entered field values.
	 *
	 * @param monitor a progress monitor to report progress. The progress
	 * monitor must not be <code>null</code>
	 * @throws CoreException Thrown if creating the package failed.
	 * @throws InterruptedException Thrown when the operation has been canceled.
	 * @since 2.1
	 */
	public void createPackage(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}

		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= root.getPackageFragment(getPackageText());

		if (pack.exists()) {
			fCreatedPackageFragment= pack;
		} else {
			fCreatedPackageFragment= root.createPackageFragment(getPackageText(), true, monitor);
		}

		if (isCreatePackageDocumentation()) {
			if (JavaModelUtil.is50OrHigher(getJavaProject())) {
				createPackageInfoJava(monitor);
			} else {
				createPackageHtml(root, monitor);
			}
		}

		// save whether package documentation should be created
		IDialogSettings dialogSettings= getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section= dialogSettings.getSection(PAGE_NAME);
			if (section == null) {
				section= dialogSettings.addNewSection(PAGE_NAME);
			}
			section.put(SETTINGS_CREATEPACKAGE_INFO_JAVA, isCreatePackageDocumentation());
		}

		if (monitor.isCanceled()) {
			throw new InterruptedException();
		}
	}

	private void createPackageInfoJava(IProgressMonitor monitor) throws CoreException {
		StringBuilder fileContent= new StringBuilder();
		fileContent.append("package "); //$NON-NLS-1$
		fileContent.append(fCreatedPackageFragment.getElementName());
		fileContent.append(";"); //$NON-NLS-1$

		InfoFilesUtil.createInfoJavaFile(PACKAGE_INFO_JAVA_FILENAME, fileContent.toString(), fCreatedPackageFragment,this.isAddComments(), monitor);
	}

	private void createPackageHtml(IPackageFragmentRoot root, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IFolder createdPackage= workspace.getRoot().getFolder(fCreatedPackageFragment.getPath());
		IFile packageHtml= createdPackage.getFile(PACKAGE_HTML_FILENAME);
		String charset= packageHtml.getCharset();
		String content= buildPackageHtmlContent(root, charset);
		try {
			packageHtml.create(new ByteArrayInputStream(content.getBytes(charset)), false, monitor);
		} catch (UnsupportedEncodingException e) {
			String message= "charset " + charset + " not supported by platform"; //$NON-NLS-1$ //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, message, e));
		}
	}

	private String buildPackageHtmlContent(IPackageFragmentRoot root, String charset) throws CoreException {
		String lineDelimiter= StubUtility.getLineDelimiterUsed(root.getJavaProject());
		StringBuilder content = new StringBuilder();
		String fileComment= InfoFilesUtil.getFileComment(PACKAGE_INFO_JAVA_FILENAME, fCreatedPackageFragment, lineDelimiter);
		String typeComment= InfoFilesUtil.getTypeComment(PACKAGE_INFO_JAVA_FILENAME, fCreatedPackageFragment, lineDelimiter);

		content.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">"); //$NON-NLS-1$
		content.append(lineDelimiter);
		if (fileComment != null) {
			content.append("<!--"); //$NON-NLS-1$
			content.append(lineDelimiter);
			content.append(stripJavaComments(fileComment));
			content.append(lineDelimiter);
			content.append("-->"); //$NON-NLS-1$
			content.append(lineDelimiter);
		}
		content.append("<html>"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("<head>"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=");  //$NON-NLS-1$
		content.append(charset);
		content.append("\">"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("<title>"); //$NON-NLS-1$
		content.append(fCreatedPackageFragment.getElementName());
		content.append("</title>"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("</head>"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("<body>"); //$NON-NLS-1$
		content.append(lineDelimiter);

		if (typeComment != null) {
			content.append(stripJavaComments(typeComment));
			content.append(lineDelimiter);
		}

		content.append("</body>"); //$NON-NLS-1$
		content.append(lineDelimiter);
		content.append("</html>"); //$NON-NLS-1$

		return content.toString();
	}

	private String stripJavaComments(String comment) {
		DocumentAdapter documentAdapter= new DocumentAdapter(null, fCreatedPackageFragment.getPath());
		try {
			documentAdapter.setContents(comment);
			return getString(new JavaDoc2HTMLTextReader(new JavaDocCommentReader(documentAdapter, 0, comment.length())));
		} finally {
			documentAdapter.close();
		}
	}

	private static String getString(Reader reader) {
		StringBuilder buf= new StringBuilder();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

}
