/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.util.AllTypesSearchEngine;
import org.eclipse.jdt.internal.ui.util.TypeRef;
import org.eclipse.jdt.internal.ui.util.TypeRefLabelProvider;


public class OrganizeImportsOperation extends WorkspaceModifyOperation {
	
	private ICompilationUnit fCompilationUnit;	
	private IImportDeclaration[] fCreatedImports;
	private boolean fDoSave;
	private boolean fHasCompilationErrors;
				
	public OrganizeImportsOperation(ICompilationUnit cu, boolean save) {
		super();
		fCompilationUnit= cu;
		fDoSave= save;
		fCreatedImports= null;
		fHasCompilationErrors= false;
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
	 * the resolving of types
	 */
	public boolean hasCompilationErrors() {
		return fHasCompilationErrors;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(CodeManipulationMessages.getString("OrganizeImportsOperation.description"), 3); //$NON-NLS-1$
			
			String[] references= findTypeReferences(fCompilationUnit);
			if (references == null) {
				// there was a compilation error
				fCreatedImports= null;
				fHasCompilationErrors= true;
				return;
			}
			fHasCompilationErrors= false;
							
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

			// add empty package symbols to get the preferred import order
			String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
			int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();
			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold);
			
			ArrayList openChoices= new ArrayList();
			
			TypeRefProcessor processor= new TypeRefProcessor(oldSingleImports, oldDemandImports, impStructure, new SubProgressMonitor(monitor, 1));
			for (int i= 0; i < references.length; i++) {
				TypeRef[] ret= processor.process(references[i]);
				if (ret != null) {
					openChoices.add(ret);
				}
			}
			processor= null;
			
			if (openChoices.size() > 0) {
				TypeRef[][] choisesArray= new TypeRef[openChoices.size()][];
				openChoices.toArray(choisesArray);
				
				if (!openChoicesSelections(choisesArray, impStructure)) {
					// cancel press by the user
					return;
				}
			}
						
			fCreatedImports= impStructure.create(fDoSave, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}
	
	private boolean openChoicesSelections(TypeRef[][] openChoices, ImportsStructure impStructure) {
		ILabelProvider labelProvider= new TypeRefLabelProvider(TypeRefLabelProvider.SHOW_FULLYQUALIFIED);
		
		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider, true, false);
		dialog.setTitle(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.message")); //$NON-NLS-1$
		dialog.setPageInfoMessage(CodeManipulationMessages.getString("OrganizeImportsOperation.dialog.pageinfo")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		
		if (dialog.open() == dialog.OK) {
			Object[] result= dialog.getResult();
			for (int i= 0; i < result.length; i++) {
				List types= (List) result[i];
				if (!types.isEmpty()) {
					TypeRef typeRef= (TypeRef) types.get(0);
					impStructure.addImport(typeRef.getPackageName(), typeRef.getEnclosingName(), typeRef.getTypeName());
				}
			}				
			return true;
		} else {
			return false;
		}
	}
	
	private static class TypeRefProcessor {
		
		private ArrayList fOldSingleImports;
		private ArrayList fOldDemandImports;
		
		private ImportsStructure fImpStructure;
				
		private ArrayList fTypeRefsFound;
		private ArrayList fAllTypes;
		
		public TypeRefProcessor(ArrayList oldSingleImports, ArrayList oldDemandImports, ImportsStructure impStructure, IProgressMonitor monitor) {
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
			AllTypesSearchEngine searchEngine= new AllTypesSearchEngine(project.getWorkspace());
			searchEngine.searchTypes(list, searchScope, IJavaElementSearchConstants.CONSIDER_TYPES, monitor);
		}
		
		
		public TypeRef[] process(String typeName) throws JavaModelException, CoreException {
			try {
				ArrayList typeRefsFound= fTypeRefsFound; // reuse
			
				findTypeRefs(typeName, typeRefsFound);				
				int nFound= typeRefsFound.size();
				if (nFound == 0) {
					// nothing found
					return null;
				} else if (nFound == 1) {
					TypeRef typeRef= (TypeRef) typeRefsFound.get(0);
					fImpStructure.addImport(typeRef.getTypeContainerName(), typeRef.getTypeName());
					return null;
				} else {
					String containerToImport= null;
					boolean ambiguousImports= false;
									
					// multiple found, use old import structure to find an entry
					for (int i= 0; i < nFound; i++) {
						TypeRef typeRef= (TypeRef) typeRefsFound.get(i);
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
						return null;
					} else {
						return (TypeRef[]) typeRefsFound.toArray(new TypeRef[nFound]);
					}
				}
			} finally {
				fTypeRefsFound.clear();
			}
		}

		private void findTypeRefs(String simpleTypeName, ArrayList typeRefsFound) {
			for (int i= fAllTypes.size() - 1; i >= 0; i--) {
				TypeRef curr= (TypeRef) fAllTypes.get(i);
				if (simpleTypeName.equals(curr.getTypeName())) {
					typeRefsFound.add(curr);
				}
			}
		}
		
	}
	
	
	// find type references in a compilation unit
	
	private static String[] findTypeReferences(ICompilationUnit cu) {
		ArrayList references= new ArrayList();
		
		TypeReferenceRequestor requestor= new TypeReferenceRequestor(references);
		SourceElementParser parser= new SourceElementParser(requestor, new ParserProblemFactory());
		if (cu instanceof org.eclipse.jdt.internal.compiler.env.ICompilationUnit) {
			try {
				parser.parseCompilationUnit((org.eclipse.jdt.internal.compiler.env.ICompilationUnit)cu, true);
			} catch (ParserError e) {
				return null;
			}
		}	
		String[] res= new String[references.size()];
		references.toArray(res);
		return res;
	}
	
	private static class ParserError extends Error {
	}
	
	private static class ParserProblemFactory implements IProblemFactory {
		public IProblem createProblem(char[] originatingFileName, int problemId, String[] arguments, int severity, int startPosition, int endPosition, int lineNumber) {
			throw new ParserError();
		}
		
		public Locale getLocale() {
			return Locale.getDefault();
		}
		
		public String getLocalizedMessage(int problemId, String[] problemArguments) {
			return "" + problemId; //$NON-NLS-1$
		}
	}	
	
	private static class TypeReferenceRequestor implements ISourceElementRequestor {
		private ArrayList fTypeRefs;
		private ArrayList fPositions;
		
		private int fImportStart;
		private int fImportEnd;
		
		public TypeReferenceRequestor(ArrayList references) {
			fTypeRefs= references;
			fPositions= new ArrayList(references);
		}
		
		public void acceptFieldReference(char[] fieldName, int sourcePosition) {}
		public void acceptInitializer(int modifiers, int declarationSourceStart, int declarationSourceEnd) {}
		public void acceptLineSeparatorPositions(int[] positions) {}
		public void acceptMethodReference(char[] methodName, int argCount, int sourcePosition) {}
		public void acceptPackage(int declarationStart, int declarationEnd, char[] name) {}
		public void acceptProblem(IProblem problem) {}
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
	
		
}
