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
import org.eclipse.core.runtime.NullProgressMonitor;
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

import org.eclipse.ui.PlatformUI;
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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class PullUpInputPage extends UserInputWizardPage {
	
	private static class PullUpFilter extends ViewerFilter {
		private Set fOkTypes;
		
		PullUpFilter(ITypeHierarchy hierarchy, IMember[] members){
			//IType -> IMember[]
			Map typeToMemberArray= PullUpInputPage.createTypeToMemberArrayMapping(members);
			
			fOkTypes= new HashSet();
			IType[] subtypes= hierarchy.getAllSubtypes(hierarchy.getType());
			for (int i= 0; i < subtypes.length; i++) {
				IType subtype= subtypes[i];
				if (isOk(subtype, typeToMemberArray, hierarchy))
					fOkTypes.add(subtype);
			}
			fOkTypes.add(hierarchy.getType());
		}
		
		private boolean isOk(IType type, Map typeToMemberArray, ITypeHierarchy hierarchy){
			if (typeToMemberArray.containsKey(type))
				return true;
			IType[] subTypes= hierarchy.getSubtypes(type);
			for (int i= 0; i < subTypes.length; i++) {
				IType subType= subTypes[i];
				if (isOk(subType, typeToMemberArray, hierarchy))
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
			return fOkTypes.contains(element);	
		}
	}
	
	private static class PullUpHierarchyContentProvider implements ITreeContentProvider{
		private ITypeHierarchy fHierarchy;
		private IMember[] fMembers;
		private Map fTypeToMemberArray; //IType -> IMember[]
		private IType fDeclaringType; 
		
		PullUpHierarchyContentProvider(IType declaringType, IMember[] members){
			fMembers= members;
			fDeclaringType= declaringType;
			fTypeToMemberArray= PullUpInputPage.createTypeToMemberArrayMapping(members);
		}
				
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof IType))
				return new Object[0];
			IType type= (IType)parentElement; 
			Set set= new HashSet();
			if (!type.equals(fDeclaringType))
				set.addAll(Arrays.asList(fHierarchy.getSubclasses(type)));
			if (fTypeToMemberArray.containsKey(type))
				set.addAll(Arrays.asList((IMember[])(fTypeToMemberArray.get(type))));
			return set.toArray();
		}

			/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IType)
				return fHierarchy.getSuperclass((IType)element);
			if (element instanceof IMember)
				return ((IMember)element).getDeclaringType();
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			if (! (element instanceof IType))
				return false;
			IType type= (IType)element;
			if (fHierarchy.getAllSubtypes(type).length > 0)
				return true;
			if (fTypeToMemberArray.containsKey(type))	
				return true;
			return false;	
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITypeHierarchy)
				return new IType[]{fHierarchy.getType()};
			return null;
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fHierarchy= null;
			fTypeToMemberArray.clear();
			fTypeToMemberArray= null;
			fMembers= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof ITypeHierarchy)
				fHierarchy= (ITypeHierarchy)newInput;
		}
	}
	
	private Label fTypeHierarchyLabel;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private SourceViewer fSourceViewer;
	private PullUpTreeViewer fTreeViewer;
	private Label fContextLabel;
	private Set fMarkedMethods; //Set of IMethods
	public static final String PAGE_NAME= "PullUpMethodsInputPage"; //$NON-NLS-1$
	
	public PullUpInputPage() {
		super(PAGE_NAME, true);
		fMarkedMethods= new HashSet();
		setMessage(RefactoringMessages.getString("PullUpInputPage.select_methods")); //$NON-NLS-1$
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite superComposite= new Composite(parent, SWT.NONE);
		superComposite.setLayout(new GridLayout());
		
		createTreeAndSourceViewer(superComposite);
		createButtonComposite(superComposite);
		setControl(superComposite);
		
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
	}

	private void createButtonComposite(Composite superComposite) {
		Composite buttonComposite= new Composite(superComposite, SWT.NONE);
		buttonComposite.setLayoutData(new GridData());
		GridLayout bcl= new GridLayout();
		bcl.numColumns= 2;
		bcl.marginWidth= 1;
		buttonComposite.setLayout(bcl);

		fSelectAllButton= createSelectButton(buttonComposite, RefactoringMessages.getString("PullUpInputPage.select_All"), true); //$NON-NLS-1$
		fDeselectAllButton= createSelectButton(buttonComposite, RefactoringMessages.getString("PullUpInputPage.deselect_All"), false); //$NON-NLS-1$
	}
	
	private Button createSelectButton(Composite composite, String buttonLabel, final boolean select){
		Button button= new Button(composite, SWT.PUSH);
		button.setText(buttonLabel);		
		button.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				IType root= ((ITypeHierarchy)fTreeViewer.getInput()).getType();
				fTreeViewer.setSubtreeChecked(root, select);
				fTreeViewer.setSubtreeGrayed(root, false);
				updateTypeHierarchyLabel();
			}
		});
		return button;
	}

	private void updateTypeHierarchyLabel(){
		String message= RefactoringMessages.getFormattedString("PullUpInputPage.hierarchyLabal", //$NON-NLS-1$
						new Integer(getCheckedMethods().length).toString());
		setMessage(message, IMessageProvider.INFORMATION);
	}	
	
	private void createTreeAndSourceViewer(Composite superComposite) {
		SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.numColumns= 2; 
		layout.marginWidth= 0; 
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		composite.setLayout(layout);
		
		initializeDialogUnits(composite);
		
		createHierarchyTreeComposite(composite);
		createSourceViewer(composite);
		composite.setWeights(new int[]{50, 50});
	}

	/*
	 * @see IWizardPage#getNextPage()
	 */	
	public IWizardPage getNextPage() {
		getPullUpMethodsRefactoring().setMethodsToDelete(getCheckedMethods());
		return super.getNextPage();
	}

	/*
	 * @see RefactoringWizardPage#performFinish()
	 */		
	protected boolean performFinish() {
		getPullUpMethodsRefactoring().setMethodsToDelete(getCheckedMethods());
		return super.performFinish();
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
	
	private void createSourceViewer(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0; 
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		c.setLayout(layout);
		
		fContextLabel= new Label(c, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(2);
		fContextLabel.setLayoutData(gd);
		
		fSourceViewer= new SourceViewer(c, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools(), null));
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));			
	}

	private void createHierarchyTreeComposite(final SashForm composite) {
		try {
			IRunnableWithProgress op= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						createHierarchyTreeComposite0(composite, pm);						
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, op);
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InterruptedException e) {
			Assert.isTrue(false);//not cancellable
		}
	}
	
	private void createHierarchyTreeComposite0(Composite parent, IProgressMonitor pm) throws JavaModelException {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0; 
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		composite.setLayout(layout);

		fTypeHierarchyLabel= new Label(composite, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(2);
		fTypeHierarchyLabel.setLayoutData(gd);
		String message= RefactoringMessages.getFormattedString("PullUpInputPage.subtypes", //$NON-NLS-1$
							JavaElementUtil.createSignature(getPullUpMethodsRefactoring().getSuperType(new NullProgressMonitor())));
		fTypeHierarchyLabel.setText(message);
		
		fTreeViewer= createTreeViewer(composite, pm);
		updateTypeHierarchyLabel();
	}

	private PullUpTreeViewer createTreeViewer(Composite composite, IProgressMonitor pm) {
		pm.beginTask("", 2); //$NON-NLS-1$
		Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		final PullUpTreeViewer treeViever= new PullUpTreeViewer(tree);
		treeViever.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		treeViever.setUseHashlookup(true);
		treeViever.setSorter(new JavaElementSorter());

		try{
			fMarkedMethods.addAll(Arrays.asList(getPullUpMethodsRefactoring().getElementsToPullUp()));
			IMember[] matchingMethods= getPullUpMethodsRefactoring().getMatchingElements(new SubProgressMonitor(pm, 1));
			ITypeHierarchy hierarchy= getPullUpMethodsRefactoring().getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
			treeViever.addFilter(new PullUpFilter(hierarchy, matchingMethods));	
			treeViever.setContentProvider(new PullUpHierarchyContentProvider(getPullUpMethodsRefactoring().getDeclaringType(), matchingMethods));
			treeViever.setInput(hierarchy);			
			treeViever.expandAll();
			treeViever.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					PullUpInputPage.this.selectionChanged(event);
				}
			});
			treeViever.addCheckStateListener(new ICheckStateListener(){
				public void checkStateChanged(CheckStateChangedEvent event){
					updateTypeHierarchyLabel();
				}
			});
		
			IMember[] members= getPullUpMethodsRefactoring().getElementsToPullUp();
			for (int i= 0; i < members.length; i++) {
				treeViever.setCheckState(members[i], true);
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			treeViever.setInput(null);
		}	
		
		return treeViever;
	}

	private void selectionChanged(SelectionChangedEvent event) {
		ISelection s= event.getSelection();
		if (!(s instanceof IStructuredSelection))
			return;
		IStructuredSelection ss= (IStructuredSelection)s;
		if (ss.size() != 1)
			return;	
		Object first= ss.getFirstElement();
		if (! (first instanceof ISourceReference))
			return;
		try{	
			showInSourceViewer((ISourceReference)first);
		} 	catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.see_log")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private void setSourceViewerContents(String contents) {
		if (fSourceViewer.getDocument() != null){
			IDocument document= fSourceViewer.getDocument();
			document.getDocumentPartitioner().disconnect();
			document.setDocumentPartitioner(null);
		}
		
		IDocument document= new Document(contents);
		
		IDocumentPartitioner partitioner= getJavaTextTools().createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		fSourceViewer.setDocument(document);
	}
	
	private void showInSourceViewer(ISourceReference selected) throws JavaModelException{
		setSourceViewerContents(selected.getSource());
		setLabelText(selected);		
	}
	
	private static JavaTextTools getJavaTextTools() {
		return JavaPlugin.getDefault().getJavaTextTools();	
	}
	
	private void setLabelText(ISourceReference entry){
		fContextLabel.setText(createLabelText(entry));	
		fContextLabel.pack(true);		
	}

	private static String createLabelText(ISourceReference entry) {
		if (entry instanceof IMethod){
			return RefactoringMessages.getFormattedString("PullUpInputPage.method", //$NON-NLS-1$
					 new String[]{((IMethod)entry).getElementName(), JavaModelUtil.getFullyQualifiedName(((IMethod)entry).getDeclaringType())});
		} else if (entry instanceof IType) {	
			return  RefactoringMessages.getFormattedString("PullUpInputPage.type",  //$NON-NLS-1$
					JavaModelUtil.getFullyQualifiedName((IType)entry));
		} else
			return ""; //$NON-NLS-1$
	}
	
	private boolean isMarkedToDelete(Object object){
		return fMarkedMethods.contains(object);
	}	
	
	private static Map createTypeToMemberArrayMapping(IMember[] members){
		Map typeToMemberSet= new HashMap();
		for (int i = 0; i < members.length; i++) {
			IMember member = members[i];
			IType type= member.getDeclaringType();
			if (! typeToMemberSet.containsKey(type))
				typeToMemberSet.put(type, new HashSet());
			((Set)typeToMemberSet.get(type)).add(member);
		}
			
		Map typeToMemberArray= new HashMap();
		for (Iterator iter = typeToMemberSet.keySet().iterator(); iter.hasNext();) {
			IType type = (IType) iter.next();
			Set memberSet= (Set)typeToMemberSet.get(type);
			IMember[] memberArray = (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
			typeToMemberArray.put(type, memberArray);
		}
		return typeToMemberArray;
	}
	
	private PullUpRefactoring getPullUpMethodsRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}
}