package org.eclipse.jdt.internal.ui.codemanipulation;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.TypeInfo;


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
				
	private static class TypeReferenceProcessor {
		
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;
		
		private ImportsStructure fImpStructure;
				
		private ArrayList fTypeRefsFound;
		private ArrayList fAllTypes;
		
		public TypeReferenceProcessor(ArrayList oldSingleImports, ArrayList oldDemandImports, ImportsStructure impStructure, IProgressMonitor monitor) {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
					
			fTypeRefsFound= new ArrayList();  	// cached array list for reuse			
			
			fAllTypes= new ArrayList(500);
			IProject project= impStructure.getCompilationUnit().getJavaProject().getProject();
			fetchAllTypes(fAllTypes, project, monitor);
		}
	
		
		private void fetchAllTypes(ArrayList list, IProject project, IProgressMonitor monitor) {
			IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IResource[] { project });		
			searchScope.setIncludesBinaries(true);
			AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(project.getWorkspace());
			searchEngine.searchTypes(list, searchScope, IJavaElementSearchConstants.CONSIDER_TYPES, monitor);
		}
		
		
		/**
		 * Tries to find the given type name and add it to the import structure.
		 * Returns array of coices if user needs to select a type.
		 */
		public TypeInfo[] process(String typeName) throws CoreException {
			try {
				ArrayList typeRefsFound= fTypeRefsFound; // reuse
						
				findTypeRefs(typeName, typeRefsFound);				
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
			}
			return null;
		}

		private void findTypeRefs(String simpleTypeName, ArrayList typeRefsFound) {
			for (int i= fAllTypes.size() - 1; i >= 0; i--) {
				TypeInfo curr= (TypeInfo) fAllTypes.get(i);
				if (simpleTypeName.equals(curr.getTypeName())) {
					typeRefsFound.add(curr);
				}
			}
		}
		
	}
	
	
	private static class ParsingError extends Error {
		private IProblem fProblem;
		
		public ParsingError(IProblem problem) {
			fProblem= problem;
		}

		public IProblem getProblem() {
			return fProblem;
		}
	}
	
	private static class TypeReferenceRequestor implements ISourceElementRequestor {
		
		private static class RefSourceRange implements ISourceRange {
			private int fOffset;
			private int fLength;
			
			public RefSourceRange(int offset, int length) {
				fOffset= offset;
				fLength= length;
			}
			
			public int getOffset() { 
				return fOffset;
			}
			
			public int getLength() {
				return fLength;
			}
		}
		
		private HashMap fTypeRefs;
		
		private int fImportEnd;
		
		public TypeReferenceRequestor(HashMap result, int importEndPos) {
			fTypeRefs= result;
			fImportEnd= importEndPos;
		}
		
		public void acceptProblem(IProblem problem) {
			if (problem.isError()) {
				throw new ParsingError(problem);
			}
		}
		
		public void acceptFieldReference(char[] fieldName, int sourcePosition) {}
		public void acceptInitializer(int modifiers, int declarationSourceStart, int declarationSourceEnd) {}
		public void acceptLineSeparatorPositions(int[] positions) {}
		public void acceptMethodReference(char[] methodName, int argCount, int sourcePosition) {}
		public void acceptPackage(int declarationStart, int declarationEnd, char[] name) {}
		
		public void enterClass(int declarationStart, int modifiers, char[] name, int nameSourceStart, int nameSourceEnd, char[] superclass, char[][] superinterfaces) {}
		public void enterCompilationUnit() {}
		public void enterConstructor(int declarationStart, int modifiers, char[] name, int nameSourceStart, int nameSourceEnd, char[][] parameterTypes, char[][] parameterNames, char[][] exceptionTypes) {}
		public void enterField(int declarationStart, int modifiers, char[] type, char[] name, int nameSourceStart, int nameSourceEnd) {}
		public void enterInterface(int declarationStart, int modifiers, char[] name, int nameSourceStart, int nameSourceEnd, char[][] superinterfaces) {}
		public void enterMethod(int declarationStart, int modifiers, char[] returnType, char[] name, int nameSourceStart, int nameSourceEnd, 
			char[][] parameterTypes, char[][] parameterNames, char[][] exceptionTypes) {} 
		public void exitClass(int declarationEnd) {}
		public void exitCompilationUnit(int declarationEnd) {}
		public void exitConstructor(int declarationEnd) {}
		public void exitField(int declarationEnd) {}
		public void exitInterface(int declarationEnd) {}
		public void exitMethod(int declarationEnd) {}
		public void enterInitializer(int declarationStart, int modifiers) {}
		public void exitInitializer(int declarationEnd) {}		
				
		public void acceptConstructorReference(char[] typeName, int argCount, int sourcePosition) {
			typeRefFound(typeName, sourcePosition);
		}

		public void acceptTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
			if (typeName.length > 0) {
				typeRefFound(typeName[0], sourceStart);
			}
		}
		
		public void acceptTypeReference(char[] typeName, int sourcePosition) {
			typeRefFound(typeName, sourcePosition);
		}
	
		public void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd) {
			if (name.length > 0) {
				typeRefFound(name[0], sourceStart);
			}
		}
	
		public void acceptUnknownReference(char[] name, int sourcePosition) {
			typeRefFound(name, sourcePosition);
		}	
	
		public void acceptImport(int declarationStart, int declarationEnd, char[] name, boolean onDemand) {
		}
		
		private void typeRefFound(char[] typeRef, int pos) {
			if (pos >= fImportEnd) {
				int len= getIdLen(typeRef);
				String name= new String(typeRef, 0, len);
				if (!fTypeRefs.containsKey(name)) {
					fTypeRefs.put(name, new RefSourceRange(pos, len));
				}
			}
		}
		
		private int getIdLen(char[] str) {
			for (int i= 0; i < str.length; i++) {
				if (str[i] == '.') {
					return i;
				}
			}
			return str.length;
		}
	}
	
	// find type references in a compilation unit
	private static HashMap findTypeReferences(ICompilationUnit cu) throws JavaModelException, ParsingError {
		HashMap references= new HashMap();
		int inportEndPos= 0;
		IImportContainer importContainer= cu.getImportContainer();
		if (importContainer.exists()) {
			ISourceRange importRange= importContainer.getSourceRange();
			inportEndPos= importRange.getOffset() + importRange.getLength();
		}		
	
		TypeReferenceRequestor requestor= new TypeReferenceRequestor(references, inportEndPos);
		SourceElementParser parser= new SourceElementParser(requestor, new DefaultProblemFactory());
		if (cu instanceof org.eclipse.jdt.internal.compiler.env.ICompilationUnit) {
			parser.parseCompilationUnit((org.eclipse.jdt.internal.compiler.env.ICompilationUnit)cu, true);
		}	
		return references;
	}
	

	private ICompilationUnit fCompilationUnit;	
	private IImportDeclaration[] fCreatedImports;
	private boolean fDoSave;
	
	private String[] fOrderPreference;
	private int fImportThreshold;
	
	private IChooseImportQuery fChooseImportQuery;
	
	// return variable: set to null if no error
	private IProblem fParsingError;	
		
	public OrganizeImportsOperation(ICompilationUnit cu, String[] importOrder, int importThreshold, boolean save, IChooseImportQuery chooseImportQuery) {
		super();
		fCompilationUnit= cu;
		fDoSave= save;
		
		fImportThreshold= importThreshold;
		fOrderPreference= importOrder;
		fChooseImportQuery= chooseImportQuery;
		
		fParsingError= null;
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
			fCreatedImports= null;
			fParsingError= null;
			
			monitor.beginTask(CodeManipulationMessages.getString("OrganizeImportsOperation.description"), 3); //$NON-NLS-1$
			
			HashMap references;
			try {
				references= findTypeReferences(fCompilationUnit);
			} catch (ParsingError e) {
				fParsingError= e.getProblem();
				throw new OperationCanceledException();
			}
							
			IImportDeclaration[] decls= fCompilationUnit.getImports();
			
			// build up old imports structures
			ArrayList oldSingleImports= new ArrayList(decls.length);
			ArrayList oldDemandImports= new ArrayList(decls.length);
			for (int i= 0; i < decls.length; i++) {
				IImportDeclaration curr= decls[i];
				if (curr.isOnDemand()) {
					String packName= Signature.getQualifier(curr.getElementName());
					oldDemandImports.add(packName);
				} else {
					oldSingleImports.add(curr.getElementName());
				}			
			}
			oldDemandImports.add(""); //$NON-NLS-1$
			oldDemandImports.add("java.lang"); //$NON-NLS-1$
			oldDemandImports.add(fCompilationUnit.getParent().getElementName());			
			
			monitor.worked(1);

			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, fOrderPreference, fImportThreshold);
			
			TypeReferenceProcessor processor= new TypeReferenceProcessor(oldSingleImports, oldDemandImports, impStructure, new SubProgressMonitor(monitor, 1));
			ArrayList openChoices= new ArrayList();
			ArrayList sourceRanges= new ArrayList();
			Iterator iter= references.keySet().iterator();
			while (iter.hasNext()) {
				String typeName= (String) iter.next();
				TypeInfo[] openChoice= processor.process(typeName);
				if (openChoice != null) {
					openChoices.add(openChoice);
					sourceRanges.add(references.get(typeName));
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
						
			fCreatedImports= impStructure.create(fDoSave, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Can be called after executing to get the created imports
	 * Return null if the compilation unit had a compilation error and
	 * no imports were added
	 */
	public IImportDeclaration[] getCreatedImports() {
		return fCreatedImports;
	}
	/**
	 * After executing the operation, ask if a compilation error prevented
	 * the resolving of types. Returns <code>null</code> if no error occured.
	 */
	public IProblem getParsingError() {
		return fParsingError;
	}

}
