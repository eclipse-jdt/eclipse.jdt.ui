/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.debug.ui;
import java.lang.reflect.InvocationTargetException;import java.net.URL;import org.eclipse.core.resources.IProject;import org.eclipse.core.runtime.*;import org.eclipse.jdt.core.*;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.dialogs.*;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.wizard.WizardPage;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.*;

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
			IJavaProject jproject= fJarRoot.getJavaProject();
			IClasspathEntry[] entries= jproject.getRawClasspath();
			
			int index= findClasspathEntry(entries, fJarRoot.getPath());
			IClasspathEntry entry;
			if (index != -1) {
				entry= entries[index];
			} else {
				entry= JavaCore.newLibraryEntry(fJarRoot.getPath(), null, null);
			}
			fSourceAttachmentBlock= new SourceAttachmentBlock(jproject.getProject(), this, entry);
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
	
	private int findClasspathEntry(IClasspathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				curr= curr.getResolvedEntry();
			}
			if (curr.getEntryKind() == IClasspathEntry.CPE_LIBRARY && path.equals(curr.getPath())) {
				return i;
			}
		}
		return -1;
	}
			
	private boolean modifyClasspathEntry(IClasspathEntry[] entries, IPath attachPath, IPath attachRoot) {
		int index= findClasspathEntry(entries, fJarRoot.getPath());
		if (index != -1) {
			IClasspathEntry old= entries[index];
			if (old.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				entries[index]= JavaCore.newVariableEntry(old.getPath(), attachPath, attachRoot);
			} else {
				entries[index]= JavaCore.newLibraryEntry(old.getPath(), attachPath, attachRoot);
			}
			return true;
		}
		return false;
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
				IJavaProject jproject= fJarRoot.getJavaProject();
				
				IPath attachPath= fSourceAttachmentBlock.getSourceAttachmentPath();
				IPath attachRoot= fSourceAttachmentBlock.getSourceAttachmentRootPath();				
				
				IClasspathEntry[] entries= jproject.getRawClasspath();
				if (!modifyClasspathEntry(entries, attachPath, attachRoot)) {
					// root not found in classpath
					if (fSourceAttachmentBlock.getSourceAttachmentPath() == null) {
						return true;
					} else if (!putJarOnClasspath()) {
						// ignore changes and return
						return true;
					}
					// put new on class path
					int nEntries= entries.length;
					IClasspathEntry[] incrEntries= new IClasspathEntry[nEntries + 1];
					System.arraycopy(entries, 0, incrEntries, 0, nEntries);
					incrEntries[nEntries]= JavaCore.newLibraryEntry(fJarRoot.getPath(), attachPath, attachRoot);
					entries= incrEntries;
				}
				final IClasspathEntry[] newEntries= entries;
				
				IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException {
						try {
							IJavaProject jproject= fJarRoot.getJavaProject();
							jproject.setRawClasspath(newEntries, monitor);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						}
					}
				};				
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (JavaModelException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				return false;							
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				JavaPlugin.log(e);
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
	 	return (dialog.open() == dialog.OK);
	}	

}
