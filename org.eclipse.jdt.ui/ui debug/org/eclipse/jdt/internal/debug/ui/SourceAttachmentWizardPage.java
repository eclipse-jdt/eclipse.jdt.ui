/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.debug.ui;
import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.IStatusChangeListener;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtil;import org.eclipse.jdt.internal.ui.wizards.NewElementWizardPage;import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

/**
 * A wizard page to attach source at debug time.
 */
public class SourceAttachmentWizardPage extends NewElementWizardPage {
	
	private final static String PREFIX = "source_attachment_wizard_page.";
	private final static String PAGE_TITLE = PREFIX + "title";
	private final static String PAGE_DESCRIPTION = PREFIX + "description";
	private final static String NO_SOURCE = PREFIX + "no_source";
	private static final String ERROR_PREFIX= PREFIX + "op_error.";	
	private static final String ERROR_TITLE= ERROR_PREFIX + "title";	private static final String ERROR_MESSAGE= ERROR_PREFIX + "message";
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
			IClasspathEntry entry= JavaModelUtil.getRawClasspathEntry(fJarRoot);			if (entry == null) {				// use a dummy entry to use for initialization				entry= JavaCore.newLibraryEntry(fJarRoot.getPath(), null, null);			}			IStatusChangeListener listener= new IStatusChangeListener() {				public void statusChanged(IStatus status) {					updateStatus(status);					updateButtons();				}			};						IWorkspaceRoot root= fJarRoot.getJavaModel().getWorkspace().getRoot();
			fSourceAttachmentBlock= new SourceAttachmentBlock(root, listener, entry);
			return fSourceAttachmentBlock.createControl(composite);				
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), DebugUIUtils.getResourceString(ERROR_TITLE), "", e.getStatus());
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
	public boolean isNoSource() {
		return fNoSource;
	}
	
	/**
	 * Sets the source attachment for the jar.
	 */
	public boolean performFinish() {
		if (fSourceAttachmentBlock != null) {			try {				IRunnableWithProgress runnable= fSourceAttachmentBlock.getRunnable(fJarRoot.getJavaProject(), getShell());						new ProgressMonitorDialog(getShell()).run(true, true, runnable);									} catch (InvocationTargetException e) {				String title= DebugUIUtils.getResourceString(ERROR_TITLE);				String message= DebugUIUtils.getResourceString(ERROR_MESSAGE);				if (!ExceptionHandler.handle(e, getShell(), title, message)) {					MessageDialog.openError(getShell(), title, message);				}				return false;			} catch (InterruptedException e) {				// cancelled				return false;			}		}		return true;
	}

}
