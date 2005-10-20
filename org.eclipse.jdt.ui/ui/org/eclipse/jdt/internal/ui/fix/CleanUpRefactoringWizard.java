package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.PageBook;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;


public class CleanUpRefactoringWizard extends RefactoringWizard {

	private final boolean fShowCUPage;
	private final boolean fShowCleanUpPage;

	private class SelectCUPage extends UserInputWizardPage {

		private ContainerCheckedTreeViewer fTreeViewer;

		public SelectCUPage(String name) {
			super(name);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			
			createViewer(composite);
			setControl(composite);
			
			Dialog.applyDialogFont(composite);
		}
		
		private TreeViewer createViewer(Composite parent) {
			fTreeViewer= new ContainerCheckedTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(40);
			gd.heightHint= convertHeightInCharsToPixels(15);
			fTreeViewer.getTree().setLayoutData(gd);
			fTreeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS));
			fTreeViewer.setContentProvider(new StandardJavaElementContentProvider());
			fTreeViewer.setSorter(new JavaElementSorter());
			fTreeViewer.addFilter(new ViewerFilter() {

				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof IJavaElement) {
						IJavaElement jElement= (IJavaElement)element;
						return !jElement.isReadOnly();
					} else {
						return false;
					}
				}
				
			});
			IJavaModel create= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			fTreeViewer.setInput(create);
			checkElements(fTreeViewer, (CleanUpRefactoring)getRefactoring());
			return fTreeViewer;
		}
		
		private void checkElements(CheckboxTreeViewer treeViewer, CleanUpRefactoring refactoring) {
			ICompilationUnit[] compilationUnits= refactoring.getCompilationUnits();
			for (int i= 0; i < compilationUnits.length; i++) {
				ICompilationUnit compilationUnit= compilationUnits[i];
				treeViewer.expandToLevel(compilationUnit, 0);
				treeViewer.setChecked(compilationUnit, true);
			}
		}

		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}
	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private void initializeRefactoring() {
			CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
			refactoring.clearCompilationUnits();
			Object[] checkedElements= fTreeViewer.getCheckedElements();
			for (int i= 0; i < checkedElements.length; i++) {
				if (checkedElements[i] instanceof ICompilationUnit)
					refactoring.addCompilationUnit((ICompilationUnit)checkedElements[i]);
			}
		}

	}
	
	private class SelectSolverPage extends UserInputWizardPage {
		
		private abstract class MultiQuickFixTree {
			private final String fLabel;
			private MultiQuickFixTree fParent;
			
			public MultiQuickFixTree(String label) {
				this.fLabel= label;
			}
			
			public String getLabel() {
				return fLabel;
			}
			
			public abstract boolean hasChildren();
			public abstract List/*<MultiQuickFixTree>*/ getChildren();

			public MultiQuickFixTree getParent() {
				return fParent;
			}
			
			public void setParent(MultiQuickFixTree parent) {
				fParent= parent;
			}
		}
		
		private class MultiQuickFixTreeLeaf extends MultiQuickFixTree {
			private final IMultiFix fFix;
			
			public MultiQuickFixTreeLeaf(String label, IMultiFix fix) {
				super(label);
				fFix= fix;
			}

			public boolean hasChildren() {
				return false;
			}

			public List getChildren() {
				return null;
			}
			
			public IMultiFix getFix() {
				return fFix;
			}
		}
		
		private class MultiQuickFixTreeInner extends MultiQuickFixTree {
			private final List/*<MultiQuickFixTree>*/ fChildren;
			
			public MultiQuickFixTreeInner(String label) {
				super(label);
				fChildren= new ArrayList();
			}
			
			public void addChildren(MultiQuickFixTree tree) {
				fChildren.add(tree);
				tree.setParent(this);
			}

			public boolean hasChildren() {
				return !fChildren.isEmpty();
			}

			public List getChildren() {
				return fChildren;
			}
		}

		private CheckboxTreeViewer fTreeViewer;

		public SelectSolverPage(String name) {
			super(name);
		}
		
		private PageBook fBook;
		
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
			
			
			Composite tree= new Composite(composite, SWT.NONE);
			tree.setLayout(new GridLayout());
			tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			createViewer(tree);
			
			fBook= new PageBook(composite, SWT.NONE);
			fBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite emptyPage= new Composite(fBook, SWT.NONE);
			emptyPage.setLayout(new GridLayout(1, false));
			emptyPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			fBook.showPage(emptyPage);
			
			createTreeListeners();
	
			setControl(composite);
			
			Dialog.applyDialogFont(composite);
		}
		
		private void createTreeListeners() {
			fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					rebuildPages(event.getSelection(), fTreeViewer.getCheckedElements());
				}
			});
		}

		private void rebuildPages(ISelection selection, Object[] checkedElements) {
			if (selection instanceof StructuredSelection) {
				StructuredSelection strucSelection= (StructuredSelection)selection;
				Object firstElement= strucSelection.getFirstElement();
			
				if (firstElement instanceof MultiQuickFixTreeLeaf) {
					
					List checked= Arrays.asList(checkedElements);
					
					IMultiFix fix= ((MultiQuickFixTreeLeaf)firstElement).getFix();
					
					Control control= fix.createConfigurationControl(fBook);
					if (!checked.contains(firstElement))
						disable(control);
					fBook.showPage(control);
				} else {
					Composite emptyPage= new Composite(fBook, SWT.NONE);
					emptyPage.setLayout(new GridLayout(1, false));
					emptyPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					fBook.showPage(emptyPage);
				}
			}
		}
		
		private void disable(Control control) {
			if (control instanceof Composite) {
				Composite composite= (Composite)control;
				Control[] children= composite.getChildren();
				for (int i= 0; i < children.length; i++) {
					disable(children[i]);
				}
			}
			control.setEnabled(false);
		}

		private TreeViewer createViewer(Composite parent) {
			MultiQuickFixTree root= getSolutionTree();
			
			ProblemSolutionContentProvider contentProvider= new ProblemSolutionContentProvider();
			ProblemSolutionLabelProvider lableProvider= new ProblemSolutionLabelProvider();
			
			fTreeViewer= new ContainerCheckedTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(40);
			gd.heightHint= convertHeightInCharsToPixels(15);
			fTreeViewer.getTree().setLayoutData(gd);
			fTreeViewer.setLabelProvider(lableProvider);
			fTreeViewer.setContentProvider(contentProvider);
			fTreeViewer.setInput(root);
			return fTreeViewer;
		}
		
		private MultiQuickFixTree getSolutionTree() {
			MultiQuickFixTreeInner root= new MultiQuickFixTreeInner("root"); //$NON-NLS-1$
			MultiQuickFixTreeLeaf strings= new MultiQuickFixTreeLeaf("String externalization", new StringMultiFix(true, true));
			root.addChildren(strings);
			MultiQuickFixTreeLeaf unusedCode= new MultiQuickFixTreeLeaf("Unused code", new UnusedCodeMultiFix(true));
			root.addChildren(unusedCode);
			MultiQuickFixTreeLeaf java50= new MultiQuickFixTreeLeaf("J2SE 5.0", new Java50MultiFix(true, true));
			root.addChildren(java50);
			MultiQuickFixTreeLeaf codeStyle= new MultiQuickFixTreeLeaf("Code style", new CodeStyleMultiFix(true, true));
			root.addChildren(codeStyle);
			return root;
		}

		private class ProblemSolutionLabelProvider extends LabelProvider {
			public String getText(Object element) {
				return ((MultiQuickFixTree)element).getLabel();
			}
		}
		
		private class ProblemSolutionContentProvider implements ITreeContentProvider {

			public Object[] getChildren(Object parentElement) {
				MultiQuickFixTree tree= (MultiQuickFixTree)parentElement;
				if (tree.hasChildren())
					return tree.getChildren().toArray(new MultiQuickFixTree[tree.getChildren().size()]);
				return null;
			}

			public Object getParent(Object element) {
				MultiQuickFixTree tree= (MultiQuickFixTree)element;
				return tree.getParent();
			}

			public boolean hasChildren(Object element) {
				MultiQuickFixTree tree= (MultiQuickFixTree)element;
				return tree.hasChildren();
			}

			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
		}
		
		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}
	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private void initializeRefactoring() {
			CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
			refactoring.clearProblemSolutions();
			Object[] checkedElements= fTreeViewer.getCheckedElements();
			for (int i= 0; i < checkedElements.length; i++) {
				if (checkedElements[i] instanceof MultiQuickFixTreeLeaf)
					refactoring.addProblemSolution(((MultiQuickFixTreeLeaf)checkedElements[i]).getFix());
			}
		}
		
	}

	public CleanUpRefactoringWizard(CleanUpRefactoring refactoring, int flags, boolean showCUPage, boolean showCleanUpPage) {
		super(refactoring, flags);
		fShowCUPage= showCUPage;
		fShowCleanUpPage= showCleanUpPage;
		setDefaultPageTitle("Clean up wizard");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		if (fShowCUPage) {
			SelectCUPage selectCUPage= new SelectCUPage("Select Compilation units Page");
			selectCUPage.setMessage("Select compilation units to clean up.");
			addPage(selectCUPage);
		}
		
		if (fShowCleanUpPage){
			SelectSolverPage selectSolverPage= new SelectSolverPage("Select clean ups Page");
			selectSolverPage.setMessage("Select clean ups and set there options to applay to the selected compilation units.");
			addPage(selectSolverPage);
		}
	}

}
