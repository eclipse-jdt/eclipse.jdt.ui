package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.wizard.IWizardPage;
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
	
	private ClasspathContainerSelectionPage fSelectionWizardPage;

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

	/**
	 * @deprecated
	 */	
	public ClasspathContainerWizard(ClasspathContainerDescriptor pageDesc) {
		this(null, pageDesc, null, null);	
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
		if (fPageDesc != null) {
			fContainerPage= getContainerPage(fPageDesc);
			addPage(fContainerPage);			
		} else if (fEntryToEdit == null) { // new entry: show selection page as first page
			ClasspathContainerDescriptor[] containers= ClasspathContainerDescriptor.getDescriptors();

			fSelectionWizardPage= new ClasspathContainerSelectionPage(containers);
			addPage(fSelectionWizardPage);

			// add as dummy, will not be shown
			fContainerPage= new ClasspathContainerDefaultPage();
			addPage(fContainerPage);
		} else { // fPageDesc == null && fEntryToEdit != null
			ClasspathContainerDescriptor[] containers= ClasspathContainerDescriptor.getDescriptors();
			ClasspathContainerDescriptor descriptor= findDescriptorPage(containers, fEntryToEdit);
			fContainerPage= getContainerPage(descriptor);
			addPage(fContainerPage);				
		}
		super.addPages();
	}
	
	private IClasspathContainerPage getContainerPage(ClasspathContainerDescriptor pageDesc) {
		IClasspathContainerPage containerPage= null;
		if (pageDesc != null) {
			try {
				containerPage= pageDesc.createPage();
			} catch (CoreException e) {
				handlePageCreationFailed(e);
				return null;
			}
		}

		if (containerPage == null)	{
			containerPage= new ClasspathContainerDefaultPage();
		}

		if (containerPage instanceof IClasspathContainerPageExtension) {
			((IClasspathContainerPageExtension) containerPage).initialize(fCurrProject, fCurrClasspath);
		}

		containerPage.setSelection(fEntryToEdit);
		containerPage.setWizard(this);
		return containerPage;
	}
	
	/* (non-Javadoc)
	 * @see IWizard#getNextPage(IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == fSelectionWizardPage) {

			ClasspathContainerDescriptor selected= fSelectionWizardPage.getSelected();
			fContainerPage= getContainerPage(selected);
			return fContainerPage;
		}
		return super.getNextPage(page);
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
	

	/* (non-Javadoc)
	 * @see IWizard#canFinish()
	 */
	public boolean canFinish() {
		if (fSelectionWizardPage != null) {
			if (!fContainerPage.isPageComplete()) {
				return false;
			}
		}
		if (fContainerPage != null) {
			return fContainerPage.isPageComplete();
		}
		return false;
	}
	
	
}
