/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.*;

import org.eclipse.jface.text.*;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;


public class JavaStructureCreator implements IStructureCreator {
	
	private static JavaTextLabelProvider fgLabelProvider= null;


	private static class ParseError extends Error {
	}
	
	static class ProblemFactory implements IProblemFactory {
		
		public IProblem createProblem(char[] originatingFileName, int problemId, String[] arguments, int severity, int startPosition, int endPosition, int lineNumber) {
			throw new ParseError();
		}
		
		public Locale getLocale() {
			return Locale.getDefault();
		}
		
		public String getLocalizedMessage(int problemId, String[] problemArguments) {
			return "" + problemId; //$NON-NLS-1$
		}
	}

	/**
	 *
	 */
	static class Info {
		
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
				
		boolean matches() {
			return !fIsOut && fAncestor != null && fLeft != null && fRight != null;
		}
	}
	
	private static JavaParseTreeBuilder fgRequestor= new JavaParseTreeBuilder();
	private static SourceElementParser fgParser= new SourceElementParser(fgRequestor, new ProblemFactory());
		
	
	public JavaStructureCreator() {
	}
	
	public String getName() {
		return CompareMessages.getString("JavaStructureViewer.title"); //$NON-NLS-1$
	}
	
	public IStructureComparator getStructure(final Object input) {
		
		String s= null;
		
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) input;			
			try {
				s= JavaCompareUtilities.readString(sca.getContents());
			} catch (CoreException ex) {
			}			
		}
		
		if (s != null) {
			int n= s.length();
			char[] buffer= new char[n];
			s.getChars(0, n, buffer, 0);
			
			final Document doc= new Document(s);
			IDocumentPartitioner dp= JavaCompareUtilities.createJavaPartitioner();
			doc.setDocumentPartitioner(dp);
			dp.connect(doc);
			
			boolean isEditable= false;
			if (input instanceof IEditableContent)
				isEditable= ((IEditableContent) input).isEditable();
			
			JavaNode root= new JavaNode(doc, isEditable) {
				void nodeChanged(JavaNode node) {
					save(this, input);
				}
			};
			fgRequestor.init(root, buffer);
			try {
				fgParser.parseCompilationUnit(fgRequestor, false);
			} catch (ParseError ex) {
				// System.out.println("Parse error: " + ex);
				// parse error: bail out
				return null;
			}
			return root;
		} 
		return null;
	}
	
	public boolean canSave() {
		return true;
	}
	
	public void save(IStructureComparator structure, Object input) {
		if (input instanceof IEditableContent && structure instanceof JavaNode) {
			IDocument doc= ((JavaNode)structure).getDocument();
			IEditableContent bca= (IEditableContent) input;
			String c= doc.get();
			bca.setContent(c.getBytes());
		}
	}
	
	public String getContents(Object node, boolean ignoreWhiteSpace) {
		
		if (! (node instanceof IStreamContentAccessor))
			return null;
		IStreamContentAccessor sca= (IStreamContentAccessor) node;
		String s= null;
		try {
			s= JavaCompareUtilities.readString(sca.getContents());
		} catch (CoreException ex) {
		}
				
		if (ignoreWhiteSpace) { 	// we copy everything but java whitespace
			
			StringBuffer buf= new StringBuffer();
			int i= 0;
			int l= s.length();
			while (i < l) {
				char c= s.charAt(i++);
				switch (c) {
				case '/':	// comment ?
					if (i < l) {
						c= s.charAt(i);
						if (c == '/') {	// line comment
							i++;
							while (i < l) {
								c= s.charAt(i++);
								if (c == '\n')
									break;
							}
						} else if (c == '*') {	// multi line comment
							i++;
							while (i < l) {
								c= s.charAt(i++);
								if (c == '*' && i < l) {
									c= s.charAt(i);
									if (c == '/') {
										i++;
										break;
									}
								}
							}
						} else
							;	// no comment
					} else
						buf.append((char)c);	
					break;
				case '"':
					buf.append((char)c);
					// don't ignore white space within strings
					while (i < l) {
						c= s.charAt(i++);
						buf.append((char)c);
						if (c == '"')
							break;
					}
					break;
				case '\'':
					buf.append((char)c);
					// don't ignore white space within character constants
					while (i < l) {
						c= s.charAt(i++);
						buf.append((char)c);
						if (c == '\'')
							break;
					}
					break;
				default:
					if (!Character.isWhitespace(c))
						buf.append((char)c);	
					break;
				}
			}
			s= buf.toString();
		}
		return s;
	}
	
	public boolean canRewriteTree() {
		return true;
	}
	
	/**
	 * Tries to detect certain combinations of additons and deletions
	 * as renames or signature changes and merges them into a single node.
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
			
			if (type == JavaNode.METHOD || type == JavaNode.CONSTRUCTOR) {
				
				String name= jn.getMethodName();
				Info info= (Info) map.get(name);
				if (info == null) {
					info= new Info();
					map.put(name, info);
				}
				info.add(diff);
				
				String sig= jn.getSignature();
				Info sinfo= null;
				if (sig != null && !sig.equals("()")) { //$NON-NLS-1$
					sinfo= (Info) map.get(sig);
					if (sinfo == null) {
						sinfo= new Info();
						map.put(sig, sinfo);
					}
					sinfo.add(diff);
				}
				
				switch (diff.getKind() & Differencer.CHANGE_TYPE_MASK) {
				case Differencer.ADDITION:
				case Differencer.DELETION:
					if (type != JavaNode.CONSTRUCTOR)
						info.setDiff((ICompareInput)diff);
					
					if (sinfo != null)
						sinfo.setDiff((ICompareInput)diff);
					break;
				default:
					break;
				}
			}
			
			// recurse
			if (diff instanceof IDiffContainer)
				rewriteTree(differencer, (IDiffContainer)diff);
		}
		
		Iterator it= map.keySet().iterator();
		while (it.hasNext()) {
			String name= (String) it.next();
			Info i= (Info) map.get(name);
			if (i.matches()) {
				DiffNode d= (DiffNode) differencer.findDifferences(true, null, root, i.fAncestor, i.fLeft, i.fRight);
				if (d == null)
					continue;	// should not happen...			
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
	
	/**
	 * 
	 */
	public IStructureComparator locate(Object input, Object item) {
		
		if (!(input instanceof IJavaElement))
			return null;

		IJavaElement je= (IJavaElement) input;
		
		List args= new ArrayList();
		while (je != null) {
			String name= getJavaElementID(je);
			if (name == null)
				return null;
			args.add(name);
			if (je instanceof IWorkingCopy || je instanceof ICompilationUnit)
				break;
			je= je.getParent();
		}
		
		int n= args.size();
		String[] path= new String[n];
		for (int i= 0; i < n; i++)
			path[i]= (String) args.get(n-1-i);
			
		IStructureComparator structure= getStructure(item);
		if (structure != null)
			return find(structure, path, 0);
		return null;
	}
			
	static boolean hasEdition(IJavaElement je) {
		return getJavaElementID(je) != null;
	}
		
	private static String getJavaElementID(IJavaElement je) {
		
		if (je instanceof IMember && ((IMember)je).isBinary())
			return null;
			
		StringBuffer sb= new StringBuffer();
		
		switch (je.getElementType()) {
		case JavaElement.COMPILATION_UNIT:
			sb.append(JavaElement.JEM_COMPILATIONUNIT);
			break;
		case JavaElement.TYPE:
			sb.append(JavaElement.JEM_TYPE);
			sb.append(je.getElementName());
			break;
		case JavaElement.FIELD:
			sb.append(JavaElement.JEM_FIELD);
			sb.append(je.getElementName());
			break;
		case JavaElement.METHOD:
			sb.append(JavaElement.JEM_METHOD);
			if (fgLabelProvider == null)
				fgLabelProvider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS);
			sb.append(fgLabelProvider.getTextLabel(je));
			break;
		case JavaElement.INITIALIZER:
			String id= je.getHandleIdentifier();
			int pos= id.lastIndexOf(JavaElement.JEM_INITIALIZER);
			if (pos >= 0)
				sb.append(id.substring(pos));
			break;
		case JavaElement.PACKAGE_DECLARATION:
			sb.append(JavaElement.JEM_PACKAGEDECLARATION);
			break;
		case JavaElement.IMPORT_CONTAINER:
			sb.append('<');
			break;
		case JavaElement.IMPORT_DECLARATION:
			sb.append(JavaElement.JEM_IMPORTDECLARATION);
			sb.append(je.getElementName());			
			break;
		default:
			return null;
		}
		return sb.toString();
	}	
	
	private IStructureComparator find(IStructureComparator node, String[] path, int index) {
				
		if (node != null) {
			Object[] children= node.getChildren();
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
}
