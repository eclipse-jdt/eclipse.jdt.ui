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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;

import org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant;
import org.eclipse.text.reconcilerpipe.IReconcilePipeParticipant;
import org.eclipse.text.reconcilerpipe.IReconcileResult;
import org.eclipse.text.reconcilerpipe.ITextModel;
import org.eclipse.text.reconcilerpipe.TextModelAdapter;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This reconcile pipe participant has a Java source document as 
 * input model and maintains a Java working copy as its model.
 *
 * @since 3.0
 */
public class JavaReconcilePipeParticipant extends AbstractReconcilePipeParticipant {

	/**
	 * Adapts an <code>ICompilationUnit</code> to the <code>ITextModel</code> interface.
	 */
	class CompilationUnitAdapter implements ITextModel {
		
		private ICompilationUnit fCompilationUnit;
		
		CompilationUnitAdapter(ICompilationUnit cu) {
			fCompilationUnit= cu;
		}
		
		private ICompilationUnit getCompilationUnit() {
			return fCompilationUnit;
		}
	}

	private CompilationUnitAdapter fWorkingCopy;

	/**
	 * Creates the last reconcile participant of the pipe.
	 */
	public JavaReconcilePipeParticipant(IFile jspFile) {
		Assert.isNotNull(jspFile);
		try {
			fWorkingCopy= new CompilationUnitAdapter(createNewWorkingCopy(jspFile));
		} catch (JavaModelException e) {
			// XXX Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Creates an intermediate reconcile participant which adds
	 * the given participant to the pipe.
	 */
	public JavaReconcilePipeParticipant(IReconcilePipeParticipant participant, IFile jspFile) {
		super(participant);
		Assert.isNotNull(jspFile);
		try {
			fWorkingCopy= new CompilationUnitAdapter(createNewWorkingCopy(jspFile));
		} catch (JavaModelException e) {
			// XXX Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant#reconcileModel(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
		Assert.isTrue(getInputModel() instanceof TextModelAdapter, "wrong model"); //$NON-NLS-1$

		ICompilationUnit cu= fWorkingCopy.getCompilationUnit(); 
		// Cannot reconcile if CU could not be built
		if (cu == null)
			return null;

		System.out.println("reconciling java model...");
		
		IBuffer buffer;
		try {
			buffer= cu.getBuffer();
		} catch (JavaModelException e) {
			e.printStackTrace();
			buffer= null;
		}
		
		if (buffer != null)
			buffer.setContents(((TextModelAdapter)getInputModel()).getDocument().get());

		try {
			synchronized (cu) {
				cu.reconcile(true, getProgressMonitor());
			}
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}

		return null;
	}

	/*
	 * @see org.eclipse.text.reconcilerpipe.AbstractReconcilePipeParticipant#getModel()
	 */
	public ITextModel getModel() {
		return fWorkingCopy;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.util.WorkingCopyUtil#getNewWorkingCopy
	 */
	private ICompilationUnit createNewWorkingCopy(IFile jspFile) throws JavaModelException {
		IPackageFragment packageFragment;
		IContainer parent= jspFile.getParent();
		if (parent.getType() == IResource.FOLDER) {
			packageFragment= (IPackageFragment)JavaCore.create(parent);
		} else {
			// project since it cannot be the workspace root
			IJavaProject jProject= (IJavaProject)JavaCore.create(parent);

			if (!jProject.exists())  {
				System.out.println("Abort reconciling: cannot create working copy: JSP is not in a Java project");
				return null;
			}
				
			IPackageFragmentRoot[] packageFragmentRoots= jProject.getPackageFragmentRoots();
			IPackageFragmentRoot packageFragmentRoot= null;
			int i= 0;
			while (i < packageFragmentRoots.length) {
				if (!packageFragmentRoots[i].isArchive() && !packageFragmentRoots[i].isExternal()) {
					packageFragmentRoot= packageFragmentRoots[i];
					break;
				}
				i++;
			}
			if (packageFragmentRoot == null) {
				System.out.println("Abort reconciling: cannot create working copy: JSP is not in a Java project with source package fragment root");
				return null;
			}
			packageFragment= packageFragmentRoot.getPackageFragment("temp");
		}
		
		return (ICompilationUnit)packageFragment.getCompilationUnit("Demo.java").getWorkingCopy();
	}
}
