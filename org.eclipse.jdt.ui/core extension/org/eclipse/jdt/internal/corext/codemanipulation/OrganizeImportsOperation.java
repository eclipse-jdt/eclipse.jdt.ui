/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class OrganizeImportsOperation implements IWorkspaceRunnable {

	public static interface IChooseImportQuery {
		/**
		 * Selects imports from a list of choices.
		 * @param openChoices From each array, a type ref has to be selected
		 * @param ranges For each choice the range of the corresponding  type reference.
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges);
	}
	
	
	private static class TypeRefASTVisitor extends GenericVisitor {

		private Selection fSubRange;
		private Collection fResult;
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;

		public TypeRefASTVisitor(Selection rangeLimit, Collection result, ArrayList oldSingleImports, ArrayList oldDemandImports) {
			super(true);
			fResult= result;
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fSubRange= rangeLimit;
		}
		
		private boolean isAffected(ASTNode node) {
			return fSubRange == null || !fSubRange.liesOutside(node);
		}
		
		
		private void addReference(SimpleName name) {
			if (isAffected(name)) {
				fResult.add(name);
			}
		}			
		
		private void typeRefFound(Name node) {
			if (node != null) {
				while (node.isQualifiedName()) {
					node= ((QualifiedName) node).getQualifier();
				}
				addReference((SimpleName) node);
			}
		}

		private void possibleTypeRefFound(Name node) {
			while (node.isQualifiedName()) {
				node= ((QualifiedName) node).getQualifier();
			}
			IBinding binding= node.resolveBinding();
			if (binding == null || binding.getKind() == IBinding.TYPE) {
				// if the binding is null, we cannot determine if 
				// we have a type binding or not, so we will assume
				// we do.
				addReference((SimpleName) node);
			}
		}
		
		private void doVisitChildren(List elements) {
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				((ASTNode) elements.get(i)).accept(this);
			}
		}
		
		private void doVisitNode(ASTNode node) {
			if (node != null) {
				node.accept(this);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
		 */
		protected boolean visitNode(ASTNode node) {
			return isAffected(node);
		}
		
		/*
		 * @see ASTVisitor#visit(ArrayType)
		 */
		public boolean visit(ArrayType node) {
			doVisitNode(node.getElementType());
			return false;
		}

		/*
		 * @see ASTVisitor#visit(SimpleType)
		 */
		public boolean visit(SimpleType node) {
			typeRefFound(node.getName());
			return false;
		}
		
		/*
		 * @see ASTVisitor#visit(QualifiedName)
		 */
		public boolean visit(QualifiedName node) {
			possibleTypeRefFound(node); // possible ref
			return false;
		}		

		/*
		 * @see ASTVisitor#visit(ImportDeclaration)
		 */
		public boolean visit(ImportDeclaration node) {
			String id= ASTResolving.getFullName(node.getName());
			if (node.isOnDemand()) {
				fOldDemandImports.add(id);
			} else {
				fOldSingleImports.add(id);
			}
			return false;
		}
		
		/*
		 * @see ASTVisitor#visit(PackageDeclaration)
		 */
		public boolean visit(PackageDeclaration node) {
			return false;
		}				

		/*
		 * @see ASTVisitor#visit(ThisExpression)
		 */
		public boolean visit(ThisExpression node) {
			typeRefFound(node.getQualifier());
			return false;
		}

		private void evalQualifyingExpression(Expression expr) {
			if (expr != null) {
				if (expr instanceof Name) {
					possibleTypeRefFound((Name) expr);
				} else {
					expr.accept(this);
				}
			}
		}			

		/*
		 * @see ASTVisitor#visit(ClassInstanceCreation)
		 */
		public boolean visit(ClassInstanceCreation node) {
			typeRefFound(node.getName());
			evalQualifyingExpression(node.getExpression());
			if (node.getAnonymousClassDeclaration() != null) {
				node.getAnonymousClassDeclaration().accept(this);
			}
			doVisitChildren(node.arguments());
			return false;
		}

		/*
		 * @see ASTVisitor#endVisit(MethodInvocation)
		 */
		public boolean visit(MethodInvocation node) {
			evalQualifyingExpression(node.getExpression());
			doVisitChildren(node.arguments());
			return false;
		}

		/*
		 * @see ASTVisitor#visit(SuperConstructorInvocation)
		 */		
		public boolean visit(SuperConstructorInvocation node) {
			if (!isAffected(node)) {
				return false;
			}
			
			evalQualifyingExpression(node.getExpression());
			doVisitChildren(node.arguments());
			return false;	
		}		

		/*
		 * @see ASTVisitor#visit(FieldAccess)
		 */
		public boolean visit(FieldAccess node) {
			evalQualifyingExpression(node.getExpression());
			return false;
		}
		
		/*
		 * @see ASTVisitor#visit(SimpleName)
		 */
		public boolean visit(SimpleName node) {
			// if the call gets here, it can only be a variable reference
			return false;
		}

		/*
		 * @see ASTVisitor#visit(TypeDeclaration)
		 */
		public boolean visit(TypeDeclaration node) {
			if (!isAffected(node)) {
				return false;
			}
			doVisitNode(node.getJavadoc());
			
			typeRefFound(node.getSuperclass());

			Iterator iter= node.superInterfaces().iterator();
			while (iter.hasNext()) {
				typeRefFound((Name) iter.next());
			}
			doVisitChildren(node.bodyDeclarations());
			return false;
		}
		
		/*
		 * @see ASTVisitor#visit(MethodDeclaration)
		 */
		public boolean visit(MethodDeclaration node) {
			if (!isAffected(node)) {
				return false;
			}
			doVisitNode(node.getJavadoc());
			
			if (!node.isConstructor()) {
				doVisitNode(node.getReturnType());
			}
			doVisitChildren(node.parameters());
			Iterator iter=node.thrownExceptions().iterator();
			while (iter.hasNext()) {
				typeRefFound((Name) iter.next());
			}
			doVisitNode(node.getBody());
			return false;
		}
		
		public boolean visit(TagElement node) {
			String name= node.getTagName();
			List list= node.fragments();
			int idx= 0;
			if (name != null && !list.isEmpty()) {
				Object first= list.get(0);
				if (first instanceof Name) {
					if ("@throws".equals(name) || "@exception".equals(name)) {  //$NON-NLS-1$//$NON-NLS-2$
						typeRefFound((Name) first);
					} else if ("@see".equals(name) || "@link".equals(name) || "@linkplain".equals(name)) {  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						possibleTypeRefFound((Name) first);
					}
					idx++;
				}
			}
			for (int i= idx; i < list.size(); i++) {
				doVisitNode((ASTNode) list.get(i));
			}
			return false;
		}
		
		public boolean visit(MemberRef node) {
			Name name= node.getName();
			if (name != null) {
				typeRefFound(name);
			}
			return false;
		}
		
		public boolean visit(MethodRef node) {
			Name qualifier= node.getQualifier();
			if (qualifier != null) {
				typeRefFound(qualifier);
			}
			List list= node.parameters();
			if (list != null) {
				doVisitChildren(list); // visit MethodRefParameter with Type
			}
			return false;
		}
	}
		
	private static class TypeReferenceProcessor {
		
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;
		
		private HashSet fImportsAdded;
		
		private ImportsStructure fImpStructure;
				
		private ArrayList fTypeRefsFound; // cached array list for reuse
		
		private boolean fDoIgnoreLowerCaseNames;
		
		private IJavaSearchScope fSearchScope;
		private IPackageFragment fCurrPackage;
		
		private ScopeAnalyzer fAnalyzer;
		
		public TypeReferenceProcessor(ArrayList oldSingleImports, ArrayList oldDemandImports, CompilationUnit root, ImportsStructure impStructure, boolean ignoreLowerCaseNames) {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
			fDoIgnoreLowerCaseNames= ignoreLowerCaseNames;
			fAnalyzer= new ScopeAnalyzer(root);

			ICompilationUnit cu= fImpStructure.getCompilationUnit();
			fSearchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { cu.getJavaProject() });
			fCurrPackage= (IPackageFragment) cu.getParent();
					
			fTypeRefsFound= new ArrayList();  	// cached array list for reuse
			fImportsAdded= new HashSet();		
		}
		
		private boolean needsImport(ITypeBinding typeBinding, SimpleName ref) {
			if (!typeBinding.isTopLevel() && !typeBinding.isMember()) {
				return false; // no imports for anonymous, local, primitive types
			}
			int modifiers= typeBinding.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				return false; // imports for privates are not required
			}
			ITypeBinding currTypeBinding= Bindings.getBindingOfParentType(ref);
			if (currTypeBinding == null) {
				return false; // not in a type
			}
			if (!Modifier.isPublic(modifiers)) {
				if (!currTypeBinding.getPackage().getName().equals(typeBinding.getPackage().getName())) {
					return false; // not visible
				}
			}
			
			if (ref.getParent() instanceof TypeDeclaration) {
				return true;
			}
			
			if (typeBinding.isMember()) {
				IBinding[] visibleTypes= fAnalyzer.getDeclarationsInScope(ref, ScopeAnalyzer.TYPES);
				for (int i= 0; i < visibleTypes.length; i++) {
					if (visibleTypes[i] == typeBinding) {
						return false;
					}
				}
			}
			return true;				
		}
			
		
		/**
		 * Tries to find the given type name and add it to the import structure.
		 * Returns array of coices if user needs to select a type.
		 */
		public TypeInfo[] process(SimpleName ref) throws CoreException {
			String typeName= ref.getIdentifier();
			
			if (fImportsAdded.contains(typeName)) {
				return null;
			}
			
			try {
				IBinding binding= ref.resolveBinding();
				if (binding != null) {
					if (binding.getKind() == IBinding.TYPE) {
						ITypeBinding typeBinding= (ITypeBinding) binding;
						if (typeBinding.isArray()) {
							typeBinding= typeBinding.getElementType();
						}
						if (needsImport(typeBinding, ref)) {
							fImpStructure.addImport(typeBinding);
							fImportsAdded.add(typeName);
						}
					}	
					return null;
				}
				
				fImportsAdded.add(typeName);
						
				ArrayList typeRefsFound= fTypeRefsFound; // reuse
				
				findTypeRefs(typeName, typeRefsFound);				
				int nFound= typeRefsFound.size();
				if (nFound == 0) {
					// nothing found
					return null;
				} else if (nFound == 1) {
					TypeInfo typeRef= (TypeInfo) typeRefsFound.get(0);
					fImpStructure.addImport(typeRef.getFullyQualifiedName());
					return null;
				} else {
					String containerToImport= null;
					boolean ambiguousImports= false;
									
					// multiple found, use old import structure to find an entry
					for (int i= 0; i < nFound; i++) {
						TypeInfo typeRef= (TypeInfo) typeRefsFound.get(i);
						String fullName= typeRef.getFullyQualifiedName();
						String containerName= typeRef.getTypeContainerName();
						if (fOldSingleImports.contains(fullName)) {
							// was single-imported
							fImpStructure.addImport(fullName);
							return null;
						} else if (fOldDemandImports.contains(containerName)) {
							if (containerToImport == null) {
								containerToImport= containerName;
							} else {  // more than one import-on-demand
								ambiguousImports= true;
							}
						}
					}
					
					if (containerToImport != null && !ambiguousImports) {
						fImpStructure.addImport(containerToImport, typeName);
					} else {
						// return the open choices
						return (TypeInfo[]) typeRefsFound.toArray(new TypeInfo[nFound]);
					}
				}
			} finally {
				fTypeRefsFound.clear();
			}
			return null;
		}
		
		private void findTypeRefs(String simpleTypeName, Collection typeRefsFound) throws JavaModelException {
			if (fDoIgnoreLowerCaseNames && simpleTypeName.length() > 0 && Strings.isLowerCase(simpleTypeName.charAt(0))) {
				return;
			}
			TypeInfo[] infos= AllTypesCache.getTypesForName(simpleTypeName, fSearchScope, null);
			for (int i= 0; i < infos.length; i++) {
				TypeInfo curr= infos[i];
				IType type= curr.resolveType(fSearchScope);
				if (type != null && JavaModelUtil.isVisible(type, fCurrPackage)) {
					typeRefsFound.add(curr);
				}
			}
		}
	}	


	private Selection fRange;
	private ImportsStructure fImportsStructure;	
	private boolean fDoSave;
	
	private boolean fIgnoreLowerCaseNames;
	
	private IChooseImportQuery fChooseImportQuery;
	
	private int fNumberOfImportsAdded;

	private IProblem fParsingError;
	private CompilationUnit fASTRoot;

	public OrganizeImportsOperation(ImportsStructure impStructure, Selection range, boolean ignoreLowerCaseNames, boolean save, IChooseImportQuery chooseImportQuery) {
		super();
		fImportsStructure= impStructure;
		fRange= range;
		fDoSave= save;
		fIgnoreLowerCaseNames= ignoreLowerCaseNames;
		fChooseImportQuery= chooseImportQuery;

		fNumberOfImportsAdded= 0;
		
		fParsingError= null;
		ASTParser parser= ASTParser.newParser(AST.LEVEL_2_0);
		parser.setSource(impStructure.getCompilationUnit());
		parser.setResolveBindings(true);
		fASTRoot= (CompilationUnit) parser.createAST(null);
	}
	
	public OrganizeImportsOperation(ICompilationUnit cu, String[] importOrder, int importThreshold, boolean ignoreLowerCaseNames, boolean save, boolean doResolve, IChooseImportQuery chooseImportQuery) throws CoreException {
		this(new ImportsStructure(cu, importOrder, importThreshold, false), null, ignoreLowerCaseNames, save, chooseImportQuery);
	}
	
	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */	
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {

			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			ICompilationUnit cu= fImportsStructure.getCompilationUnit();
			fNumberOfImportsAdded= 0;
			
			monitor.beginTask(CodeGenerationMessages.getFormattedString("OrganizeImportsOperation.description", cu.getElementName()), 2); //$NON-NLS-1$

			ArrayList oldSingleImports= new ArrayList();
			ArrayList oldDemandImports= new ArrayList();
			
			Iterator references= findTypeReferences(oldSingleImports, oldDemandImports);
			if (references == null) {
				return;
			}
			
			int nOldImports= oldDemandImports.size() + oldSingleImports.size();
						
			oldDemandImports.add(""); //$NON-NLS-1$
			oldDemandImports.add("java.lang"); //$NON-NLS-1$
			oldDemandImports.add(cu.getParent().getElementName());			
			
			monitor.worked(1);
		
			TypeReferenceProcessor processor= new TypeReferenceProcessor(oldSingleImports, oldDemandImports, fASTRoot, fImportsStructure, fIgnoreLowerCaseNames);
			ArrayList openChoices= new ArrayList();
			ArrayList sourceRanges= new ArrayList();
			while (references.hasNext()) {
				SimpleName typeRef= (SimpleName) references.next();
				TypeInfo[] openChoice= processor.process(typeRef);
				if (openChoice != null) {
					openChoices.add(openChoice);
					sourceRanges.add(new SourceRange(typeRef.getStartPosition(), typeRef.getLength()));
				}	
			}
			
			processor= null;
			
			if (openChoices.size() > 0 && fChooseImportQuery != null) {
				TypeInfo[][] choices= (TypeInfo[][]) openChoices.toArray(new TypeInfo[openChoices.size()][]);
				ISourceRange[] ranges= (ISourceRange[]) sourceRanges.toArray(new ISourceRange[sourceRanges.size()]);
				TypeInfo[] chosen= fChooseImportQuery.chooseImports(choices, ranges);
				if (chosen == null) {
					// cancel pressed by the user
					throw new OperationCanceledException();
				}
				for (int i= 0; i < chosen.length; i++) {
					TypeInfo typeInfo= chosen[i];
					fImportsStructure.addImport(typeInfo.getFullyQualifiedName());
				}				
			}
			fImportsStructure.create(fDoSave, new SubProgressMonitor(monitor, 1));
			
			fNumberOfImportsAdded= fImportsStructure.getNumberOfImportsCreated() - nOldImports;
		} finally {
			monitor.done();
		}
	}
	
	private boolean isAffected(IProblem problem) {
		return fRange == null || (fRange.getOffset() <= problem.getSourceEnd() && fRange.getExclusiveEnd() > problem.getSourceStart());
	}

	
	// find type references in a compilation unit
	private Iterator findTypeReferences(ArrayList oldSingleImports, ArrayList oldDemandImports) {
		IProblem[] problems= fASTRoot.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			if (curr.isError() && (curr.getID() & IProblem.Syntax) != 0 && isAffected(curr)) {
				fParsingError= problems[i];
				return null;
			}
		}
		ArrayList result= new ArrayList();
		TypeRefASTVisitor visitor = new TypeRefASTVisitor(fRange, result, oldSingleImports, oldDemandImports);
		fASTRoot.accept(visitor);

		return result.iterator();
	}	
	
	/**
	 * After executing the operation, returns <code>null</code> if the operation has been executed successfully or
	 * the range where parsing failed. 
	 */
	public IProblem getParseError() {
		return fParsingError;
	}
	
	public int getNumberOfImportsAdded() {
		return fNumberOfImportsAdded;
	}
	
}
