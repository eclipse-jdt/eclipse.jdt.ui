package org.eclipse.jdt.internal.ui.refactoring;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.changes.ReorgRefactoring;
import org.eclipse.jdt.internal.ui.packageview.PackageFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

abstract class ReorgInputPage extends UserInputWizardPage {

	private TreeViewer fViewer;
	private Boolean fIsTreeEmpty;
	
	ReorgInputPage(String pageName){
		super(pageName, false);
	}
	
	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		fViewer= new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setControl(fViewer.getControl());
		
		fViewer.setLabelProvider(createLabelProvider());
		fViewer.setContentProvider(createContentProvider());
		fViewer.setSorter(new PackageViewerSorter());
		fViewer.addFilter(new ContainerFilter(getReorgRefactoring()));
		fViewer.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected= getSelectedElement(event.getSelection());
				updateOKStatus(selected);
				updateMessage(selected);
			}	
		});
	}
	
	private ReorgInputPage2 getNextReorgPage(){	
		return ((ReorgInputPage2)getRefactoringWizard().getPage(ReorgInputPage2.PAGE_NAME));	
	}	
	
	public IWizardPage getNextPage() {
		try{
			initializeRefactoring();
			ReorgInputPage2 next= getNextReorgPage();
			if (next.hasAnyInput())
				return super.getNextPage();
			else
				return next.getNextPage();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Reorg", "Internal Error. See log for details.");
			return this;
		}	
	}
	
	public boolean performFinish(){
		try{
			initializeRefactoring();
			return super.performFinish();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Reorg", "Internal Error. See log for details.");
			return false;
		}	
	}
	
	private List getElementsToReorg(){
		return getReorgRefactoring().getElementsToReorg();
	}
	
	abstract void updateMessage(Object selectedElement);
	abstract String getValidationMessage(Object element);
	
	private void initializeRefactoring() throws JavaModelException{
		getReorgRefactoring().setDestination(getSelectedElement());
	}
	
	private ReorgRefactoring getReorgRefactoring(){
		return (ReorgRefactoring)getRefactoring();
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public boolean canFlipToNextPage() {
		//XXX 
		
		// we can't call getNextPage to determine if flipping is allowed since computing
		// the next page is quite expensive (checking preconditions and creating a
		// change). So we say yes if the page is complete.
		return isPageComplete();
	}
	
	/**
	 * @see DialogPane#dispose()
	 */
	public void dispose() {
		super.dispose();
		fViewer= null;
	}
	
	private void updateOKStatus(Object element){
		if (!isTreeEmpty())
			updateStatus(getValidationMessage(element));
		else 
			updateStatus("Empty List");
	}
	
	private void updateStatus(String msg){
		setErrorMessage(msg);
		setPageComplete(msg == null);	
	}
	
	private static JavaElementContentProvider createContentProvider(){
		return new JavaElementContentProvider() {
			public boolean hasChildren(Object element) {
				// prevent the + from being shown in front of packages
				return !(element instanceof IPackageFragment) && super.hasChildren(element);
			}
		};
	}
	
	private static JavaElementLabelProvider createLabelProvider(){
		return new DestinationRenderer(JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}
	
	private boolean isTreeEmpty() {
		if (fIsTreeEmpty == null)
			fIsTreeEmpty= checkIfTreeIsEmpty();
		return fIsTreeEmpty.booleanValue();
	}
	
	private Boolean checkIfTreeIsEmpty() {	
		Object[] elements= ((ITreeContentProvider)fViewer.getContentProvider()).getElements(fViewer.getInput());
		if (elements.length == 0) 
			return Boolean.TRUE;
		
		ViewerFilter[] filters= fViewer.getFilters();
		if (filters == null) 
			return Boolean.FALSE;
			
		for (int i= 0; i < filters.length; i++) {
			elements= filters[i].filter(fViewer, fViewer.getInput(), elements);
		}
		return new Boolean(elements.length == 0);
	}
	
	private static Object getSelectedElement(ISelection selection){
		return getFirstElement(SelectionUtil.toList(selection));
	}
	
	private Object getSelectedElement(){
		return getSelectedElement(fViewer.getSelection());
	}
	
	private static Object getFirstElement(List list){
		if (list.isEmpty())
			return null;
		else
			return list.get(0);	
	}
	
	private static class DestinationRenderer extends JavaElementLabelProvider {

		public DestinationRenderer(int flags) {
			super(flags);
		}

		public String getText(Object element) {
			try {
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot)element;
					if (root.getUnderlyingResource() instanceof IProject)
						return "packages";
				}
			} catch (JavaModelException e) {
			}
			return super.getText(element);
		}
	}
	
	private static class ContainerFilter extends PackageFilter {
		private ReorgRefactoring fRefactoring;
	
		public ContainerFilter(ReorgRefactoring refactoring) {
			fRefactoring= refactoring;
		}

		public boolean select(Viewer viewer, Object parent, Object o) {
			List elements= fRefactoring.getElementsToReorg();
			if (elements.contains(o))
				return false;
			return fRefactoring.canBeAncestor(o);	
		}
	}
}

