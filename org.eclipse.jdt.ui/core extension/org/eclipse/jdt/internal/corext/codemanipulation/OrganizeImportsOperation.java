package org.eclipse.jdt.internal.corext.codemanipulation;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.TypeInfo;



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
	
	private static class ASTError extends Error {
		
		public ASTNode errorNode;
		
		public ASTError(ASTNode node) {
			errorNode= node;
		}
	}
	
	private static class TypeRefVisitor extends ASTVisitor {

		private HashMap fReferences;
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;

		public TypeRefVisitor(HashMap references, ArrayList oldSingleImports, ArrayList oldDemandImports) {
			fReferences= references;
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
		}
		
		private void addReference(SimpleName name) {
			String identifier= name.getIdentifier();
			if (!fReferences.containsKey(identifier)) {
				fReferences.put(identifier, name);
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
		
		private void visitChildren(List elements) {
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				((ASTNode) elements.get(i)).accept(this);
			}
		}
		
		/*
		 * @see ASTVisitor#visit(ArrayType)
		 */
		public boolean visit(ArrayType node) {
			node.getElementType().accept(this);
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
			if (node.isOnDemand()) {
				fOldDemandImports.add(node.getName());
			} else {
				fOldSingleImports.add(node.getName());
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
			visitChildren(node.arguments());
			return false;
		}

		/*
		 * @see ASTVisitor#endVisit(MethodInvocation)
		 */
		public boolean visit(MethodInvocation node) {
			evalQualifyingExpression(node.getExpression());
			visitChildren(node.arguments());
			return false;
		}

		/*
		 * @see ASTVisitor#visit(SuperConstructorInvocation)
		 */		
		public boolean visit(SuperConstructorInvocation node) {
			evalQualifyingExpression(node.getExpression());
			visitChildren(node.arguments());
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
			typeRefFound(node.getSuperclass());

			Iterator iter= node.superInterfaces().iterator();
			while (iter.hasNext()) {
				typeRefFound((Name) iter.next());
			}
			visitChildren(node.bodyDeclarations());
			return false;
		}
		
		/*
		 * @see ASTVisitor#preVisit(ASTNode)
		 */		
		public void preVisit(ASTNode node) {
			if ((node.getFlags() & ASTNode.MALFORMED) != 0) {
				throw new ASTError(node);
			}
		}
	}
		
	private static class TypeReferenceProcessor {
		
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;
		
		private ImportsStructure fImpStructure;
				
		private ArrayList fTypeRefsFound; // cached array list for reuse
		private ArrayList fNamesFound; // cached array list for reuse
		
		private ArrayList fAllTypes;
		private boolean fIgnoreLowerCaseNames;
		
		
		public TypeReferenceProcessor(ArrayList oldSingleImports, ArrayList oldDemandImports, ImportsStructure impStructure, boolean ignoreLowerCaseNames) throws JavaModelException {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
			fIgnoreLowerCaseNames= ignoreLowerCaseNames;
					
			fTypeRefsFound= new ArrayList();  	// cached array list for reuse
			fNamesFound= new ArrayList();  	// cached array list for reuse		
			
			fAllTypes= null;
		}
		
		private ArrayList getAllTypes() throws JavaModelException {
			if (fAllTypes == null) {
				fAllTypes= new ArrayList(500);
				
				IJavaProject project= fImpStructure.getCompilationUnit().getJavaProject();
				IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaProject[] { project });		
				AllTypesCache.getTypes(searchScope, IJavaSearchConstants.TYPE, null, fAllTypes);
			}
			return fAllTypes;
		}
		
		/**
		 * Tries to find the given type name and add it to the import structure.
		 * Returns array of coices if user needs to select a type.
		 */
		public TypeInfo[] process(SimpleName ref) throws CoreException {
			try {
				IBinding binding= ref.resolveBinding();
				if (binding != null) {
					if (binding.getKind() == binding.TYPE) {
						ITypeBinding typeBinding= (ITypeBinding) binding;
						if (typeBinding.isArray()) {
							typeBinding= typeBinding.getElementType();
						}
						if (typeBinding.isTopLevel() || typeBinding.isMember()) {
							String name= Bindings.getFullyQualifiedName(typeBinding);
							fImpStructure.addImport(name);
						}
					}
					return null;
				}
				
				ArrayList typeRefsFound= fTypeRefsFound; // reuse
				String typeName= ref.getIdentifier();
				findTypeRefs(typeName, typeRefsFound, fNamesFound);				
				int nFound= typeRefsFound.size();
				if (nFound == 0) {
					// nothing found
					return null;
				} else if (nFound == 1) {
					TypeInfo typeRef= (TypeInfo) typeRefsFound.get(0);
					fImpStructure.addImport(typeRef.getTypeContainerName(), typeRef.getTypeName());
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
							fImpStructure.addImport(containerName, typeRef.getTypeName());
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
				fNamesFound.clear();
			}
			return null;
		}

		private void findTypeRefs(String simpleTypeName, ArrayList typeRefsFound, ArrayList namesFound) throws JavaModelException {
			if (fIgnoreLowerCaseNames && simpleTypeName.length() > 0 && Character.isLowerCase(simpleTypeName.charAt(0))) {
				return;
			}
			ArrayList allTypes= getAllTypes();
			
			for (int i= allTypes.size() - 1; i >= 0; i--) {
				TypeInfo curr= (TypeInfo) allTypes.get(i);
				if (simpleTypeName.equals(curr.getTypeName())) {
					String fullyQualifiedName= curr.getFullyQualifiedName();
					if (!namesFound.contains(fullyQualifiedName)) {
						namesFound.add(fullyQualifiedName);
						typeRefsFound.add(curr);
					}
				}
			}
		}
	}	

	private ICompilationUnit fCompilationUnit;	
	private boolean fDoSave;
	
	private String[] fOrderPreference;
	private int fImportThreshold;
	private boolean fIgnoreLowerCaseNames;
	
	private IChooseImportQuery fChooseImportQuery;
	
	private int fNumberOfImportsAdded;

	private ISourceRange fParsingErrorRange;

	private boolean fDoResolve;
		
	public OrganizeImportsOperation(ICompilationUnit cu, String[] importOrder, int importThreshold, boolean ignoreLowerCaseNames, boolean save, boolean doResolve, IChooseImportQuery chooseImportQuery) {
		super();
		fCompilationUnit= cu;
		fDoSave= save;
		fDoResolve= doResolve;
		
		fImportThreshold= importThreshold;
		fOrderPreference= importOrder;
		fIgnoreLowerCaseNames= ignoreLowerCaseNames;
		fChooseImportQuery= chooseImportQuery;

		fNumberOfImportsAdded= 0;
		
		fParsingErrorRange= null;
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
			fNumberOfImportsAdded= 0;
			
			monitor.beginTask(CodeGenerationMessages.getFormattedString("OrganizeImportsOperation.description", fCompilationUnit.getElementName()), 2); //$NON-NLS-1$

			ArrayList oldSingleImports= new ArrayList();
			ArrayList oldDemandImports= new ArrayList();
			
			Iterator references= findTypeReferences(oldSingleImports, oldDemandImports);
			if (references == null) {
				return;
			}
			
			
			int nOldImports= oldDemandImports.size() + oldSingleImports.size();
						
			oldDemandImports.add(""); //$NON-NLS-1$
			oldDemandImports.add("java.lang"); //$NON-NLS-1$
			oldDemandImports.add(fCompilationUnit.getParent().getElementName());			
			
			monitor.worked(1);

			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, fOrderPreference, fImportThreshold, false);
			
			TypeReferenceProcessor processor= new TypeReferenceProcessor(oldSingleImports, oldDemandImports, impStructure, fIgnoreLowerCaseNames);
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
			
			if (openChoices.size() > 0) {
				TypeInfo[][] choices= (TypeInfo[][]) openChoices.toArray(new TypeInfo[openChoices.size()][]);
				ISourceRange[] ranges= (ISourceRange[]) sourceRanges.toArray(new ISourceRange[sourceRanges.size()]);
				TypeInfo[] chosen= fChooseImportQuery.chooseImports(choices, ranges);
				if (chosen == null) {
					// cancel pressed by the user
					throw new OperationCanceledException();
				}
				for (int i= 0; i < chosen.length; i++) {
					TypeInfo typeInfo= chosen[i];
					impStructure.addImport(typeInfo.getPackageName(), typeInfo.getEnclosingName(), typeInfo.getTypeName());
				}				
			}
			impStructure.create(fDoSave, new SubProgressMonitor(monitor, 1));
			
			fNumberOfImportsAdded= impStructure.getNumberOfImportsCreated() - nOldImports;
		} finally {
			monitor.done();
		}
	}
	
	// find type references in a compilation unit
	private Iterator findTypeReferences(ArrayList oldSingleImports, ArrayList oldDemandImports) throws JavaModelException {
		HashMap references= new HashMap();		

		try {
			CompilationUnit astRoot= AST.parseCompilationUnit(fCompilationUnit, fDoResolve);

			TypeRefVisitor visitor = new TypeRefVisitor(references, oldSingleImports, oldDemandImports);
			astRoot.accept(visitor);

			return references.values().iterator();
		} catch (ASTError e) {
			fParsingErrorRange= new SourceRange(e.errorNode.getStartPosition(), e.errorNode.getLength());
		}
		return null;
	}	
	
	/**
	 * After executing the operation, returns <code>null</code> if the operation has been executed successfully or
	 * the range where parsing failed. 
	 */
	public ISourceRange getErrorSourceRange() {
		return fParsingErrorRange;
	}
	
	public int getNumberOfImportsAdded() {
		return fNumberOfImportsAdded;
	}
	
}