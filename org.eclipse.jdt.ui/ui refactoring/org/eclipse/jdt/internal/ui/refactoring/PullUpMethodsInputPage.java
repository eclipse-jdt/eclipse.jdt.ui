package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpMethodRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PullUpMethodsInputPage extends UserInputWizardPage {
	
	private class PullUpMethodsFilter extends ViewerFilter {
		private Set fOkTypes;
		
		PullUpMethodsFilter(ITypeHierarchy hierarchy, IMethod[] methods){
			//IType -> IMethod[]
			Map typeToMethodArray= PullUpMethodsInputPage.createTypeToMethodArrayMapping(methods);
			
			fOkTypes= new HashSet();
			IType[] subtypes= hierarchy.getAllSubtypes(hierarchy.getType());
			for (int i= 0; i < subtypes.length; i++) {
				IType subtype= subtypes[i];
				if (isOk(subtype, typeToMethodArray, hierarchy))
					fOkTypes.add(subtype);
			}
			fOkTypes.add(hierarchy.getType());
		}
		
		private boolean isOk(IType type, Map typeToMethodArray, ITypeHierarchy hierarchy){
			if (typeToMethodArray.containsKey(type))
				return true;
			IType[] subTypes= hierarchy.getSubtypes(type);
			for (int i= 0; i < subTypes.length; i++) {
				IType subType= subTypes[i];
				if (isOk(subType, typeToMethodArray, hierarchy))
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
	
	private static class PullUpMethodsHierarchyContentProvider implements ITreeContentProvider{
		private ITypeHierarchy fHierarchy;
		private IMethod[] fMethods;
		private Map fTypeToMethodArray; //IType -> IMethod[]
		
		PullUpMethodsHierarchyContentProvider(IMethod[] methods){
			fMethods= methods;
			fTypeToMethodArray= PullUpMethodsInputPage.createTypeToMethodArrayMapping(methods);
		}
				
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof IType))
				return new Object[0];
			IType type= (IType)parentElement; 
			Set set= new HashSet();
			set.addAll(Arrays.asList(fHierarchy.getSubclasses(type)));
			if (fTypeToMethodArray.containsKey(type))
				set.addAll(Arrays.asList((IMethod[])(fTypeToMethodArray.get(type))));
			return set.toArray();
		}

			/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IType)
				return fHierarchy.getSuperclass((IType)element);
			if (element instanceof IMethod)
				return ((IMethod)element).getDeclaringType();
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
			if (fTypeToMethodArray.containsKey(type))	
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
			fTypeToMethodArray.clear();
			fTypeToMethodArray= null;
			fMethods= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof ITypeHierarchy)
				fHierarchy= (ITypeHierarchy)newInput;
		}
	}
	
	private SourceViewer fSourceViewer;
	private CheckboxTreeViewer fTreeViewerViewer;
	private Label fContextLabel;
	private Set fMarkedMethods; //Set of IMethods
	public static final String PAGE_NAME= "PullUpMethodsInputPage"; //$NON-NLS-1$
	private static final String fgHelpContextID= "HELP CONTEXT";
	
	public PullUpMethodsInputPage() {
		super(PAGE_NAME, true);
		fMarkedMethods= new HashSet();
		setMessage("Select the methods to be deleted");
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		SashForm composite= new SashForm(parent, SWT.HORIZONTAL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2; 
		layout.marginWidth= 0; 
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		composite.setLayout(layout);
		
		createHierarchyTreeComposite(composite);
		createSourceViewer(composite);
		
		composite.setWeights(new int[]{50, 50});
		setControl(composite);
		
		///FIX ME: wrong
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, fgHelpContextID));			
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
		Object[] checked= fTreeViewerViewer.getCheckedElements();
		List methods= new ArrayList(checked.length);
		for (int i= 0; i < checked.length; i++) {
			if (checked[i] instanceof IMethod)
				methods.add(checked[i]);
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);
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
		
		fContextLabel= new Label(c, SWT.NONE);
		fContextLabel.setLayoutData(new GridData());
		
		fSourceViewer= new SourceViewer(c, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools(), null));
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));			
	}
	
	private void createHierarchyTreeComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0; 
		layout.marginHeight= 0;
		layout.horizontalSpacing= 1;
		layout.verticalSpacing= 1;
		composite.setLayout(layout);

		Label label= new Label(composite, SWT.NONE);
		label.setText("Type hierarchy");
		label.setLayoutData(new GridData());
		
		fTreeViewerViewer= createTreeViewer(composite);
	}

	private CheckboxTreeViewer createTreeViewer(Composite composite) {
		Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		final CheckboxTreeViewer treeViever= new PullUpMethodTreeViewer(tree);
		treeViever.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		treeViever.setUseHashlookup(true);
		
		//XXX pm
		try{
			fMarkedMethods.addAll(Arrays.asList(getPullUpMethodsRefactoring().getMethodsToPullUp()));
			IMethod[] matchingMethods= getPullUpMethodsRefactoring().getMatchingMethods();
			ITypeHierarchy hierarchy= getPullUpMethodsRefactoring().getSuperType().newTypeHierarchy(new NullProgressMonitor());
			treeViever.addFilter(new PullUpMethodsFilter(hierarchy, matchingMethods));	
			treeViever.setContentProvider(new PullUpMethodsHierarchyContentProvider(matchingMethods));
			treeViever.setInput(hierarchy);
			
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Pull Up Methods", "Unexpected exception. See log for details.");
			treeViever.setInput(null);
		}	
		treeViever.expandAll();
		treeViever.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				PullUpMethodsInputPage.this.selectionChanged(event);
			}
		});
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
			ExceptionHandler.handle(e, "Pull Up Methods", "See log");
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
		if (entry == null)
			fContextLabel.setText("");
		else{
			String text= "";
			if (entry instanceof IMethod)
				text= "Source of method: '" + ((IMethod)entry).getElementName()+ "' declared in type '" + ((IMethod)entry).getDeclaringType().getFullyQualifiedName() + "'";
			else if (entry instanceof IType)	
				text= "Source of type: '" + ((IType)entry).getElementName()+ "' declared in package '" + ((IType)entry).getPackageFragment().getElementName()+ "'";
			fContextLabel.setText(text);
		}	
		fContextLabel.pack(true);		
	}
	
	private boolean isMarkedToDelete(Object object){
		return fMarkedMethods.contains(object);
	}	
	
	private static Map createTypeToMethodArrayMapping(IMethod[] fMethods){
			Map typeToMethodSet= new HashMap();
			for (int i = 0; i < fMethods.length; i++) {
				IMethod method = fMethods[i];
				IType type= method.getDeclaringType();
				if (!typeToMethodSet.containsKey(type))
					typeToMethodSet.put(type, new HashSet());

				((Set)typeToMethodSet.get(type)).add(method);
			}
			
			Map typeToMethodArray= new HashMap();
			for (Iterator iter = typeToMethodSet.keySet().iterator(); iter.hasNext();) {
				IType type = (IType) iter.next();
				Set methodSet= (Set)typeToMethodSet.get(type);
				IMethod[] methodArray = (IMethod[]) methodSet.toArray(new IMethod[methodSet.size()]);
				typeToMethodArray.put(type, methodArray);
			}
			return typeToMethodArray;
		}
	
	private PullUpMethodRefactoring getPullUpMethodsRefactoring(){
		return (PullUpMethodRefactoring)getRefactoring();
	}
}