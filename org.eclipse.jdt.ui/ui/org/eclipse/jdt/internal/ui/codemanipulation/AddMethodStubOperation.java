/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

/**
 * Add method stubs to a type (the parent type)
 * Methods are added without checking if they already exist (will result in duplicated methods)
 * If the parent type is open in an editor, be sure to pass over its working copy.
 */
public class AddMethodStubOperation extends WorkspaceModifyOperation {
		
	private IType fType;
	private IMethod[] fMethods;
	private IMethod[] fCreatedMethods;
	private boolean fDoSave;
		
	private IRequestQuery fOverrideQuery;
	private IRequestQuery fReplaceQuery;
	
	private boolean fOverrideAll;
	private boolean fReplaceAll;
	
	public AddMethodStubOperation(IType type, IMethod[] methods, IRequestQuery overrideQuery, IRequestQuery replaceQuery, boolean save) {
		super();
		fType= type;
		fMethods= methods;
		fCreatedMethods= null;
		fDoSave= save;
		fOverrideQuery= overrideQuery;
		fReplaceQuery= replaceQuery;
	}
	
	private boolean queryOverrideFinalMethods(IMethod inheritedMethod) throws InterruptedException {
		if (!fOverrideAll) {
			switch (fOverrideQuery.doQuery(inheritedMethod)) {
				case IRequestQuery.CANCEL:
					throw new InterruptedException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fOverrideAll= true;
			}
		}
		return true;
	}
	
	private boolean queryReplaceMethods(IMethod method) throws InterruptedException {
		if (!fReplaceAll) {
			switch (fReplaceQuery.doQuery(method)) {
				case IRequestQuery.CANCEL:
					throw new InterruptedException();
				case IRequestQuery.NO:
					return false;
				case IRequestQuery.YES_ALL:
					fReplaceAll= true;
			}
		}
		return true;
	}	
	
	public void execute(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(CodeManipulationMessages.getString("AddMethodStubOperation.description"), fMethods.length + 2); //$NON-NLS-1$

			fOverrideAll= (fOverrideQuery == null);
			fReplaceAll= (fReplaceQuery == null);

			List existingMethods= Arrays.asList(fType.getMethods());
			
			ArrayList createdMethods= new ArrayList();
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit());
			
			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;
			
			ITypeHierarchy typeHierarchy= fType.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1));
			
			for (int i= 0; i < fMethods.length; i++) {
				try {
					String content;
					IMethod curr= fMethods[i];
					if (StubUtility.findMethod(curr, createdMethods) != null) {
						// ignore duplicated methods
						continue;
					}
					
					IMethod inheritedMethod= StubUtility.findInHierarchy(typeHierarchy, curr);
					if (inheritedMethod == null) {
						// create method without super call
						content= StubUtility.genStub(fType, curr, false, false, imports);
					} else {
						int flags= inheritedMethod.getFlags();
						if (Flags.isFinal(flags) || Flags.isPrivate(flags)) {
							// ask before overwriting final methods
							if (!queryOverrideFinalMethods(inheritedMethod)) {
								continue;
							}
						}
						content= StubUtility.genStub(fType, inheritedMethod, imports);	
					}
					IMethod sibling= null;
					IMethod existing= StubUtility.findMethod(curr, existingMethods);
					if (existing != null) {
						// ask before replacing a method
						if (!queryReplaceMethods(existing)) {
							continue;
						}					
						int idx= existingMethods.indexOf(existing) + 1;
						if (idx < existingMethods.size()) {
							sibling= (IMethod) existingMethods.get(idx);
						}
						existing.delete(false, null);
					} else if (curr.isConstructor() && !existingMethods.isEmpty()) {
						// add constructors at the beginning
						sibling= (IMethod) existingMethods.get(1);
					}
						
					String formattedContent= StubUtility.codeFormat(content, indent, lineDelim) + lineDelim;				
					IMethod newMethod= fType.createMethod(formattedContent, sibling, true, null);
					createdMethods.add(newMethod);
				} finally {
					monitor.worked(1);
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}
				}
			}
			
			int nCreated= createdMethods.size();
			if (nCreated > 0) {
				imports.create(fDoSave, null);
				monitor.worked(1);
				fCreatedMethods= (IMethod[]) createdMethods.toArray(new IMethod[nCreated]);
			}
		} finally {
			monitor.done();
		}
	}
	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
	
		
}
