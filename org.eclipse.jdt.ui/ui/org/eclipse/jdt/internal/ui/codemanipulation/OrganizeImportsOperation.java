package org.eclipse.jdt.internal.ui.codemanipulation;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

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
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeInfo[] chooseImports(TypeInfo[][] openChoices);
	}
				
	private static class TypeReferenceProcessor {
		
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;
		
		private ImportsStructure fImpStructure;
				
		private ArrayList fTypeRefsFound;
		private ArrayList fAllTypes;
		
		private ArrayList fOpenChoices;
		
		public TypeReferenceProcessor(ArrayList oldSingleImports, ArrayList oldDemandImports, ImportsStructure impStructure, IProgressMonitor monitor) {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
						
			fTypeRefsFound= new ArrayList();  	// cached array list for reuse			
			
			fAllTypes= new ArrayList(500);
			IProject project= impStructure.getCompilationUnit().getJavaProject().getProject();
			fetchAllTypes(fAllTypes, project, monitor);
			
			fOpenChoices= new ArrayList();
		}
		
		public TypeInfo[][] getOpenChoices() {
			return (TypeInfo[][]) fOpenChoices.toArray(new TypeInfo[fOpenChoices.size()][]);
		}
		
		
		private void fetchAllTypes(ArrayList list, IProject project, IProgressMonitor monitor) {
			IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IResource[] { project });		
			AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(project.getWorkspace());
			searchEngine.searchTypes(list, searchScope, IJavaElementSearchConstants.CONSIDER_TYPES, monitor);
		}
		
		
		public void process(String typeName) throws CoreException {
			try {
				ArrayList typeRefsFound= fTypeRefsFound; // reuse
			
				findTypeRefs(typeName, typeRefsFound);				
				int nFound= typeRefsFound.size();
				if (nFound == 0) {
					// nothing found
					return;
				} else if (nFound == 1) {
					TypeInfo typeRef= (TypeInfo) typeRefsFound.get(0);
					fImpStructure.addImport(typeRef.getTypeContainerName(), typeRef.getTypeName());
					return;
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
							return;
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
						// remember. Need to ask the client
						fOpenChoices.add(typeRefsFound.toArray(new TypeInfo[nFound]));
					}
				}
			} finally {
				fTypeRefsFound.clear();
			}
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
		private ArrayList fTypeRefs;
		private ArrayList fPositions;
		
		private ISourceRange fImportRange;
		
		public TypeReferenceRequestor(ArrayList references, ISourceRange importRange) {
			fTypeRefs= references;
			fImportRange= importRange;
			fPositions= new ArrayList(references);
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
			for (int i= 0; i < typeName.length - 1; i++) {
				unknownRefFound(typeName[i], sourceStart);
			}
			typeRefFound(typeName[typeName.length - 1], sourceStart);
		}
		
		public void acceptTypeReference(char[] typeName, int sourcePosition) {
			typeRefFound(typeName, sourcePosition);
		}
	
		public void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd) {
			for (int i= 0; i < name.length; i++) {
				unknownRefFound(name[i], sourceStart);
			}
		}
	
		public void acceptUnknownReference(char[] name, int sourcePosition) {
			unknownRefFound(name, sourcePosition);
		}	
		
		private void typeRefFound(char[] typeRef, int pos) {
			String typeRefName= new String(typeRef);
			int idx= fTypeRefs.indexOf(typeRefName);
			if (idx == -1) {
				fTypeRefs.add(typeRefName);
				fPositions.add(new Integer(pos));
			} else {
				fPositions.set(idx, new Integer(pos));
			}
		}
			
		private void unknownRefFound(char[] unknownRef, int pos) {
			if (unknownRef.length > 0) {
				if (Character.isUpperCase(unknownRef[0])) {
					typeRefFound(unknownRef, pos);
				}
			}			
		}
		
		public void acceptImport(int declarationStart, int declarationEnd, char[] name, boolean onDemand) {
			// remove all type references that came from the import statements
			for (int i= fTypeRefs.size()-1; i >= 0; i--) {
				int pos= ((Integer)fPositions.get(i)).intValue();
				if (declarationStart <= pos && declarationEnd > pos) {
					fTypeRefs.remove(i);
					fPositions.remove(i);
				}
			}
		}


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
			
			String[] references;
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
			for (int i= 0; i < references.length; i++) {
				processor.process(references[i]);
			}
			TypeInfo[][] openChoices= processor.getOpenChoices();
			processor= null;
			
			if (openChoices.length > 0) {
				TypeInfo[] chosen= fChooseImportQuery.chooseImports(openChoices);
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
	
	// find type references in a compilation unit
	private static String[] findTypeReferences(ICompilationUnit cu) throws JavaModelException, ParsingError {
		ArrayList references= new ArrayList();
		IImportContainer importContainer= cu.getImportContainer();
		ISourceRange importRange= null;
		if (importContainer.exists()) {
			importRange= importContainer.getSourceRange();
		}
		
		TypeReferenceRequestor requestor= new TypeReferenceRequestor(references, importRange);
		SourceElementParser parser= new SourceElementParser(requestor, new DefaultProblemFactory());
		if (cu instanceof org.eclipse.jdt.internal.compiler.env.ICompilationUnit) {
			parser.parseCompilationUnit((org.eclipse.jdt.internal.compiler.env.ICompilationUnit)cu, true);
		}	
		String[] res= new String[references.size()];
		references.toArray(res);
		return res;
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
