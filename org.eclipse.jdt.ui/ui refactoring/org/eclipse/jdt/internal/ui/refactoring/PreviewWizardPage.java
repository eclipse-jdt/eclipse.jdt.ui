/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.PageBook;

import org.eclipse.compare.CompareUI;

import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.ComparePreviewer.CompareInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ViewerPane;

/**
 * Presents the changes made by the refactoring.
 * Consists of a tree of changes and a compare viewer that shows the differences. 
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
	
	private static class NullPreviewer implements IPreviewViewer {
		private Label fLabel;
		public NullPreviewer(Composite parent) {
			fLabel= new Label(parent, SWT.CENTER | SWT.FLAT);
			fLabel.setText(RefactoringMessages.getString("PreviewWizardPage.no_preview")); //$NON-NLS-1$
		}
		public void setInput(Object input) {
			// do nothing
		}
		public void refresh() {
			// do nothing
		}
		public Control getControl() {
			return fLabel;
		}
	}
	
	private class NextChange extends Action {
		public NextChange() {
			setImageDescriptor(CompareUI.DESC_ETOOL_NEXT);
			setDisabledImageDescriptor(CompareUI.DESC_DTOOL_NEXT);
			setHoverImageDescriptor(CompareUI.DESC_CTOOL_NEXT);
			setToolTipText(RefactoringMessages.getString("PreviewWizardPage.next_Change")); //$NON-NLS-1$
		}
		public void run() {
			fTreeViewer.revealNext();	
		}
	}
	
	private class PreviousChange extends Action {
		public PreviousChange() {
			setImageDescriptor(CompareUI.DESC_ETOOL_PREV);
			setDisabledImageDescriptor(CompareUI.DESC_DTOOL_PREV);
			setHoverImageDescriptor(CompareUI.DESC_CTOOL_PREV);
			setToolTipText(RefactoringMessages.getString("PreviewWizardPage.previous_Change")); //$NON-NLS-1$
		}	
		public void run() {
			fTreeViewer.revealPrevious();
		}
	}
	
	private class ShowDetailsAction extends Action {
		private final String SHOW_QUALIFIED_NAMES= RefactoringMessages.getString("PreviewWizardPage.show_Qualified_Names"); //$NON-NLS-1$
		private final String HIDE_QUALIFIED_NAMES= RefactoringMessages.getString("PreviewWizardPage.hide_Qualified_Names"); //$NON-NLS-1$
		public ShowDetailsAction() {
			setImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE);
			setChecked(true);
			setToolTipText(HIDE_QUALIFIED_NAMES);
		}
		public void run() {
			boolean isChecked= isChecked();
			if (isChecked)
				setToolTipText(HIDE_QUALIFIED_NAMES);
			else
				setToolTipText(SHOW_QUALIFIED_NAMES);
			((ChangeElementLabelProvider)fTreeViewer.getLabelProvider()).setShowQualification(isChecked);
		}
		
	}
	
	private IChange fChange;		
	private ChangeElement fCurrentSelection;
	private ChangeElementTreeViewer fTreeViewer;
	private PageBook fPreviewContainer;
	private IPreviewViewer fCurrentPreviewViewer;
	private IPreviewViewer fNullPreviewer;
	private ComparePreviewer fComparePreview;
	
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
		setTreeViewerInput();
	}

	/**
	 * Creates the tree viewer to present the hierarchy of changes. Subclasses may override
	 * to create their own custom tree viewer.
	 * 
	 * @return the tree viewer to present the hierarchy of changes
	 */
	protected ChangeElementTreeViewer createTreeViewer(Composite parent) {
		return new ChangeElementTreeViewer(parent);
	}
	
	/**
	 * Creates the content provider used to fill the tree of changes. Subclasses may override
	 * to create their own custom tree content provider.
	 *
	 * @return the tree content provider used to fill the tree of changes
	 */
	protected ITreeContentProvider createTreeContentProvider() {
		return new ChangeElementContentProvider();
	}
	
	/**
	 * Creates the label provider used to render the tree of changes. Subclasses may override
	 * to create their own custom label provider.
	 *
	 * @return the label provider used to render the tree of changes
	 */
	protected ILabelProvider createTreeLabelProvider() {
		return new ChangeElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}
	
	/**
	 * Returns the <code>CompareInput</code> element, if the preview for the given 
	 * <code>ChangeElement</code> can be presented in a compare viewer. The method
	 * may return <code>null</code> indicating that the preview cannot be displayed using
	 * a compare viewer.
	 * <p>
	 * Subclasses may override to provide their own input element.
	 * 
	 * @return the compare input if the preview for the given change element can be
	 * 	presented using a compare viewer; otherwise <code>null</code>.
	 */
	protected CompareInput getCompareInput(ChangeElement element) {
		try {
			if (element instanceof DefaultChangeElement) {
				IChange change= ((DefaultChangeElement)element).getChange();
				if (change instanceof TextChange) {
					TextChange cuc= (TextChange)change;
					String type= ComparePreviewer.TEXT_TYPE;
					if (change instanceof CompilationUnitChange)
						type= ComparePreviewer.JAVA_TYPE;
					return createCompareInput(
						element,
						cuc.getCurrentContent(),
						cuc.getPreviewContent(),
						type);
				} else if (change instanceof CreateTextFileChange){
					CreateTextFileChange ctfc= (CreateTextFileChange)change;
					String type= ctfc.isJavaFile() ? ComparePreviewer.JAVA_TYPE: ComparePreviewer.TEXT_TYPE;
					return createCompareInput(
						element,
						ctfc.getCurrentContent(),
						ctfc.getPreview(),
						type);
				}
			} else if (element instanceof TextEditChangeElement) {
				EditChange tec= ((TextEditChangeElement)element).getTextEditChange();
				TextChange change= tec.getTextChange();
				if (change instanceof CompilationUnitChange) {
					ISourceReference sourceReference= findSourceReference(element);
					if (sourceReference != null) {
						CompilationUnitChange cuc= (CompilationUnitChange)change;
						return createCompareInput(
							element,
							cuc.getCurrentContent(sourceReference),
							cuc.getPreviewContent(sourceReference, new EditChange[] {tec}),
							ComparePreviewer.JAVA_TYPE);
					}
				}
				return createCompareInput(
					element,
					change.getCurrentContent(tec, 2),
					change.getPreviewContent(tec, 2),
					ComparePreviewer.TEXT_TYPE);
			} else if (element instanceof PseudoJavaChangeElement) {
				PseudoJavaChangeElement pjce= (PseudoJavaChangeElement)element;
				List l= collectTextEditChanges(pjce);
				if (l.size() > 0) {
					EditChange[] changes= (EditChange[]) l.toArray(new EditChange[l.size()]);
					CompilationUnitChange change= (CompilationUnitChange)changes[0].getTextChange();
					ISourceReference sourceReference= (ISourceReference)pjce.getJavaElement();
					return createCompareInput(
						element,
						change.getCurrentContent(sourceReference),
						change.getPreviewContent(sourceReference, changes),
						ComparePreviewer.JAVA_TYPE);
				}
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PreviewWizardPage.showing_preview"), RefactoringMessages.getString("PreviewWizardPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}
	
	private CompareInput createCompareInput(ChangeElement element, String left, String right, String type) {
		if (element == null || left == null || right == null || type == null)
			return null;
		return new CompareInput(element, left, right, type);
	}
	
	/**
	 * Returns a viewer used to show a preview for the given change element. The returned viewer
	 * is kept referenced until the wizard page gets disposed. So it is up to the implementor of this
	 * method to reuse existing viewers over different change elements. The method may return
	 * <code>nulll</code> indicating that no preview is available for the given change element.
	 * <p>
	 * Subclasses may override to provide their own preview.
	 * 
	 * @param element the change element for which a preview control is requested
	 * @param currentViewer the currently used preview viewer
	 * @param parent the parent to be used if a new preview viewer must be created
	 * @return the viewer to show a preview for the given change element
	 */
	protected IPreviewViewer getPreviewer(ChangeElement element, IPreviewViewer currentViewer, Composite parent) {
		CompareInput input= getCompareInput(element);
		if (input != null) {
			fComparePreview.setInput(input);
			return fComparePreview;
		}
		return null;
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
		initializeDialogUnits(parent);
		// XXX The composite is needed to limit the width of the SashForm. See http://bugs.eclipse.org/bugs/show_bug.cgi?id=6854
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0; layout.marginWidth= 0;
		result.setLayout(layout);
		
		
		SashForm sashForm= new SashForm(result, SWT.VERTICAL);

		ViewerPane pane= new ViewerPane(sashForm, SWT.BORDER | SWT.FLAT);
		pane.setText(RefactoringMessages.getString("PreviewWizardPage.changes")); //$NON-NLS-1$
		ToolBarManager tbm= pane.getToolBarManager();
		tbm.add(new NextChange());
		tbm.add(new PreviousChange());
		tbm.update(true);
		
		fTreeViewer= createTreeViewer(pane);
		fTreeViewer.setContentProvider(createTreeContentProvider());
		fTreeViewer.setLabelProvider(createTreeLabelProvider());
		fTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		fTreeViewer.addCheckStateListener(createCheckStateListener());
		pane.setContent(fTreeViewer.getControl());
		setTreeViewerInput();
		
		fPreviewContainer= new PageBook(sashForm, SWT.NONE);
		fComparePreview= new ComparePreviewer(fPreviewContainer);
		fNullPreviewer= new NullPreviewer(fPreviewContainer);
		fPreviewContainer.showPage(fNullPreviewer.getControl());
		
		sashForm.setWeights(new int[]{33, 67});
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(80);
		sashForm.setLayoutData(gd);
		
		setControl(result);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.REFACTORING_PREVIEW_WIZARD_PAGE);
	}
	
	/* (Non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void setVisible(boolean visible) {
		fCurrentSelection= null;
		ChangeElement treeViewerInput= (ChangeElement)fTreeViewer.getInput();
		if (visible && treeViewerInput != null) {
			IStructuredSelection selection= (IStructuredSelection)fTreeViewer.getSelection();
			if (selection.isEmpty()) {
				ITreeContentProvider provider= (ITreeContentProvider)fTreeViewer.getContentProvider();
				Object[] elements= provider.getElements(treeViewerInput);
				if (elements != null && elements.length > 0) {
					Object element= elements[0];
					if (getRefactoringWizard().getExpandFirstNode()) {
						Object[] subElements= provider.getElements(element);
						if (subElements != null && subElements.length > 0) {
							fTreeViewer.expandToLevel(element, 999);
						}
					}
					fTreeViewer.setSelection(new StructuredSelection(element));
				}
			}
		}
		super.setVisible(visible);
		fTreeViewer.getControl().setFocus();
	}
	
	private void setTreeViewerInput() {
		ChangeElement input;
		if (fChange == null) {
			input= null;
		} else if (fChange instanceof ICompositeChange && !(fChange instanceof TextChange)) {
			input= new DefaultChangeElement(null, fChange);
		} else {
			input= new DefaultChangeElement(null, new DummyRootNode(fChange));
		}
		if (fTreeViewer != null) {
			fTreeViewer.setInput(input);
		}
	}
	
	private ICheckStateListener createCheckStateListener() {
		return new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event){
				ChangeElement element= (ChangeElement)event.getElement();
				if (isChild(fCurrentSelection, element) || isChild(element, fCurrentSelection)) {
					showPreview(fCurrentSelection);
				}
			}
			private boolean isChild(ChangeElement element, ChangeElement child) {
				while (child != null) {
					if (child == element)
						return true;
					child= child.getParent();
				}
				return false;
			}
		};
	}
		
	private ISelectionChangedListener createSelectionChangedListener() {
		return new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				if (sel.size() == 1) {
					ChangeElement newSelection= (ChangeElement)sel.getFirstElement();
					if (newSelection != fCurrentSelection) {
						fCurrentSelection= newSelection;
						showPreview(newSelection);
					}
				} else {
					showPreview(null);
				}
			}
		};
	}	

	private void showPreview(ChangeElement element) {
		if (element != null)
			fCurrentPreviewViewer= getPreviewer(element, fCurrentPreviewViewer, fPreviewContainer);
		else
			fCurrentPreviewViewer= null;
			
		if (fCurrentPreviewViewer == null)
			fCurrentPreviewViewer= fNullPreviewer;
		fPreviewContainer.showPage(fCurrentPreviewViewer.getControl());
	}
	
	private ISourceReference findSourceReference(ChangeElement element) {
		if (element == null) {
			return null;
		} else if (element instanceof PseudoJavaChangeElement) {
			return (ISourceReference)((PseudoJavaChangeElement)element).getJavaElement();
		} else if (element instanceof DefaultChangeElement) {
			IChange change= ((DefaultChangeElement)element).getChange();
			if (change instanceof CompilationUnitChange) {
				return ((CompilationUnitChange)change).getCompilationUnit();
			}
		}
		return findSourceReference(element.getParent());
	}
	
	private List collectTextEditChanges(PseudoJavaChangeElement element) {
		List result= new ArrayList(10);
		ChangeElement[] children= element.getChildren();
		for (int i= 0; i < children.length; i++) {
			ChangeElement child= children[i];
			if (child instanceof TextEditChangeElement) {
				result.add(((TextEditChangeElement)child).getTextEditChange());
			} else if (child instanceof PseudoJavaChangeElement) {
				result.addAll(collectTextEditChanges((PseudoJavaChangeElement)child));
			}
		}
		return result;
	}
}