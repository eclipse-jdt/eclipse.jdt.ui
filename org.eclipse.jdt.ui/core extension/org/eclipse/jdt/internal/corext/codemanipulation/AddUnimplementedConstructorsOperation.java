/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * Evaluates unimplemented constructors for the superclass.
 * If the type is open in an editor, be sure to pass over the types working working copy.
 */
public class AddUnimplementedConstructorsOperation implements IWorkspaceRunnable {

	private IType fType;
	private IMethod[] fCreatedMethods;
	private boolean fDoSave;
	private CodeGenerationSettings fSettings;
	
	public AddUnimplementedConstructorsOperation(IType type, CodeGenerationSettings settings, boolean save) {
		super();
		fType= type;
		fDoSave= save;
		fCreatedMethods= null;
		fSettings= settings;
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
			monitor.setTaskName(CodeGenerationMessages.getString("AddUnimplementedMethodsOperation.description")); //$NON-NLS-1$
			monitor.beginTask("", 3); //$NON-NLS-1$
			
			ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			monitor.worked(1);
			IType superType= hierarchy.getSuperclass(fType);
			if (superType == null) {
				fCreatedMethods= new IMethod[0];
				return;
			}
							
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit(), fSettings.importOrder, fSettings.importThreshold, true);
			String[] toImplement= StubUtility.evalConstructors(fType, superType, fSettings, imports);
			if (toImplement == null) {
				throw new OperationCanceledException();
			}
			
			int nToImplement= toImplement.length;
			ArrayList createdMethods= new ArrayList(nToImplement);
			
			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;
			
			IMethod sibling= null;
			IMethod[] existing= fType.getMethods();
			for (int i= 0; i < existing.length; i++) {
				if (existing[i].isConstructor()) {
					sibling= existing[i];
				}
			}
			if (sibling == null && existing.length > 0) {
				sibling= existing[0];
			}
		
			for (int i= 0; i < nToImplement; i++) {
				String formattedContent= StubUtility.codeFormat(toImplement[i], indent, lineDelim) + lineDelim;
				IMethod lastMethod= fType.createMethod(formattedContent, sibling, true, null);
				createdMethods.add(lastMethod);
			}
			monitor.worked(1);	

			imports.create(fDoSave, null);
			monitor.worked(1);

			fCreatedMethods= new IMethod[createdMethods.size()];
			createdMethods.toArray(fCreatedMethods);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the created accessors. To be called after a sucessful run.
	 */	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
		
}
