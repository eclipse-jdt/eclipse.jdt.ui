package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;


/**
  */
public class ClasspathContainerWizard extends Wizard {
	
	private IClasspathEntry fEntryToEdit;
	
	private ClasspathContainerSelectionPage fSelectionWizardPage;
	private IClasspathContainerPage fContainerPage;

	/**
	 * Constructor for ClasspathContainerWizard.
	 */
	public ClasspathContainerWizard(IClasspathEntry entryToEdit) {
		super();
		fEntryToEdit= entryToEdit;
	}
	
	public IClasspathEntry getNewEntry() {
		if (fContainerPage != null) {
			return fContainerPage.getSelection();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {
		if (fContainerPage != null) {
			return fContainerPage.finish();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see IWizard#addPages()
	 */
	public void addPages() {
		if (fEntryToEdit == null) {
			ClasspathContainerDescriptor[] containers= ClasspathContainerDescriptor.getDescriptors();
			fSelectionWizardPage= new ClasspathContainerSelectionPage(containers);
			addPage(fSelectionWizardPage);
			
			// add as dummy, will not be shown
			fContainerPage= new ClasspathContainerDefaultPage();
			addPage(fContainerPage); 
		} else {
			IClasspathContainerPage containerPage= null;
			ClasspathContainerDescriptor desc= findDescriptorPage(fEntryToEdit);
			if (desc != null) {
				try {
					containerPage= desc.createPage();
				} catch (CoreException e) {
					handlePageCreationFailed(e);
				}
			}
			if (containerPage == null)	{
				containerPage= new ClasspathContainerDefaultPage();
			}
			
			containerPage.setSelection(fEntryToEdit);
			fContainerPage= containerPage;
			fSelectionWizardPage= null;
			
			addPage(containerPage);
		}
		super.addPages();
	}
	
	private void handlePageCreationFailed(CoreException e) {
		String title= NewWizardMessages.getString("ClasspathContainerWizard.pagecreationerror.title"); //$NON-NLS-1$
		String message= NewWizardMessages.getString("ClasspathContainerWizard.pagecreationerror.message"); //$NON-NLS-1$
		ExceptionHandler.handle(e, getShell(), title, message);
	}
	
	
	private ClasspathContainerDescriptor findDescriptorPage(IClasspathEntry entry) {
		ClasspathContainerDescriptor[] containers= ClasspathContainerDescriptor.getDescriptors();
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].canEdit(fEntryToEdit)) {
				return containers[i];
			}
		}
		return null;
	}			
	
	
		/* (non-Javadoc)
	 * @see IWizard#getNextPage(IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == fSelectionWizardPage) {
			
			ClasspathContainerDescriptor selected= fSelectionWizardPage.getSelected();
			if (selected != null) {
				try {
					fContainerPage= selected.createPage();
					fContainerPage.setSelection(null);
					fContainerPage.setWizard(this);
					
					return fContainerPage;
				} catch (CoreException e) {
					handlePageCreationFailed(e);
				}
				fContainerPage= null;
			}	
		}
		return super.getNextPage(page);
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
