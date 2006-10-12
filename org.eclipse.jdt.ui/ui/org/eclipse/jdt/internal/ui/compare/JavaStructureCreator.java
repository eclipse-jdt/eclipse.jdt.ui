/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.services.IDisposable;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.SharedDocumentAdapterWrapper;
import org.eclipse.compare.structuremergeviewer.StructureCreator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class JavaStructureCreator extends StructureCreator {
	
	private Map fDefaultCompilerOptions;
	
	private final class RootJavaNode extends JavaNode implements IDisposable {
		
		private final Object fInput;
		private final IDisposable fDisposable;

		private RootJavaNode(IDocument document, boolean editable, Object input, IDisposable disposable) {
			super(document, editable);
			fInput= input;
			fDisposable= disposable;
		}

		void nodeChanged(JavaNode node) {
			save(this, fInput);
		}

		public void dispose() {
			if (fDisposable != null)
				fDisposable.dispose();
		}

		public Object getAdapter(Class adapter) {
			if (adapter == ISharedDocumentAdapter.class) {
				ISharedDocumentAdapter elementAdapter= SharedDocumentAdapterWrapper.getAdapter(fInput);
				if (elementAdapter == null)
					return null;
				
				return new SharedDocumentAdapterWrapper(elementAdapter) {
					public IEditorInput getDocumentKey(Object element) {
						if (element instanceof JavaNode)
							return getWrappedAdapter().getDocumentKey(fInput);

						return super.getDocumentKey(element);
					}
				};
			}
			
			return super.getAdapter(adapter);
		}
	}

	/**
	 * RewriteInfos are used temporarily when rewriting the diff tree
	 * in order to combine similar diff nodes ("smart folding").
	 */
	static class RewriteInfo {
		
		boolean fIsOut= false;
		
		JavaNode fAncestor= null;
		JavaNode fLeft= null;
		JavaNode fRight= null;
		
		ArrayList fChildren= new ArrayList();
		
		void add(IDiffElement diff) {
			fChildren.add(diff);
		}
		
		void setDiff(ICompareInput diff) {
			if (fIsOut)
				return;
			
			fIsOut= true;
			
			JavaNode a= (JavaNode) diff.getAncestor();
			JavaNode y= (JavaNode) diff.getLeft();
			JavaNode m= (JavaNode) diff.getRight();
			
			if (a != null) {
				if (fAncestor != null)
					return;
				fAncestor= a;
			}
			if (y != null) {
				if (fLeft != null)
					return;
				fLeft= y;
			}
			if (m != null) {
				if (fRight != null)
					return;
				fRight= m;
			}
			
			fIsOut= false;
		}
				
		/**
		 * Returns true if some nodes could be successfully combined into one.
		 */
		boolean matches() {
			return !fIsOut && fAncestor != null && fLeft != null && fRight != null;
		}
	}		
	
	public JavaStructureCreator() {
	}
	
	void setDefaultCompilerOptions(Map compilerSettings) {
		fDefaultCompilerOptions= compilerSettings;
	}
	
	/**
	 * Returns the name that appears in the enclosing pane title bar.
	 */
	public String getName() {
		return CompareMessages.JavaStructureViewer_title; 
	}
	
	/**
	 * Returns a tree of JavaNodes for the given input
	 * which must implement the IStreamContentAccessor interface.
	 * In case of error null is returned.
	 */
	public IStructureComparator getStructure(final Object input) {
		String contents= null;
		char[] buffer= null;
		IDocument doc= CompareUI.getDocument(input);
		if (doc == null) {
			if (input instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) input;			
				try {
					contents= JavaCompareUtilities.readString(sca);
				} catch (CoreException ex) {
					// return null indicates the error.
					return null;
				}			
			}
			
			if (contents != null) {
				int n= contents.length();
				buffer= new char[n];
				contents.getChars(0, n, buffer, 0);
				
				doc= new Document(contents);
				setupDocument(doc);				
			}
		}
		
		return createStructureComparator(input, buffer, doc, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DocumentStructureCreator#createStructureComparator(java.lang.Object, org.eclipse.jface.text.IDocument, org.eclipse.ui.services.IDisposable)
	 */
	protected IStructureComparator createStructureComparator(Object input, IDocument doc, IDisposable disposable) throws CoreException {
		return createStructureComparator(input, null, doc, disposable);
	}
	
	private IStructureComparator createStructureComparator(final Object input, char[] buffer, IDocument doc, IDisposable disposable) {
		String contents;
		Map compilerOptions= null;
		
		if (input instanceof IResourceProvider) {
			IResource resource= ((IResourceProvider) input).getResource();
			if (resource != null) {
				IJavaElement element= JavaCore.create(resource);
				if (element != null) {
					IJavaProject javaProject= element.getJavaProject();
					if (javaProject != null)
						compilerOptions= javaProject.getOptions(true);
				}
			}
		}
		if (compilerOptions == null)
			compilerOptions= fDefaultCompilerOptions;
		
		if (doc != null) {
			boolean isEditable= false;
			if (input instanceof IEditableContent)
				isEditable= ((IEditableContent) input).isEditable();
			
			// we hook into the root node to intercept all node changes
			JavaNode root= new RootJavaNode(doc, isEditable, input, disposable);
			
			if (buffer == null) {
				contents= doc.get();
				int n= contents.length();
				buffer= new char[n];
				contents.getChars(0, n, buffer, 0);
			}
						
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			if (compilerOptions != null)
				parser.setCompilerOptions(compilerOptions);
			parser.setSource(buffer);
			parser.setFocalPosition(0);
			CompilationUnit cu= (CompilationUnit) parser.createAST(null);
			cu.accept(new JavaParseTreeBuilder(root, buffer, true));
			
			return root;
		}
		return null;
	}
	
	/**
	 * Returns the contents of the given node as a string.
	 * This string is used to test the content of a Java element
	 * for equality. Is is never shown in the UI, so any string representing
	 * the content will do.
	 * @param node must implement the IStreamContentAccessor interface
	 * @param ignoreWhiteSpace if true all Java white space (including comments) is removed from the contents.
	 */
	public String getContents(Object node, boolean ignoreWhiteSpace) {
		
		if (! (node instanceof IStreamContentAccessor))
			return null;
			
		IStreamContentAccessor sca= (IStreamContentAccessor) node;
		String content= null;
		try {
			content= JavaCompareUtilities.readString(sca);
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
			return null;
		}
				
		if (ignoreWhiteSpace) { 	// we return everything but Java whitespace
			
			// replace comments and whitespace by a single blank
			StringBuffer buf= new StringBuffer();
			char[] b= content.toCharArray();
			
			// to avoid the trouble when dealing with Unicode
			// we use the Java scanner to extract non-whitespace and non-comment tokens
			IScanner scanner= ToolFactory.createScanner(true, true, false, false);	// however we request Whitespace and Comments
			scanner.setSource(b);
			try {
				int token;
				while ((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
					switch (token) {
					case ITerminalSymbols.TokenNameWHITESPACE:
					case ITerminalSymbols.TokenNameCOMMENT_BLOCK:
					case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
					case ITerminalSymbols.TokenNameCOMMENT_LINE:
						int l= buf.length();
						if (l > 0 && buf.charAt(l-1) != ' ')
							buf.append(' ');
						break;
					default:
						buf.append(scanner.getCurrentTokenSource());
						buf.append(' ');
						break;
					}
				}
				content= buf.toString();	// success!
			} catch (InvalidInputException ex) {
				// NeedWork
			}
		}
		return content;
	}
	
	/**
	 * Returns true since this IStructureCreator can rewrite the diff tree
	 * in order to fold certain combinations of additions and deletions.
	 */
	public boolean canRewriteTree() {
		return true;
	}
	
	/**
	 * Tries to detect certain combinations of additions and deletions
	 * as renames or signature changes and folders them into a single node.
	 */
	public void rewriteTree(Differencer differencer, IDiffContainer root) {
		
		HashMap map= new HashMap(10);
				
		Object[] children= root.getChildren();
		for (int i= 0; i < children.length; i++) {
			DiffNode diff= (DiffNode) children[i];
			JavaNode jn= (JavaNode) diff.getId();
			
			if (jn == null)
				continue;
			int type= jn.getTypeCode();
			
			// we can only combine methods or constructors
			if (type == JavaNode.METHOD || type == JavaNode.CONSTRUCTOR) {
				
				// find or create a RewriteInfo for all methods with the same name
				String name= jn.extractMethodName();
				RewriteInfo nameInfo= (RewriteInfo) map.get(name);
				if (nameInfo == null) {
					nameInfo= new RewriteInfo();
					map.put(name, nameInfo);
				}
				nameInfo.add(diff);
				
				// find or create a RewriteInfo for all methods with the same
				// (non-empty) argument list
				String argList= jn.extractArgumentList();
				RewriteInfo argInfo= null;
				if (argList != null && !argList.equals("()")) { //$NON-NLS-1$
					argInfo= (RewriteInfo) map.get(argList);
					if (argInfo == null) {
						argInfo= new RewriteInfo();
						map.put(argList, argInfo);
					}
					argInfo.add(diff);
				}
				
				switch (diff.getKind() & Differencer.CHANGE_TYPE_MASK) {
				case Differencer.ADDITION:
				case Differencer.DELETION:
					// we only consider addition and deletions
					// since a rename or argument list change looks
					// like a pair of addition and deletions
					if (type != JavaNode.CONSTRUCTOR)
						nameInfo.setDiff(diff);
					
					if (argInfo != null)
						argInfo.setDiff(diff);
					break;
				default:
					break;
				}
			}
			
			// recurse
			rewriteTree(differencer, diff);
		}
		
		// now we have to rebuild the diff tree according to the combined
		// changes
		Iterator it= map.keySet().iterator();
		while (it.hasNext()) {
			String name= (String) it.next();
			RewriteInfo i= (RewriteInfo) map.get(name);
			if (i.matches()) { // we found a RewriteInfo that could be successfully combined
				
				// we have to find the differences of the newly combined node
				// (because in the first pass we only got a deletion and an addition)
				DiffNode d= (DiffNode) differencer.findDifferences(true, null, root, i.fAncestor, i.fLeft, i.fRight);
				if (d != null) {// there better should be a difference
					d.setDontExpand(true);
					Iterator it2= i.fChildren.iterator();
					while (it2.hasNext()) {
						IDiffElement rd= (IDiffElement) it2.next();
						root.removeToRoot(rd);
						d.add(rd);
					}
				}
			}
		}
	}
	
	/**
	 * If selector is an IJavaElement this method tries to return an
	 * IStructureComparator object for it.
	 * In case of error or if the given selector cannot be found
	 * null is returned.
	 * @param selector the IJavaElement to extract
	 * @param input must implement the IStreamContentAccessor interface.
	 */
	public IStructureComparator locate(Object selector, Object input) {
		if (!(selector instanceof IJavaElement))
			return null;

		// try to build the JavaNode tree from input
		// TODO: Could make use of shared document
		IStructureComparator structure= getStructure(input);
		if (structure == null)	// we couldn't parse the structure 
			return null;		// so we can't find anything
			
		// build a path
		String[] path= createPath((IJavaElement) selector);
			
		// find the path in the JavaNode tree
		return find(structure, path, 0);
	}
	
	private static String[] createPath(IJavaElement je) {
			
		// build a path starting at the given Java element and walk
		// up the parent chain until we reach a IWorkingCopy or ICompilationUnit
		List args= new ArrayList();
		while (je != null) {
			// each path component has a name that uses the same
			// conventions as a JavaNode name
			String name= JavaCompareUtilities.getJavaElementID(je);
			if (name == null)
				return null;
			args.add(name);
			if (je instanceof ICompilationUnit)
				break;
			je= je.getParent();
		}
		
		// revert the path
		int n= args.size();
		String[] path= new String[n];
		for (int i= 0; i < n; i++)
			path[i]= (String) args.get(n-1-i);
			
		return path;
	}
	
	/**
	 * Recursively extracts the given path from the tree.
	 */
	private static IStructureComparator find(IStructureComparator tree, String[] path, int index) {
		if (tree != null) {
			Object[] children= tree.getChildren();
			if (children != null) {
				for (int i= 0; i < children.length; i++) {
					IStructureComparator child= (IStructureComparator) children[i];
					if (child instanceof ITypedElement && child instanceof DocumentRangeNode) {
						String n1= null;
						if (child instanceof DocumentRangeNode)
							n1= ((DocumentRangeNode)child).getId();
						if (n1 == null)
							n1= ((ITypedElement)child).getName();
						String n2= path[index];
						if (n1.equals(n2)) {
							if (index == path.length-1)
								return child;
							IStructureComparator result= find(child, path, index+1);
							if (result != null)
								return result;
						}	
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns true if the given IJavaElement maps to a JavaNode.
	 * The JavaHistoryAction uses this function to determine whether
	 * a selected Java element can be replaced by some piece of
	 * code from the local history.
	 */
	static boolean hasEdition(IJavaElement je) {

		if (je instanceof IMember && ((IMember)je).isBinary())
			return false;
			
		switch (je.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
		case IJavaElement.TYPE:
		case IJavaElement.FIELD:
		case IJavaElement.METHOD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.PACKAGE_DECLARATION:
		case IJavaElement.IMPORT_CONTAINER:
		case IJavaElement.IMPORT_DECLARATION:
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#getDocumentPartitioner()
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return JavaCompareUtilities.createJavaPartitioner();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#getDocumentPartitioning()
	 */
	protected String getDocumentPartitioning() {
		return IJavaPartitions.JAVA_PARTITIONING;
	}
}
