/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

/**
 * Add imports to a compilation unit.
 * The input is an array of full qualified type names. No elimination of unnecessary
 * imports is done (use ImportStructure for this). Duplicates are not added.
 * If the compilation unit is open in an editor, be sure to pass over its working copy.
 */
public class AddImportsOperation implements IWorkspaceRunnable {
	
	public static interface IChooseImportQuery {
		/**
		 * Selects an import from a list of choices.
		 * @param openChoices Array of found types
		 * @param containerName Name type to be imported
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeInfo chooseImport(TypeInfo[] openChoices, String containerName);
	}
	
	private ICompilationUnit fCompilationUnit;
	private final IDocument fDocument;
	private final int fSelectionOffset;
	private final int fSelectionLength;
	private final IChooseImportQuery fQuery;
	private IStatus fStatus;
	
	
	/**
	 * Generate import statements for the passed java elements
	 * Elements must be of type IType (-> single import) or IPackageFragment
	 * (on-demand-import). Other JavaElements are ignored
	 * @param cu The compilation unit
	 * @param document Document corrsponding to the compilation unit
	 * @param selectionOffset Start of the current text selection
	 * 	@param selectionLength End of the current text selection
	 * @param query Query element to be used fo UI interaction or <code>null</code> to not select anything
	 * when multiple possibilities are available
	 */
	public AddImportsOperation(ICompilationUnit cu, IDocument document, int selectionOffset, int selectionLength, IChooseImportQuery query) {
		super();
		Assert.isNotNull(cu);
		Assert.isNotNull(document);
		
		fCompilationUnit= cu;
		fDocument= document;
		fSelectionOffset= selectionOffset;
		fSelectionLength= selectionLength;
		fQuery= query;
		fStatus= Status.OK_STATUS;
	}
	
	/**
	 * @return Returns the status.
	 */
	public IStatus getStatus() {
		return fStatus;
	}

	/**
	 * Runs the operation.
	 * @param monitor The progress monito
	 * @throws CoreException  
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			monitor.beginTask(CodeGenerationMessages.getString("AddImportsOperation.description"), 15); //$NON-NLS-1$

			ImportRewrite importRewrite= new ImportRewrite(fCompilationUnit);
			importRewrite.setFindAmbiguosImports(true);
			
			CompilationUnit astRoot= JavaPlugin.getDefault().getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, new SubProgressMonitor(monitor, 5));
			TextEdit edit= evaluateEdits(astRoot, importRewrite, fSelectionOffset, fSelectionLength, new SubProgressMonitor(monitor, 5));
			if (edit != null) {
				edit.apply(fDocument, 0);
				
				TextEdit importsEdit= importRewrite.createEdit(fDocument, new SubProgressMonitor(monitor, 5));
				importsEdit.apply(fDocument, 0);
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			monitor.done();
		}
	}
	
	private TextEdit evaluateEdits(CompilationUnit root, ImportRewrite importRewrite, int offset, int length, IProgressMonitor monitor) throws BadLocationException, JavaModelException {
		SimpleName nameNode= null;
		if (root != null) { // got an AST
			ASTNode node= NodeFinder.perform(root, offset, length);
			if (node instanceof QualifiedName) {
				nameNode= ((QualifiedName) node).getName();
			} else if (node instanceof SimpleName) {
				nameNode= (SimpleName) node;
			}
			
		}
		
		String name, simpleName, containerName;
		int qualifierStart;
		int simpleNameStart;
		if (nameNode != null) {
			simpleName= nameNode.getIdentifier();
			simpleNameStart= nameNode.getStartPosition();
			if (nameNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
				Name qualifier= ((QualifiedName) nameNode.getParent()).getQualifier();
				containerName= qualifier.getFullyQualifiedName();
				name= JavaModelUtil.concatenateName(containerName, simpleName);
				qualifierStart= qualifier.getStartPosition();
			} else if (nameNode.getParent().getLocationInParent() == QualifiedType.NAME_PROPERTY) {
				Type qualifier= ((QualifiedType) nameNode.getParent().getParent()).getQualifier();
				containerName= ASTNodes.asString(qualifier);
				name= JavaModelUtil.concatenateName(containerName, simpleName);
				qualifierStart= qualifier.getStartPosition();
			} else if (nameNode.getLocationInParent() == MethodInvocation.NAME_PROPERTY) {
				ASTNode qualifier= ((MethodInvocation) nameNode.getParent()).getExpression();
				if (qualifier instanceof Name) {
					containerName= ASTNodes.asString(qualifier);
					name= JavaModelUtil.concatenateName(containerName, simpleName);
					qualifierStart= qualifier.getStartPosition();
				} else {
					return null;
				}
			} else {
				containerName= ""; //$NON-NLS-1$
				name= simpleName;
				qualifierStart= simpleNameStart;
			}
			
			IBinding binding= nameNode.resolveBinding();
			if (binding != null) {
				if (binding instanceof ITypeBinding) {
					ITypeBinding typeBinding= (ITypeBinding) binding;
					String qualifiedBindingName= typeBinding.getQualifiedName();
					if (containerName.length() > 0 && !qualifiedBindingName.equals(name)) {
						return null;
					}
					
					String res= importRewrite.addImport(typeBinding);
					if (containerName.length() > 0 && !res.equals(simpleName)) {
						// adding import failed
						return null;
					}
					return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
				} else if (binding instanceof IVariableBinding || binding instanceof IMethodBinding) {
					boolean isField= binding instanceof IVariableBinding;
					ITypeBinding declaringClass= isField ? ((IVariableBinding) binding).getDeclaringClass() : ((IMethodBinding) binding).getDeclaringClass();
					if (Modifier.isStatic(binding.getModifiers())) {
						if (containerName.length() > 0) { 
							if (containerName.equals(declaringClass.getName()) || containerName.equals(declaringClass.getQualifiedName()) ) {
								String res= importRewrite.addStaticImport(declaringClass.getQualifiedName(), binding.getName(), isField);
								if (!res.equals(simpleName)) {
									// adding import failed
									return null;
								}
								return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
							}
						}
					}
					return null; // no static imports for packages
				} else {
					return null;
				}
			}
			
		} else {
			qualifierStart= getNameStart(fDocument, offset);
			int nameEnd= getNameEnd(fDocument, offset + length);
			int len= nameEnd - qualifierStart;
			
			name= fDocument.get(qualifierStart, len).trim();
			simpleName= Signature.getSimpleName(name);
			containerName= Signature.getQualifier(name);
			
			simpleNameStart= getSimpleNameStart(fDocument, qualifierStart, containerName);
			
			String existingImport= importRewrite.findImport(simpleName);
			if (existingImport != null) {
				if (containerName.length() == 0) {
					return null;
				}
				if (!existingImport.equals(name)) {
					fStatus= JavaUIStatus.createError(IStatus.ERROR, CodeGenerationMessages.getFormattedString("AddImportsOperation.error.importclash", existingImport), null); //$NON-NLS-1$
					return null;
				}

				return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
			}
		}
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { fCompilationUnit.getJavaProject() });
		
		TypeInfo[] types= findAllTypes(simpleName, searchScope, new SubProgressMonitor(monitor, 1));
		if (types.length== 0) {
			fStatus= JavaUIStatus.createError(IStatus.ERROR, CodeGenerationMessages.getFormattedString("AddImportsOperation.error.notresolved.message", simpleName), null); //$NON-NLS-1$
			return null;
		}
		
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		TypeInfo chosen;
		if (types.length > 1 && fQuery != null) {
			chosen= fQuery.chooseImport(types, containerName);
			if (chosen == null) {
				throw new OperationCanceledException();
			}
		} else {
			chosen= types[0];
		}
		importRewrite.addImport(chosen.getFullyQualifiedName());
		return new ReplaceEdit(qualifierStart, simpleNameStart - qualifierStart, ""); //$NON-NLS-1$
	}

	
	private int getNameStart(IDocument doc, int pos) throws BadLocationException {
		while (pos > 0) {
			char ch= doc.getChar(pos - 1);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				return pos;
			}
			pos--;
		}
		return pos;
	}
	
	private int getNameEnd(IDocument doc, int pos) throws BadLocationException {
		if (pos > 0) {
			if (Character.isWhitespace(doc.getChar(pos - 1))) {
				return pos;
			}
		}
		int len= doc.getLength();
		while (pos < len) {
			char ch= doc.getChar(pos);
			if (!Character.isJavaIdentifierPart(ch)) {
				return pos;
			}
			pos++;
		}
		return pos;
	}
	
	private int getSimpleNameStart(IDocument doc, int nameStart, String containerName) throws BadLocationException {
		int containerLen= containerName.length();
		int docLen= doc.getLength();
		if ((containerLen > 0) && (nameStart + containerLen + 1 < docLen)) {
			for (int k= 0; k < containerLen; k++) {
				if (doc.getChar(nameStart + k) != containerName.charAt(k)) {
					return nameStart;
				}
			}
			if (doc.getChar(nameStart + containerLen) == '.') {
				return nameStart + containerLen;
			}
		}
		return nameStart;
	}
	
	
	/*
	 * Finds a type by the simple name.
	 */
	private static TypeInfo[] findAllTypes(String simpleTypeName, IJavaSearchScope searchScope, IProgressMonitor monitor) {
		return AllTypesCache.getTypesForName(simpleTypeName, searchScope, monitor);
	}
	
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
		
}
