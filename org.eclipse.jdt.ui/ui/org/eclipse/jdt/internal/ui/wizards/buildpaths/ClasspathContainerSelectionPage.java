package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
  */
public class ClasspathContainerSelectionPage extends WizardPage {

	private static final String DIALOGSTORE_SECTION= "ClasspathContainerSelectionPage";
	private static final String DIALOGSTORE_CONTAINER_IDX= "index";


	private static class ClasspathContainerLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return ((ClasspathContainerDescriptor) element).getName();
		}
	}

	private static class ClasspathContainerSorter extends ViewerSorter {
	}

	private ListViewer fListViewer;
	private ClasspathContainerDescriptor[] fContainers;
	private IDialogSettings fDialogSettings;

	/**
	 * Constructor for ClasspathContainerWizardPage.
	 * @param pageName
	 */
	protected ClasspathContainerSelectionPage(ClasspathContainerDescriptor[] containerPages) {
		super("ClasspathContainerWizardPage"); //$NON-NLS-1$
		setTitle(NewWizardMessages.getString("ClasspathContainerSelectionPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("ClasspathContainerSelectionPage.description")); //$NON-NLS-1$

		fContainers= containerPages;

		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fDialogSettings= settings.getSection(DIALOGSTORE_SECTION);
		if (fDialogSettings == null) {
			fDialogSettings= fDialogSettings.addNewSection(DIALOGSTORE_SECTION);
			fDialogSettings.put(DIALOGSTORE_CONTAINER_IDX, 0);
		}
		validatePage();
	}

	/* (non-Javadoc)
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		fListViewer= new ListViewer(parent, SWT.SINGLE);
		fListViewer.setLabelProvider(new ClasspathContainerLabelProvider());
		fListViewer.setContentProvider(new ListContentProvider());
		fListViewer.setSorter(new ClasspathContainerSorter());
		fListViewer.setInput(Arrays.asList(fContainers));
		fListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validatePage();
			}
		});
		int selectionIndex= fDialogSettings.getInt(DIALOGSTORE_CONTAINER_IDX);
		if (selectionIndex >= fContainers.length) {
			selectionIndex= 0;
		}
		fListViewer.getList().select(selectionIndex);
		validatePage();
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (!visible && fListViewer != null) {
			fDialogSettings.put(DIALOGSTORE_CONTAINER_IDX, fListViewer.getList().getSelectionIndex());
		}
		super.setVisible(visible);
	}

}
