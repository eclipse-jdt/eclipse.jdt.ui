package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
  */
public class ClasspathContainerSelectionPage extends WizardPage {
	
	private static class ClasspathContainerLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return ((ClasspathContainerDescriptor) element).getName();
		}
	}
	
	private ListViewer fListViewer;
	private ClasspathContainerDescriptor[] fContainers;

	/**
	 * Constructor for ClasspathContainerWizardPage.
	 * @param pageName
	 */
	protected ClasspathContainerSelectionPage(ClasspathContainerDescriptor[] containerPages) {
		super("ClasspathContainerWizardPage"); //$NON-NLS-1$
		setTitle(NewWizardMessages.getString("ClasspathContainerSelectionPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("ClasspathContainerSelectionPage.description")); //$NON-NLS-1$
		
		fContainers= containerPages;
		
		validatePage();
	}

	/* (non-Javadoc)
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		fListViewer= new ListViewer(parent, SWT.SINGLE);
		fListViewer.setLabelProvider(new ClasspathContainerLabelProvider());
		fListViewer.setContentProvider(new ListContentProvider());
		fListViewer.setInput(Arrays.asList(fContainers));
		fListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validatePage();
			}
		});
		if (fContainers.length > 0) {
			fListViewer.setSelection(new StructuredSelection(fContainers[0]));
		}
		setControl(fListViewer.getList());
	}

	/**
	 * Method validatePage.
	 */
	private void validatePage() {
		setPageComplete(getSelected() != null);
	}
	
	
	public ClasspathContainerDescriptor getSelected() {
		if (fListViewer != null) {
			ISelection selection= fListViewer.getSelection();
			return (ClasspathContainerDescriptor) SelectionUtil.getSingleElement(selection);
		}
		return null;
	}
	

	/* (non-Javadoc)
	 * @see IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete(); // avoid the getNextPage call to prevent potential plugin load
	}

}
