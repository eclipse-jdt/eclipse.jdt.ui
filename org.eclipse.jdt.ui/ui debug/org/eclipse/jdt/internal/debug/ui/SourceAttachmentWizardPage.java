package org.eclipse.jdt.internal.debug.ui;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */
 import java.lang.reflect.InvocationTargetException;import java.net.URL;import org.eclipse.core.resources.IProject;import org.eclipse.core.runtime.*;import org.eclipse.jdt.core.*;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.dialogs.*;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.*;

/**
 * A wizard page to attach source at debug time.
 */
public class SourceAttachmentWizardPage extends WizardPage implements IStatusChangeListener {
	
	private final static String PREFIX = "source_attachment_wizard_page.";
	private final static String PAGE_TITLE = PREFIX + "title";
	private final static String PAGE_DESCRIPTION = PREFIX + "description";
	private final static String NO_SOURCE = PREFIX + "no_source";
	private static final String ERROR_PREFIX= PREFIX + "op_error.";	
	private static final String ADD_TO_BUILD_PATH= PREFIX + "add_to_build_path_dialog";	

	private Button fNoSourceButton;
	private Control fBlockControl;
	private boolean fNoSource = false;
	
	private IPackageFragmentRoot fJarRoot;
	private SourceAttachmentBlock fSourceAttachmentBlock;

	public SourceAttachmentWizardPage(IPackageFragmentRoot jarRoot) {
		super(DebugUIUtils.getResourceString(PAGE_TITLE));
		fJarRoot= jarRoot;
	}
	
	/**
	 * @see IWizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		IPath path = fSourceAttachmentBlock.getSourceAttachmentPath();
		return fNoSourceButton.getSelection() || (path != null && !path.isEmpty());
	}

	/**
	 * @see IDialogPage
	 */
	public void createControl(Composite parent) {
		
		Composite root= new Composite(parent, SWT.NONE);
		GridLayout l= new GridLayout();
		l.numColumns= 1;
		l.verticalSpacing = 10;
		l.makeColumnsEqualWidth= true;
		root.setLayout(l);
		
		createJarLabel(root);
		
		fBlockControl = createContents(root);

		setTitle(DebugUIUtils.getResourceString(PAGE_TITLE));
		setDescription(DebugUIUtils.getResourceString(PAGE_DESCRIPTION));
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAVA_LAUNCH);
		
		// spacer
		new Label(root, SWT.NONE);
		
		fNoSourceButton= new Button(root, SWT.CHECK);
		fNoSourceButton.setText(DebugUIUtils.getResourceString(NO_SOURCE));
		fNoSourceButton.addSelectionListener(
			new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					updateButtons();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					updateButtons();
				}
				
			}
		);
		
		setControl(root);
	}
	
	protected Control createContents(Composite composite) {
		try {
			IPath path= fJarRoot.getSourceAttachmentPath();
			IPath prefix= fJarRoot.getSourceAttachmentRootPath();
			URL jdocLocation= JavaDocAccess.getJavaDocLocation(fJarRoot);				
			IProject proj= fJarRoot.getJavaProject().getProject();		
			fSourceAttachmentBlock= new SourceAttachmentBlock(proj, this, fJarRoot.getPath(), path, prefix, jdocLocation);
			return fSourceAttachmentBlock.createControl(composite);				
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "", e.getStatus());
			return null;
		}	
	}
	
	protected void createJarLabel(Composite parent) {
		Composite root= new Composite(parent, SWT.NONE);
		GridLayout l= new GridLayout();
		l.numColumns= 2;
		root.setLayout(l);

		JavaElementLabelProvider lp = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);
		Label label1 = new Label(root, SWT.NONE);
		label1.setImage(lp.getImage(fJarRoot));
		Label label2 = new Label(root, SWT.NONE);
		label2.setText(((IJavaElement)fJarRoot).getElementName());

	}
	
	/**
	 * The status has changed.  Update the state
	 * of the wizard buttons and the widgets that
	 * are part of this page.
	 */
	public void updateButtons() {
		if (getControl() != null) {
			fNoSource = fNoSourceButton.getSelection();
			fBlockControl.setEnabled(!fNoSource);
			if (fBlockControl instanceof Composite) {
				Control[] children= ((Composite)fBlockControl).getChildren();
				for (int i= 0; i < children.length; i++) {
					children[i].setEnabled(!fNoSource);
				}
			}
			getWizard().getContainer().updateButtons();
		}
	}
	
	public void statusChanged(IStatus status) {
		StatusTool.applyToStatusLine(this, status);
		updateButtons();
	}
	
	public boolean isNoSource() {
		return fNoSource;
	}
	
	/**
	 * Set the source attachment for the jar
	 */
	public boolean performFinish() {
		if (fSourceAttachmentBlock != null) {
			try {
				if (!JavaModelUtility.isOnBuildPath(fJarRoot)) {
					if (fSourceAttachmentBlock.getSourceAttachmentPath() == null) {
						return true;
					} else if (!putJarOnClasspath()) {
						// ignore changes and return
						return true;
					}
				}
			} catch (JavaModelException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				return true;	
			}
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						IPath newPath= fSourceAttachmentBlock.getSourceAttachmentPath();
						IPath newRoot= fSourceAttachmentBlock.getSourceAttachmentRootPath();							
						fJarRoot.attachSource(newPath, newRoot, monitor);
						URL jdocLocation= fSourceAttachmentBlock.getJavaDocLocation();
						JavaDocAccess.setJavaDocLocation(fJarRoot, jdocLocation);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (InvocationTargetException e) {
				if (!ExceptionHandler.handle(e.getTargetException(), getShell(), DebugUIUtils.getResourceBundle(), ERROR_PREFIX)) {
					MessageDialog.openError(getShell(), "Error", e.getMessage());
				}
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	
	private boolean putJarOnClasspath() {
		String title= DebugUIUtils.getResourceString(ADD_TO_BUILD_PATH + ".title");
		String message= DebugUIUtils.getResourceString(ADD_TO_BUILD_PATH + ".message");
		MessageDialog dialog= new MessageDialog(getShell(), title, null, message, SWT.ICON_QUESTION,
	 			new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL } , 0
	 	);
	 	if (dialog.open() != dialog.OK) {
	 		return false;
	 	}
		IJavaProject jproject= fJarRoot.getJavaProject();
		try {
			IClasspathEntry[] entries= jproject.getRawClasspath();
			IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length]= JavaCore.newLibraryEntry(fJarRoot.getPath());
			jproject.setRawClasspath(newEntries, null);
			return true;
		} catch (JavaModelException e) {
			ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
		}
		return false;
	}	

}
