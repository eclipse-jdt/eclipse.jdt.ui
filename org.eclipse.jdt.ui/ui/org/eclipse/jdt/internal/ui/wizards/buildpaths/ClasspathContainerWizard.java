package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


/**
  */
public class ClasspathContainerWizard extends Wizard {

	private ClasspathContainerDescriptor fPageDesc;
	private IClasspathEntry fEntryToEdit;

	private IClasspathEntry fNewEntry;
	private IClasspathContainerPage fContainerPage;
	private IJavaProject fCurrProject;
	private IClasspathEntry[] fCurrClasspath;

	/**
	 * Constructor for ClasspathContainerWizard.
	 */
	public ClasspathContainerWizard(IClasspathEntry entryToEdit, IJavaProject currProject, IClasspathEntry[] currEntries) {
		this(entryToEdit, null, currProject, currEntries);
	}
	
	/**
	 * Constructor for ClasspathContainerWizard.
	 */
	public ClasspathContainerWizard(ClasspathContainerDescriptor pageDesc, IJavaProject currProject, IClasspathEntry[] currEntries) {
		this(null, pageDesc, currProject, currEntries);	
	}
	
	private ClasspathContainerWizard(IClasspathEntry entryToEdit, ClasspathContainerDescriptor pageDesc, IJavaProject currProject, IClasspathEntry[] currEntries) {
		fEntryToEdit= entryToEdit;
		fPageDesc= pageDesc;
		fNewEntry= null;
		
		fCurrProject= currProject;
		fCurrClasspath= currEntries;			
	}
	
	
	public IClasspathEntry getNewEntry() {
		return fNewEntry;
	}

	/* (non-Javadoc)
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {
		if (fContainerPage != null) {
			if (fContainerPage.finish()) {
				fNewEntry= fContainerPage.getSelection();
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see IWizard#addPages()
	 */
	public void addPages() {

		if (fPageDesc == null && fEntryToEdit != null) {
			ClasspathContainerDescriptor[] containers= ClasspathContainerDescriptor.getDescriptors();

			fPageDesc= findDescriptorPage(containers, fEntryToEdit);
		}

		IClasspathContainerPage containerPage= null;
		if (fPageDesc != null) {
			try {
				containerPage= fPageDesc.createPage();
			} catch (CoreException e) {
				handlePageCreationFailed(e);
			}
		}

		if (containerPage == null)	{
			containerPage= new ClasspathContainerDefaultPage();
		}
		
		if (containerPage instanceof IClasspathContainerPageExtension) {
			((IClasspathContainerPageExtension) containerPage).initialize(fCurrProject, fCurrClasspath);
		}
		
		containerPage.setSelection(fEntryToEdit);
		fContainerPage= containerPage;

		addPage(containerPage);

		super.addPages();
	}
	
	private void handlePageCreationFailed(CoreException e) {
		String title= NewWizardMessages.getString("ClasspathContainerWizard.pagecreationerror.title"); //$NON-NLS-1$
		String message= NewWizardMessages.getString("ClasspathContainerWizard.pagecreationerror.message"); //$NON-NLS-1$
		ExceptionHandler.handle(e, getShell(), title, message);
	}
	
	
	private ClasspathContainerDescriptor findDescriptorPage(ClasspathContainerDescriptor[] containers, IClasspathEntry entry) {
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].canEdit(entry)) {
				return containers[i];
			}
		}
		return null;
	}			
	
}
