/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

class PullUpInputPage2 extends UserInputWizardPage {

	private static class PullUpFilter extends ViewerFilter {
		private final Set fTypesToShow;
	
		PullUpFilter(ITypeHierarchy hierarchy, IMember[] members) {
			//IType -> IMember[]
			Map typeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
			fTypesToShow= computeTypesToShow(hierarchy, typeToMemberArray);
		}
	
		private static Set computeTypesToShow(ITypeHierarchy hierarchy, Map typeToMemberArray) {
			Set typesToShow= new HashSet();
			typesToShow.add(hierarchy.getType());
			typesToShow.addAll(computeShowableSubtypesOfMainType(hierarchy, typeToMemberArray));
			return typesToShow;
		}
		
		private static Set computeShowableSubtypesOfMainType(ITypeHierarchy hierarchy, Map typeToMemberArray) {
			Set result= new HashSet();
			IType[] subtypes= hierarchy.getAllSubtypes(hierarchy.getType());
			for (int i= 0; i < subtypes.length; i++) {
				IType subtype= subtypes[i];
				if (canBeShown(subtype, typeToMemberArray, hierarchy))
					result.add(subtype);
			}
			return result;
		}
	
		private static boolean canBeShown(IType type, Map typeToMemberArray, ITypeHierarchy hierarchy) {
			if (typeToMemberArray.containsKey(type))
				return true;
			return anySubtypeCanBeShown(type, typeToMemberArray, hierarchy);	
		}
		
		private static boolean anySubtypeCanBeShown(IType type, Map typeToMemberArray, ITypeHierarchy hierarchy){
			IType[] subTypes= hierarchy.getSubtypes(type);
			for (int i= 0; i < subTypes.length; i++) {
				if (canBeShown(subTypes[i], typeToMemberArray, hierarchy))
					return true;
			}
			return false;
		}
	
		/*
		 * @see ViewerFilter#select(Viewer, Object, Object)
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IMethod)
				return true;
			return fTypesToShow.contains(element);
		}
	}
	
	private static class PullUpHierarchyContentProvider implements ITreeContentProvider {
		private ITypeHierarchy fHierarchy;
		private IMember[] fMembers;
		private Map fTypeToMemberArray; //IType -> IMember[]
		private IType fDeclaringType;
	
		PullUpHierarchyContentProvider(IType declaringType, IMember[] members) {
			fMembers= members;
			fDeclaringType= declaringType;
			fTypeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
		}
	
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType)
				return getSubclassesAndMembers((IType) parentElement);
			else
				return new Object[0];
		}
	
		private Object[] getSubclassesAndMembers(IType type) {
			Set set= new HashSet();
			set.addAll(Arrays.asList(getSubclasses(type)));
			set.addAll(Arrays.asList(getMembers(type)));
			return set.toArray();
		}
	
		private IType[] getSubclasses(IType type) {
			if (type.equals(fDeclaringType))
				return new IType[0];
			return fHierarchy.getSubclasses(type);
		}
	
		private IMember[] getMembers(IType type) {
			if (fTypeToMemberArray.containsKey(type))
				return (IMember[]) (fTypeToMemberArray.get(type));
			else
				return new IMember[0];
		}
		/*
		  * @see ITreeContentProvider#getParent(Object)
		  */
		public Object getParent(Object element) {
			if (element instanceof IType)
				return fHierarchy.getSuperclass((IType) element);
			if (element instanceof IMember)
				return ((IMember) element).getDeclaringType();
			Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
			return null;
		}
	
		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			if (!(element instanceof IType))
				return false;
			IType type= (IType) element;
			return hasSubtypes(type) || hasMembers(type);
		}
		
		private boolean hasSubtypes(IType type){
			return fHierarchy.getAllSubtypes(type).length > 0;
		}
		
		private boolean hasMembers(IType type){
			return fTypeToMemberArray.containsKey(type);
		}
	
		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			Assert.isTrue(inputElement ==null || inputElement instanceof ITypeHierarchy);
			return new IType[] { fHierarchy.getType()};
		}
	
		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fHierarchy= null;
			fTypeToMemberArray.clear();
			fTypeToMemberArray= null;
			fMembers= null;
			fDeclaringType= null;
		}
	
		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			Assert.isTrue(newInput ==null || newInput instanceof ITypeHierarchy);
			fHierarchy= (ITypeHierarchy) newInput;
		}
	}
	
  private Label fTypeHierarchyLabel;
  private SourceViewer fSourceViewer;
  private PullUpTreeViewer fTreeViewer;
  public static final String PAGE_NAME= "PullUpMethodsInputPage2"; //$NON-NLS-1$
	
  public PullUpInputPage2() {
	  super(PAGE_NAME, true);
	  setMessage(RefactoringMessages.getString("PullUpInputPage.select_methods")); //$NON-NLS-1$
  }

  /*
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
	  Composite composite= new Composite(parent, SWT.NONE);
	  composite.setLayout(new GridLayout());
		
	  createTreeAndSourceViewer(composite);
	  createButtonComposite(composite);
	  setControl(composite);
		
	  WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
  }

	private void createButtonComposite(Composite superComposite) {
		Composite buttonComposite= new Composite(superComposite, SWT.NONE);
		buttonComposite.setLayoutData(new GridData());
		GridLayout bcl= new GridLayout();
		bcl.numColumns= 2;
		bcl.marginWidth= 1;
		buttonComposite.setLayout(bcl);
	
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("PullUpInputPage2.Select")); //$NON-NLS-1$
		button.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PullUpInputPage2.this.checkPulledUp();
				updateTypeHierarchyLabel();
			}
		});
	}
	
	public void checkPulledUp() {
		uncheckAll();
		fTreeViewer.setCheckedElements(getPullUpMethodsRefactoring().getMembersToPullUp());
		IType parent= getPullUpMethodsRefactoring().getDeclaringType();
		fTreeViewer.setChecked(parent, true);
		checkAllParents(parent);
	}
	
	private void checkAllParents(IType parent) {
		ITypeHierarchy th= getTreeViewerInput();
		IType root= getTreeViewerInputRoot();
		IType type= parent;
		while (! root.equals(type)){
			fTreeViewer.setChecked(type, true);
			type= th.getSuperclass(type);			
		}
		fTreeViewer.setChecked(root, true);
	}

	private void uncheckAll() {
		IType root= getTreeViewerInputRoot();
		fTreeViewer.setSubtreeChecked(root, false);
		fTreeViewer.setSubtreeGrayed(root, false);
	}
	
	private IType getTreeViewerInputRoot() {
		return getTreeViewerInput().getType();
	}
	
	private ITypeHierarchy getTreeViewerInput() {
		return (ITypeHierarchy)fTreeViewer.getInput();
	}
	
  private void updateTypeHierarchyLabel(){
	  String message= RefactoringMessages.getFormattedString("PullUpInputPage.hierarchyLabal", //$NON-NLS-1$
					  new Integer(getCheckedMethods().length).toString());
	  setMessage(message, IMessageProvider.INFORMATION);
  }	
	
  private void createTreeAndSourceViewer(Composite superComposite) {
	  SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
	  initializeDialogUnits(superComposite);
	  GridData gd = new GridData(GridData.FILL_BOTH);
	  gd.heightHint= convertHeightInCharsToPixels(20);
	  gd.widthHint= convertWidthInCharsToPixels(10);
	  composite.setLayoutData(gd);
	  GridLayout layout= new GridLayout();
	  layout.numColumns= 2; 
	  layout.marginWidth= 0; 
	  layout.marginHeight= 0;
	  layout.horizontalSpacing= 1;
	  layout.verticalSpacing= 1;
	  composite.setLayout(layout);
		
	  createHierarchyTreeComposite(composite);
	  createSourceViewerComposite(composite);
	  composite.setWeights(new int[]{50, 50});
  }

	public void setVisible(boolean visible) {
		if (visible) {
			initializeTreeViewer();
			setHierarchyLabelText();
		}
		super.setVisible(visible);
	}

	private void initializeTreeViewer() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					try {
						initializeTreeViewer(pm);
					} finally {
						pm.done();
					}
				}
			});
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			Assert.isTrue(false); //not cancellable
		}
	}

  /*
   * @see IWizardPage#getNextPage()
   */	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}

	/*
	 * @see RefactoringWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		initializeRefactoring();
		return super.performFinish();
	}

	private void initializeRefactoring() {
		getPullUpMethodsRefactoring().setMethodsToDelete(getCheckedMethods());
	} 
	
  private IMethod[] getCheckedMethods(){
	  Object[] checked= fTreeViewer.getCheckedElements();
	  List members= new ArrayList(checked.length);
	  for (int i= 0; i < checked.length; i++) {
		  if (checked[i] instanceof IMethod)
			  members.add(checked[i]);
	  }
	  return (IMethod[]) members.toArray(new IMethod[members.size()]);
  }
	
	private void createSourceViewerComposite(Composite parent) {
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		c.setLayout(layout);
	
		createSourceViewerLabel(c);
		createSourceViewer(c);
	}
	
	private void createSourceViewer(Composite c) {
		  fSourceViewer= new SourceViewer(c, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		  fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools(), null));
		  fSourceViewer.setEditable(false);
		  fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));			
	}
	
	private void createSourceViewerLabel(Composite c) {
		  Label label= new Label(c, SWT.WRAP);
		  GridData gd= new GridData(GridData.FILL_HORIZONTAL);
   		  label.setText(RefactoringMessages.getString("PullUpInputPage2.Source")); //$NON-NLS-1$
		  label.setLayoutData(gd);
	}
	
	private void createHierarchyTreeComposite(Composite parent){
	  Composite composite= new Composite(parent, SWT.NONE);
	  composite.setLayoutData(new GridData(GridData.FILL_BOTH));
	  GridLayout layout= new GridLayout();
	  layout.marginWidth= 0; 
	  layout.marginHeight= 0;
	  layout.horizontalSpacing= 1;
	  layout.verticalSpacing= 1;
	  composite.setLayout(layout);

	  createTypeHierarchyLabel(composite);	
	  createTreeViewer(composite);
  }
  
	private void createTypeHierarchyLabel(Composite composite) {
		fTypeHierarchyLabel= new Label(composite, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		fTypeHierarchyLabel.setLayoutData(gd);
	}
	
	private void setHierarchyLabelText() {
		  String message= RefactoringMessages.getFormattedString("PullUpInputPage.subtypes", getSupertypeSignature()); //$NON-NLS-1$
		  fTypeHierarchyLabel.setText(message);
	}
	
	private String getSupertypeSignature(){
			return JavaElementUtil.createSignature(getPullUpMethodsRefactoring().getTargetClass());
	}

	private void createTreeViewer(Composite composite) {
		Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTreeViewer= new PullUpTreeViewer(tree);
		fTreeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setSorter(new JavaElementSorter());	
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				PullUpInputPage2.this.treeViewerSelectionChanged(event);
			}
		});
		fTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				PullUpInputPage2.this.updateTypeHierarchyLabel();
			}
		});
	}
	
	private void precheckElements(final PullUpTreeViewer treeViewer) {
		IMember[] members= getPullUpMethodsRefactoring().getMembersToPullUp();
		for (int i= 0; i < members.length; i++) {
			treeViewer.setCheckState(members[i], true);
		}
	}

	private void initializeTreeViewer(IProgressMonitor pm) {
		try {
			IMember[] matchingMethods= getPullUpMethodsRefactoring().getMatchingElements(new SubProgressMonitor(pm, 1), false);
			ITypeHierarchy hierarchy= getPullUpMethodsRefactoring().getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1));
			removeAllTreeViewFilters();
			fTreeViewer.addFilter(new PullUpFilter(hierarchy, matchingMethods));
			fTreeViewer.setContentProvider(new PullUpHierarchyContentProvider(getPullUpMethodsRefactoring().getDeclaringType(), matchingMethods));
			fTreeViewer.setInput(hierarchy);
			precheckElements(fTreeViewer);
			fTreeViewer.expandAll();
			updateTypeHierarchyLabel();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			fTreeViewer.setInput(null);
		}
	}
	
	private void removeAllTreeViewFilters() {
		ViewerFilter[] filters= fTreeViewer.getFilters();
		for (int i= 0; i < filters.length; i++) {
			fTreeViewer.removeFilter(filters[i]);
		}
	}

  private void treeViewerSelectionChanged(SelectionChangedEvent event) {
	  try{	
		  showInSourceViewer(getFirstSelectedSourceReference(event));
	  } catch (JavaModelException e){
		  ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.see_log")); //$NON-NLS-1$ //$NON-NLS-2$
	  }
  }

	private ISourceReference getFirstSelectedSourceReference(SelectionChangedEvent event){
		ISelection s= event.getSelection();
		if (!(s instanceof IStructuredSelection))
			return null;
		IStructuredSelection ss= (IStructuredSelection)s;
		if (ss.size() != 1)
			return null;	
		Object first= ss.getFirstElement();
		if (! (first instanceof ISourceReference))
			return null;
		return (ISourceReference)first;
	}
	
  private void setSourceViewerContents(String contents) {
	  if (fSourceViewer.getDocument() != null){
		  IDocument document= fSourceViewer.getDocument();
		  document.getDocumentPartitioner().disconnect();
		  document.setDocumentPartitioner(null);
	  }
		
	  IDocument document= (contents == null) ? new Document(): new Document(contents);
		
	  IDocumentPartitioner partitioner= getJavaTextTools().createDocumentPartitioner();
	  partitioner.connect(document);
	  document.setDocumentPartitioner(partitioner);
		
	  fSourceViewer.setDocument(document);
  }
	
  private void showInSourceViewer(ISourceReference selected) throws JavaModelException{
	if (selected == null)
		setSourceViewerContents(null);
	else		
		setSourceViewerContents(selected.getSource());
  }
	
  private static JavaTextTools getJavaTextTools() {
	  return JavaPlugin.getDefault().getJavaTextTools();	
  }
	
	//IType -> IMember[]
	private static Map createTypeToMemberArrayMapping(IMember[] members) {
		Map typeToMemberSet= createTypeToMemberSetMapping(members);
	
		Map typeToMemberArray= new HashMap();
		for (Iterator iter= typeToMemberSet.keySet().iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			Set memberSet= (Set) typeToMemberSet.get(type);
			IMember[] memberArray= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
			typeToMemberArray.put(type, memberArray);
		}
		return typeToMemberArray;
	}
	
	//	IType -> Set of IMember
	private static Map createTypeToMemberSetMapping(IMember[] members) {
		Map typeToMemberSet= new HashMap();
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			IType type= member.getDeclaringType();
			if (! typeToMemberSet.containsKey(type))
				typeToMemberSet.put(type, new HashSet());
			((Set) typeToMemberSet.get(type)).add(member);
		}
		return typeToMemberSet;
	}
	
  private PullUpRefactoring getPullUpMethodsRefactoring(){
	  return (PullUpRefactoring)getRefactoring();
  }
}
