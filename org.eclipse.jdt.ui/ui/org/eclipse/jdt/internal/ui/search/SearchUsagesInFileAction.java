/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v0.5 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v05.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.MarkerUtilities;
 
/**
 * Searches for occurences of the element at the caret 
 * in a file using the AST.
 */
public class SearchUsagesInFileAction extends Action {
	
	static public class NameUsagesFinder extends ASTVisitor {
		private IBinding fTarget;
		private List fUsages= new ArrayList();
		private List fWriteUsages= new ArrayList();
	
		public NameUsagesFinder(IBinding target) {
			super();
			fTarget= target;
		}

		public boolean visit(QualifiedName node) {
			match(node, fUsages);
			return super.visit(node);
		}

		public boolean visit(SimpleName node) {
			match(node, fUsages);
			return super.visit(node);
		}

		public boolean visit(Assignment node) {
			Expression lhs= node.getLeftHandSide();
			Name name= getName(lhs);
			if (name != null) 
				match(name, fWriteUsages);	
			lhs.accept(this);
			node.getRightHandSide().accept(this);
			return false;
		}
		
		public boolean visit(SingleVariableDeclaration node) {
			if (node.getInitializer() != null)
				match(node.getName(), fWriteUsages);
			return super.visit(node);
		}

		public boolean visit(VariableDeclarationFragment node) {
			if (node.getInitializer() != null)
				match(node.getName(), fWriteUsages);
			return super.visit(node);
		}

		public boolean visit(PrefixExpression node) {
			PrefixExpression.Operator operator= node.getOperator();	
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				Expression operand= node.getOperand();
				Name name= getName(operand);
				if (name != null) 
					match(name, fWriteUsages);				
			}
			return super.visit(node);
		}

		public boolean visit(PostfixExpression node) {
			Expression operand= node.getOperand();
			Name name= getName(operand);
			if (name != null) 
				match(name, fWriteUsages);
			return super.visit(node);
		}

		public List getUsages() {
			return fUsages;
		}
		
		public List getWriteUsages() {
			return fWriteUsages;
		}
		
		private void match(Name node, List result) {
			IBinding binding= node.resolveBinding();
			
			if (binding == null)
				return;
				
			if (binding.equals(fTarget)) {
				result.add(node);
				return;
			}
			
			String otherKey= binding.getKey();
			String targetKey= fTarget.getKey();
						
			if (targetKey != null && otherKey != null) {
				if (targetKey.equals(otherKey))
					result.add(node);
			}
		}

		private Name getName(Expression expression) {
			if (expression instanceof SimpleName)
				return ((SimpleName)expression);
			else if (expression instanceof QualifiedName)
				return ((QualifiedName)expression);
			else if (expression instanceof FieldAccess)
				return ((FieldAccess)expression).getName();
			return null;
		}	
	}

	static class SearchGroupByKeyComputer implements IGroupByKeyComputer {
		//TODO it seems that GroupByKeyComputers are obsolete...
		public Object computeGroupByKey(IMarker marker) {
			return marker; 
		}
	}
	
	public static final String SHOWREFERENCES= "ShowReferences"; //$NON-NLS-1$
	public static final String IS_WRITEACCESS= "writeAccess"; //$NON-NLS-1$
	public static final String IS_VARIABLE= "variable"; //$NON-NLS-1$

	private JavaEditor fEditor;
	private IRunnableWithProgress fNullOperation= getNullOperation();
	
	public SearchUsagesInFileAction(JavaEditor editor) {
		super(SearchMessages.getString("Search.FindUsageInFile.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindUsageInFile.tooltip")); //$NON-NLS-1$
		// TODO help context ID
		fEditor= editor;
		boolean enabled= false;
		if (getClassFile() != null || SelectionConverter.getInputAsCompilationUnit(fEditor) != null)
			enabled= true;
		setEnabled(enabled); 
	}
		
	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public final  void run() {
		ITextSelection ts= getTextSelection();
		final CompilationUnit root= createAST();
		if (root == null)
			return;
		ASTNode node= NodeFinder.perform(root, ts.getOffset(), ts.getLength());
		
		if (!(node instanceof Name))
			return;
 
		final Name name= (Name)node;
		final IBinding target= name.resolveBinding();
		
		if (target == null)
			return;
		
		final IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IJavaElement element= getInput();
				ISearchResultView view= startSearch(element.getElementName(), name);
				NameUsagesFinder finder= new NameUsagesFinder(target);
				root.accept(finder);
				List matches= finder.getUsages();
				List writeMatches= finder.getWriteUsages();
				IResource file= getResource(element);				
				for (Iterator each= matches.iterator(); each.hasNext();) {
					ASTNode node= (ASTNode) each.next();
					addMatch(
						view, 
						file, 
						createMarker(file, node, writeMatches.contains(node), 
						target instanceof IVariableBinding)
					);
				}
				searchFinished(view);
			}
		};
		run(runnable);
	}
	
	public CompilationUnit createAST() {
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFile classFile= ((IClassFileEditorInput)input).getClassFile();
			return AST.parseCompilationUnit(classFile, true);
		}
		return AST.parseCompilationUnit(getCompilationUnit(), true);
	}
	
	public void run(final IWorkspaceRunnable runnable) {
		BusyIndicator.showWhile(fEditor.getSite().getShell().getDisplay(),
			new Runnable() {
				public void run() {
					try {
						ResourcesPlugin.getWorkspace().run(runnable, null);
					} catch (CoreException e) {
						JavaPlugin.log(e);
					}
				}
			}
		);
	}
	
	public ISearchResultView startSearch(String fileName, final Name name) {
		SearchUI.activateSearchResultView();
		ISearchResultView view= SearchUI.getSearchResultView();
		String elementName= getName(name);
		
		if (view != null) 
			view.searchStarted(
				null,
				getSingularLabel(elementName, fileName),
				getPluralLabel(elementName, fileName),
				JavaPluginImages.DESC_OBJS_SEARCH_REF,
				"org.eclipse.jdt.ui.JavaFileSearch", //$NON-NLS-1$
				new SearchUsagesInFileLabelProvider(),
				new GotoMarkerAction(), 
				new SearchGroupByKeyComputer(),
				fNullOperation
			);	
		return view;
	}
	
	public String getPluralLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, "{0}", elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("JavaSearchInFile.pluralPostfix", args); //$NON-NLS-1$
	}
	
	public String getSingularLabel(String nodeContents, String elementName) {
		String[] args= new String[] {nodeContents, elementName}; //$NON-NLS-1$
		return SearchMessages.getFormattedString("JavaSearchInFile.singularPostfix", args); //$NON-NLS-1$
	}

	public void addMatch(final ISearchResultView view, IResource file, IMarker marker) {
		if (view != null)
			view.addMatch("", getGroupByKey(marker), file, marker); //$NON-NLS-1$
	}
	
	private Object getGroupByKey(IMarker marker) {
		try {
			return marker.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
		}
		return marker;
	}
	
	public void searchFinished(final ISearchResultView view) {
		if (view != null) 
			view.searchFinished();
	}

	public IRunnableWithProgress getNullOperation() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			}
		};
	}
	
	private String getName(Name name) {
		IDocument document= getDocument();
		String result= ""; //$NON-NLS-1$
		try {
			result= document.get(name.getStartPosition(), name.getLength());
		} catch (BadLocationException e) {
		}
		return result;
	}
		
	private IResource getResource(IJavaElement element) throws JavaModelException {
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			if (cu.isWorkingCopy()) {
				IJavaElement original= cu.getOriginalElement();
				return original.getUnderlyingResource();
			}
			return cu.getUnderlyingResource();
		}
		return element.getJavaProject().getProject();
	}
	
	private IMarker createMarker(IResource file, ASTNode element, boolean writeAccess, boolean isVariable) throws CoreException {
		Map attributes= new HashMap(10);
		IMarker marker= file.createMarker(SearchUI.SEARCH_MARKER);

		int startPosition= element.getStartPosition();
		MarkerUtilities.setCharStart(attributes, startPosition);
		MarkerUtilities.setCharEnd(attributes, startPosition + element.getLength());
		
		// for class files
		if (!(file instanceof IFile)) {
			attributes.put(IWorkbenchPage.EDITOR_ID_ATTR, JavaUI.ID_CF_EDITOR);
			JavaCore.addJavaElementMarkerAttributes(attributes, getClassFile().getType());
			attributes.put(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, getClassFile().getType().getHandleIdentifier());
		}

		if(writeAccess)
			attributes.put(IS_WRITEACCESS, new Boolean(true));

		if(isVariable)
			attributes.put(IS_VARIABLE, new Boolean(true));
			
		IDocument document= getDocument();
		try {
			int line= document.getLineOfOffset(startPosition);
			MarkerUtilities.setLineNumber(attributes, line);
			IRegion region= document.getLineInformation(line);
			String lineContents= document.get(region.getOffset(), region.getLength());
			MarkerUtilities.setMessage(attributes, lineContents.trim());
		} catch (BadLocationException e) {
		}
		marker.setAttributes(attributes);
		return marker;
	}

	protected final IJavaElement getInput() {
		IClassFile classFile= getClassFile();
		if (classFile != null)
			return classFile;
		return getCompilationUnit();
	}
	
	protected ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
	}
	
	protected final IClassFile getClassFile() {
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		return null;
	}
	
	protected final IDocument getDocument() {
		return fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
	}
	
	protected final ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}
}
