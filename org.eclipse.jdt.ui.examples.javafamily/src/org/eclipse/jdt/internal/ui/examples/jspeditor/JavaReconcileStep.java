/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.AbstractReconcileStep;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilableModel;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.texteditor.AnnotationTypeLookup;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.core.BufferManager;


/**
 * This reconcile step has a Java source document as
 * input model and maintains a Java working copy as its model.
 * <p>
 * FIXME: We do not destroy the temporary working copy at the end.
 *         There are two ways to fix this:
 *         1. destroy it after each reconcile call ==> no internal model anylonger
 * 		   2. add life-cycle to reconcile steps (at least dispose/destroy)
 * </p>
 * @since 3.0
 */
public class JavaReconcileStep extends AbstractReconcileStep {

	private AnnotationTypeLookup fAnnotationTypeLookup= EditorsUI.getAnnotationTypeLookup();
	
	private static class TemporaryWorkingCopyOwner extends WorkingCopyOwner  {


		/*
		 * @see org.eclipse.jdt.core.WorkingCopyOwner#createBuffer(org.eclipse.jdt.core.ICompilationUnit)
		 */
		public IBuffer createBuffer(ICompilationUnit workingCopy) {
			// FIXME: Don't know how to get a buffer without using internal API.
			return BufferManager.createBuffer(workingCopy);
		}
	}

	private class ProblemAdapter extends AnnotationAdapter  {
		
		private IProblem fProblem;
		private Position fPosition;
		
		ProblemAdapter(IProblem problem)  {
			fProblem= problem;
		}

		public Position getPosition()  {
			if (fPosition == null)
				fPosition= createPositionFromProblem();
			return fPosition;
		}

		public Annotation createAnnotation() {
			int start= fProblem.getSourceStart();
			if (start < 0)
				return null;
				
			int length= fProblem.getSourceEnd() - fProblem.getSourceStart() + 1;
			if (length < 0)
				return null;

			int type= IMarker.SEVERITY_INFO;
			if (fProblem.isError())
				type= IMarker.SEVERITY_ERROR;
			else if (fProblem.isWarning())
				type= IMarker.SEVERITY_WARNING;
				
			return new Annotation(fAnnotationTypeLookup.getAnnotationType(IMarker.PROBLEM, type), false, fProblem.getMessage());
		}
		
		private Position createPositionFromProblem() {
			int start= fProblem.getSourceStart();
			if (start < 0)
				return null;
				
			int length= fProblem.getSourceEnd() - fProblem.getSourceStart() + 1;
			if (length < 0)
				return null;
				
			return new Position(start, length);
		}
	}

	private class ProblemRequestor implements IProblemRequestor  {
		
		private List fCollectedProblems;
		private boolean fIsActive= false;
		private boolean fIsRunning= false;
	
		/*
		 * @see IProblemRequestor#beginReporting()
		 */
		public void beginReporting() {
			fIsRunning= true;
			fCollectedProblems= new ArrayList();
		}
		
		/*
		 * @see IProblemRequestor#acceptProblem(IProblem)
		 */
		public void acceptProblem(IProblem problem) {
			if (isActive())
				fCollectedProblems.add(problem);
		}
	
		/*
		 * @see IProblemRequestor#endReporting()
		 */
		public void endReporting() {
			fIsRunning= false;

// WAS:
//			if (!isActive())
//				return;
//
//			if (isCanceled())
//				return;
		}
		
		public IReconcileResult[] getReconcileResult() {
			Assert.isTrue(!fIsRunning);

			int size= fCollectedProblems.size();
			IReconcileResult[] result= new IReconcileResult[size];

			for (int i= 0; i < size; i++)
				result[i]= new ProblemAdapter((IProblem)fCollectedProblems.get(i));
			
			return result;
		}
		
		/*
		 * @see IProblemRequestor#isActive()
		 */
		public boolean isActive() {
			return fIsActive && fCollectedProblems != null && !isCanceled();
		}
		
		/**
		 * Sets the active state of this problem requestor.
		 * 
		 * @param isActive the state of this problem requestor
		 */
		public void setIsActive(boolean isActive) {
			if (fIsActive != isActive) {
				fIsActive= isActive;
				if (fIsActive)
					startCollectingProblems();
				else
					stopCollectingProblems();
			}
		}

		/**
		 * Tells this annotation model to collect temporary problems from now on.
		 */
		private void startCollectingProblems() {
			fCollectedProblems= new ArrayList();
		}

		/**
		 * Tells this annotation model to no longer collect temporary problems.
		 */
		private void stopCollectingProblems() {
			// empty implementation
		}
	}

	/**
	 * Adapts an <code>ICompilationUnit</code> to the <code>ITextModel</code> interface.
	 */
	class CompilationUnitAdapter implements IReconcilableModel {
		
		private ICompilationUnit fCompilationUnit;
		
		CompilationUnitAdapter(ICompilationUnit cu) {
			fCompilationUnit= cu;
		}
		
		private ICompilationUnit getCompilationUnit() {
			return fCompilationUnit;
		}
	}

	private CompilationUnitAdapter fWorkingCopy;
	private ProblemRequestor fProblemRequestor;
	private WorkingCopyOwner fTemporaryWorkingCopyOwner;

	/**
	 * Creates the last reconcile step of the pipe.
	 */
	public JavaReconcileStep(IFile jspFile) {
		Assert.isNotNull(jspFile);
		fTemporaryWorkingCopyOwner= new TemporaryWorkingCopyOwner();
		try {
			fWorkingCopy= new CompilationUnitAdapter(createTemporaryWorkingCopy(jspFile));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates an intermediate reconcile step which adds
	 * the given step to the pipe.
	 */
	public JavaReconcileStep(IReconcileStep step, IFile jspFile) {
		super(step);
		Assert.isNotNull(jspFile);
		fTemporaryWorkingCopyOwner= new TemporaryWorkingCopyOwner();
		try {
			fWorkingCopy= new CompilationUnitAdapter(createTemporaryWorkingCopy(jspFile));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/*
	 * @see AbstractReconcileStep#reconcileModel(DirtyRegion, IRegion)
	 */
	protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
		Assert.isTrue(getInputModel() instanceof DocumentAdapter, "wrong model"); //$NON-NLS-1$

		ICompilationUnit cu= fWorkingCopy.getCompilationUnit();
		// Cannot reconcile if CU could not be built
		if (cu == null)
			return null;

		System.out.println("reconciling java model..."); //$NON-NLS-1$
		
		IBuffer buffer;
		try {
			buffer= cu.getBuffer();
		} catch (JavaModelException e) {
			e.printStackTrace();
			buffer= null;
		}
		
		if (buffer != null)
			buffer.setContents(((DocumentAdapter)getInputModel()).getDocument().get());

		try {
			synchronized (cu) {
				fProblemRequestor.setIsActive(true);
				cu.reconcile(true, getProgressMonitor());
			}
		} catch (JavaModelException ex) {
			ex.printStackTrace();
		} finally  {
			fProblemRequestor.setIsActive(false);
		}

		return fProblemRequestor.getReconcileResult();
	}

	/*
	 * @see AbstractReconcileStep#getModel()
	 */
	public IReconcilableModel getModel() {
		return fWorkingCopy;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.util.WorkingCopyUtil#getNewWorkingCopy
	 */
	private ICompilationUnit createTemporaryWorkingCopy(IFile jspFile) throws JavaModelException {

		IContainer parent= jspFile.getParent();
		IPackageFragment packageFragment= null;
		IJavaElement je= JavaCore.create(parent);
		
		if (je == null || !je.exists())
			return null;

		switch (je.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				je= je.getParent();
				//$FALL-THROUGH$
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot)je;
				packageFragment= packageFragmentRoot.getPackageFragment(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH);
				break;

			case IJavaElement.JAVA_PROJECT:
				IJavaProject jProject= (IJavaProject)je;
	
				if (!jProject.exists())  {
					System.out.println("Abort reconciling: cannot create working copy: JSP is not in a Java project"); //$NON-NLS-1$
					return null;
				}
					
				packageFragmentRoot= null;
				IPackageFragmentRoot[] packageFragmentRoots= jProject.getPackageFragmentRoots();
				int i= 0;
				while (i < packageFragmentRoots.length) {
					if (!packageFragmentRoots[i].isArchive() && !packageFragmentRoots[i].isExternal()) {
						packageFragmentRoot= packageFragmentRoots[i];
						break;
					}
					i++;
				}
				if (packageFragmentRoot == null) {
					System.out.println("Abort reconciling: cannot create working copy: JSP is not in a Java project with source package fragment root"); //$NON-NLS-1$
					return null;
				}
				packageFragment= packageFragmentRoot.getPackageFragment(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH);
				break;

			default :
				return null;
		}
		
		fProblemRequestor= new ProblemRequestor();
		
		return packageFragment.getCompilationUnit("Demo.java").getWorkingCopy(fTemporaryWorkingCopyOwner, fProblemRequestor, getProgressMonitor()); //$NON-NLS-1$
	}
}
