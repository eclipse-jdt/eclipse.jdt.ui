/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.SashForm;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.compare.CompareConfiguration;import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jface.viewers.CheckStateChangedEvent;import org.eclipse.jface.viewers.CheckboxTreeViewer;import org.eclipse.jface.viewers.ICheckStateListener;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.ITreeContentProvider;import org.eclipse.jface.viewers.ITreeViewerListener;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TreeExpansionEvent;import org.eclipse.jface.viewers.Viewer;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.corext.refactoring.base.Change;import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.corext.refactoring.base.IChange;import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;import org.eclipse.jdt.internal.corext.refactoring.text.AbstractTextBufferChange;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.compare.JavaMergeViewer;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
/**
 * Presents the changes made by the refactoring.
 * Consists of a tree of changes and a compare viewer that shows the differences.
 * @deprecated Use NewPreviewWizardPage 
 */
public class PreviewWizardPage extends RefactoringWizardPage implements IPreviewWizardPage {

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
		public Object getModifiedLanguageElement() {
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
			Object me= change.getModifiedLanguageElement();
			if (me == null)
				return null;
			return super.getImage(me);
		}
		
		public final String getText(Object element) {
			StringBuffer result= new StringBuffer();
			IChange change= (IChange)element;
			Object me= change.getModifiedLanguageElement();
			if (me instanceof IJavaElement) {
				result.append(super.getText(me));
			}
			String name= change.getName();
			if (name != null) {
				result.append(" : "); //$NON-NLS-1$
				result.append(name);
			}
			return result.toString();
		}
	}
	
	
	private IChange fChange;		
	private CompareViewerSwitchingPane fSwitchingPane;
	private CheckboxTreeViewer fTreeViewer;
	private boolean fExpandFirstNode;
	
	/**
	 * Creates a new proposed changes wizard page.
	 */
	public PreviewWizardPage() {
		super(PAGE_NAME);
		setDescription(RefactoringMessages.getString("PreviewWizardPage.description")); //$NON-NLS-1$
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
		IChange treeViewerInput;
		
		if (fChange == null) {
			treeViewerInput= null;
		} else if (fChange instanceof ICompositeChange && !(fChange instanceof AbstractTextBufferChange)) {
			treeViewerInput= fChange;
		} else {
			treeViewerInput= new DummyRootNode(fChange);
		}
		if (fTreeViewer != null)
			fTreeViewer.setInput(treeViewerInput);
			
		if (treeViewerInput != null)
			checkAllActiveNodes(treeViewerInput);
	}
	
	/**
	 * Defines whether the frist node in the preview page is supposed to be expanded.
	 * 
	 * @param expand <code>true</code> if the first node is to be expanded. Otherwise
	 *  <code>false</code>
	 */
	public void setExpandFirstNode(boolean expand) {
		fExpandFirstNode= expand;
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
		SashForm sashForm= new SashForm(parent, SWT.VERTICAL);
		
		createTreeViewer(sashForm);
		createCompareViewer(sashForm);
		sashForm.setWeights(new int[]{33, 67});
		
		setControl(sashForm);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.REFACTORING_PREVIEW_WIZARD_PAGE));
	}
	
	/* (Non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void setVisible(boolean visible){
		IChange treeViewerInput= (IChange)fTreeViewer.getInput();
		if (visible && treeViewerInput != null) {
			IStructuredSelection selection= (IStructuredSelection)fTreeViewer.getSelection();
			if (selection.isEmpty()) {
				ITreeContentProvider provider= (ITreeContentProvider)fTreeViewer.getContentProvider();
				Object[] elements= provider.getElements(treeViewerInput);
				if (elements != null && elements.length > 0) {
					Object element= elements[0];
					if (fExpandFirstNode) {
						Object[] subElements= provider.getElements(element);
						if (subElements != null && subElements.length > 0) {
							fTreeViewer.expandToLevel(element, 1);
							checkAllActiveNodes((IChange)element);
							element= subElements[0];
						}
					}
					fTreeViewer.setSelection(new StructuredSelection(element));
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
		//XXX: should not go all the way to the bottom of the tree
		fTreeViewer.setCheckedElements(getActiveNodes(element).toArray());
	}
	
	private void createCompareViewer(final Composite parent){
		fSwitchingPane= new CompareViewerSwitchingPane(parent, SWT.BORDER, true){
			protected Viewer getViewer(Viewer oldViewer, Object input){
				JavaMergeViewer mergeViewer= new JavaMergeViewer(oldViewer.getControl().getParent(), SWT.NONE, new CompareConfiguration());
				mergeViewer.setContentProvider(new MergeTextViewerContentProvider());
				mergeViewer.setInput(new Object());
				return mergeViewer;
			}
		};
	}
	
	private void createTreeViewer(Composite parent){
		fTreeViewer= new CheckboxTreeViewer(parent);
		fTreeViewer.setContentProvider(createTreeContentProvider());
		fTreeViewer.setLabelProvider(createTreeLabelProvider());
		fTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		fTreeViewer.addCheckStateListener(createCheckStateListener());
		fTreeViewer.addTreeListener(createTreeViewerListener());
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
				if (selection.size() == 1) 
					setViewerInput(selection.getFirstElement());
			}
		};
	}
		
	private ISelectionChangedListener createSelectionChangedListener(){
		return new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				if (sel.size() == 1) {
					Object currentInput= fSwitchingPane.getInput();
					Object newInput= sel.getFirstElement();
					if (currentInput != newInput)
						setViewerInput(newInput);
				} else {
					setViewerInput(new Object());
				}
			}
		};
	}	
	
	private void setViewerInput(Object input){
		fSwitchingPane.setInput(input);
	}
}