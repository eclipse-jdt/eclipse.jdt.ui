/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.SashForm;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.jface.viewers.CheckStateChangedEvent;import org.eclipse.jface.viewers.CheckboxTreeViewer;import org.eclipse.jface.viewers.ICheckStateListener;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.ITreeViewerListener;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TreeExpansionEvent;import org.eclipse.jface.viewers.Viewer;import org.eclipse.compare.CompareConfiguration;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.refactoring.Change;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.ICompositeChange;import org.eclipse.jdt.core.refactoring.text.AbstractTextBufferChange;import org.eclipse.jdt.internal.ui.compare.JavaMergeViewer;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * Presents the changes made by the refactoring.
 * Consists of a tree of changes and a compare viewer that shows the differences. 
 */
public class PreviewWizardPage extends RefactoringWizardPage {

	// Dummy root node if input element isn't a composite change.
	private static class DummyRootNode extends Change implements ICompositeChange {
		private IChange[] fChildren;
		
		public DummyRootNode(IChange change) {
			fChildren= new IChange[] { change };
		}
		public IChange[] getChildren() {
			return fChildren;
		}
		public String getName() {
			return null;
		}
		public IJavaElement getCorrespondingJavaElement() {
			return null;
		}
		public IChange getUndoChange() {
			return null;
		}
		public void perform(ChangeContext context, IProgressMonitor pm) {
		}
	}

	// Content provider for the tree viewer.
	private static class ChangeTreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object element){
			if (element instanceof ICompositeChange)
				return ((ICompositeChange)element).getChildren();
			return null;	
		}
		public Object getParent(Object element){
			return null;
		}
		public boolean hasChildren(Object element){
			Object[] children= getChildren(element);
			return children != null && children.length > 0;
		}
		public void dispose(){
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
		}
		public boolean isDeleted(Object element){
			return false;
		}
		public Object[] getElements(Object element){
			return getChildren(element);
		}
	}
	
	// Label provider for the tree viewer.
	private static class ChangeTreeLabelProvider extends JavaElementLabelProvider {
	
		public ChangeTreeLabelProvider() {
			super(JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_SMALL_ICONS);
		}
			
		public final Image getImage(Object element) {
			IChange change= (IChange)element;
			IJavaElement je= change.getCorrespondingJavaElement();
			if (je == null)
				return null;
			return super.getImage(je);
		}
		
		public final String getText(Object element) {
			StringBuffer result= new StringBuffer();
			IChange change= (IChange)element;
			IJavaElement je= change.getCorrespondingJavaElement();
			if (je != null) {
				result.append(super.getText(je));
			}
			String name= change.getName();
			if (name != null) {
				result.append(" - ");
				result.append(name);
			}
			return result.toString();
		}
	}
	
	
	private IChange fChange;
	private IChange fTreeViewerInput;
		
	private SashForm fSashForm;
	private JavaMergeViewer fMergeViewer;
	private CheckboxTreeViewer fTreeViewer;
	
	public static final String PAGE_NAME= "PreviewPage";

	/**
	 * Creates a new proposed changes wizard page.
	 */
	public PreviewWizardPage() {
		super(PAGE_NAME);
		setDescription(RefactoringResources.getResourceString(getName() + ".description"));
	}

	/**
	 * Sets the given change. Setting the change initializes the tree viewer with
	 * the given change.
	 * @param change the new change.
	 */
	public void setChange(IChange change){
		if (fChange == change)
			return;
		
		fChange= change;	
		if (fChange == null) {
			fTreeViewerInput= null;
		} else if (fChange instanceof ICompositeChange && !(fChange instanceof AbstractTextBufferChange)) {
			fTreeViewerInput= fChange;
		} else {
			fTreeViewerInput= new DummyRootNode(fChange);
		}
		if (fTreeViewer != null)
			fTreeViewer.setInput(fTreeViewerInput);
			
		if (fTreeViewerInput != null)
			checkAllActiveNodes(fTreeViewerInput);
	}
	
	/**
	 * Creates the content provider used to fill the tree of changes. Subclasses may override
	 * to create their own custom tree content provider.
	 *
	 * @return the tree content provider used to fill the tree of changes. Must not return <code>
	 *  null</code>.
	 */
	protected ITreeContentProvider createTreeContentProvider() {
		return new ChangeTreeContentProvider();
	}
	
	/**
	 * Creates the label provider used to render the tree of changes. Subclasses may override
	 * to create their own custom label provider.
	 *
	 * @return the label provider used to render the tree of changes. Must not return <code>
	 *  null</code>.
	 */
	protected ILabelProvider createTreeLabelProvider() {
		return new ChangeTreeLabelProvider();
	}
	
	/* (non-JavaDoc)
	 * Method defined in RefactoringWizardPage
	 */
	protected boolean performFinish() {
		return getRefactoringWizard().performFinish(new PerformChangeOperation(fChange));
	} 
	
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public boolean canFlipToNextPage() {
		return false;
	}
	
	/* (Non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void createControl(Composite parent) {
		fSashForm= new SashForm(parent, SWT.VERTICAL);
		
		createTreeViewer(fSashForm);
		
		//Control compare= 
		createCompareViewer(fSashForm);
		//compare.setLayoutData(new GridData(GridData.FILL_BOTH));
		fSashForm.setWeights(new int[]{33, 67});
		
		setControl(fSashForm);
	}
	
	/* (Non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void setVisible(boolean visible){
		if (visible && fTreeViewerInput != null) {
			IStructuredSelection selection= (IStructuredSelection)fTreeViewer.getSelection();
			if (selection.isEmpty()) {
				ITreeContentProvider provider= (ITreeContentProvider)fTreeViewer.getContentProvider();
				Object[] elements= provider.getElements(fTreeViewerInput);
				if (elements != null && elements.length > 0) {
					fTreeViewer.setSelection(new StructuredSelection(elements[0]));
				}
			}
		}
		super.setVisible(visible);
	}
	
	
	private List getActiveNodes(IChange root){
		if (!root.isActive()) //assumes that no subelements can be active here
			return new ArrayList(0);
			
		List active= null;
		if (root instanceof ICompositeChange) {
			IChange[] children= ((ICompositeChange)root).getChildren();
			active= new ArrayList(children.length + 1);
			active.add(root);
			for (int i= 0; i < children.length; i++) {
				active.addAll(getActiveNodes(children[i]));
			}
		} else {
			active= new ArrayList(1);
			active.add(root);
		}
		return active;
	}
	
	private void checkAllActiveNodes(IChange element){
		JdtHackFinder.fixMeSoon("should not go all the way to the bottom of the tree");
		fTreeViewer.setCheckedElements(getActiveNodes(element).toArray());
	}
	
	private Control createCompareViewer(Composite parent){
		fMergeViewer= new JavaMergeViewer(parent, SWT.BORDER, new CompareConfiguration());
		fMergeViewer.setContentProvider(new MergeTextViewerContentProvider());
		fMergeViewer.setInput(new Object());
			
		Control control= fMergeViewer.getControl();		
		return control;
	}
	
	private Control createTreeViewer(Composite parent){
		fTreeViewer= new CheckboxTreeViewer(parent);
		fTreeViewer.setContentProvider(createTreeContentProvider());
		fTreeViewer.setLabelProvider(createTreeLabelProvider());
		fTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		fTreeViewer.addCheckStateListener(createCheckStateListener());
		fTreeViewer.addTreeListener(createTreeViewerListener());
		return fTreeViewer.getControl();
	}
	
	private ITreeViewerListener createTreeViewerListener(){
		return new ITreeViewerListener(){
			public void treeCollapsed(TreeExpansionEvent event){	
			}
			
			public void treeExpanded(TreeExpansionEvent event){
				IChange change= (IChange)event.getElement();
				if (change instanceof ICompositeChange) {
					IChange[] children= ((ICompositeChange)change).getChildren();
					if (children != null) {
						for (int i= 0; i < children.length; i++) {
							IChange child= children[i];
							fTreeViewer.setChecked(child, child.isActive());
						}
					}
				}
			}
		};
	}
	
	private ICheckStateListener createCheckStateListener(){
		return new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event){
				IChange change= (IChange)event.getElement();
				change.setActive(event.getChecked());
				fTreeViewer.setSubtreeChecked(change, event.getChecked());
				IStructuredSelection selection= (IStructuredSelection)fTreeViewer.getSelection();
				if (selection.size() == 1) {
					Object input= selection.getFirstElement();
					fMergeViewer.setInput(input);
				} 
			}
		};
	}
		
	private ISelectionChangedListener createSelectionChangedListener(){
		return new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				if (sel.size() == 1) {
					Object currentInput= fMergeViewer.getInput();
					Object newInput= sel.getFirstElement();
					if (currentInput != newInput)
						fMergeViewer.setInput(newInput);
				} else {
					fMergeViewer.setInput(new Object());	
				}
			}
		};
	}	
}