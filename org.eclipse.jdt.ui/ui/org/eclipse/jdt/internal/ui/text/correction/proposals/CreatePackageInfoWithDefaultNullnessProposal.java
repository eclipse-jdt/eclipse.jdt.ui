/*******************************************************************************
 * Copyright (c) 2017, 2018 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.MultiFixTarget;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.NullAnnotationsFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

public final class CreatePackageInfoWithDefaultNullnessProposal extends ChangeCorrectionProposal {
	public static CreatePackageInfoWithDefaultNullnessProposal createFor(int problemId, String name, IPackageFragment pack) throws CoreException {
		int relevance= IProposalRelevance.ADD_MISSING_NULLNESS_ANNOTATION;
		String nonNullByDefaultAnnotationName= NullAnnotationsFix.getNonNullByDefaultAnnotationName(pack, false);

		String lineDelimiter= StubUtility.getLineDelimiterUsed(pack.getJavaProject());
		StringBuilder content= new StringBuilder();
		String fileComment= InfoFilesUtil.getFileComment(JavaModelUtil.PACKAGE_INFO_JAVA, pack, lineDelimiter);
		String typeComment= InfoFilesUtil.getTypeComment(JavaModelUtil.PACKAGE_INFO_JAVA, pack, lineDelimiter);
		if (fileComment != null) {
			content.append(fileComment);
			content.append(lineDelimiter);
		}

		if (typeComment != null) {
			content.append(typeComment);
			content.append(lineDelimiter);
		} else if (fileComment != null) {
			// insert an empty file comment to avoid that the file comment becomes the type comment
			content.append("/**"); //$NON-NLS-1$
			content.append(lineDelimiter);
			content.append(" *"); //$NON-NLS-1$
			content.append(lineDelimiter);
			content.append(" */"); //$NON-NLS-1$
			content.append(lineDelimiter);
		}

		content.append("@"); //$NON-NLS-1$
		content.append(nonNullByDefaultAnnotationName);
		content.append(lineDelimiter);
		content.append("package "); //$NON-NLS-1$
		content.append(pack.getElementName());
		content.append(";"); //$NON-NLS-1$
		content.append(lineDelimiter);
		String source= content.toString();

		ICompilationUnit unit= pack.getCompilationUnit(JavaModelUtil.PACKAGE_INFO_JAVA);
		Change change= new CreateCompilationUnitChange(unit, source, null);
		CreatePackageInfoWithDefaultNullnessProposal proposal= new CreatePackageInfoWithDefaultNullnessProposal(problemId, name, change, relevance, unit);
		return proposal;
	}

	private final ICompilationUnit fUnit;

	public final int fProblemId;

	public CreatePackageInfoWithDefaultNullnessProposal(int problemId, String name, Change change, int relevance, ICompilationUnit unit) {
		super(name, change, relevance);
		fProblemId= problemId;
		fUnit= unit;
	}

	@Override
	public void apply(org.eclipse.jface.text.IDocument document) {
		super.apply(document);
		IEditorPart part= null;
		if (fUnit.getResource().exists()) {
			part= EditorUtility.isOpenInEditor(fUnit);
			if (part == null) {
				try {
					part= JavaUI.openInEditor(fUnit);
				} catch (PartInitException | JavaModelException e) {
					return;
				}
			}
			IWorkbenchPage page= JavaPlugin.getActivePage();
			if (page != null && part != null) {
				page.bringToTop(part);
			}
			if (part != null) {
				part.setFocus();
			}
		}
	}

	public void resolve(MultiFixTarget[] problems, IProgressMonitor monitor) throws CoreException {
		/*
		 * if there are multiple problems for different source folders with the same package and they are in the same project,
		 * only create a package-info.java in one of them. prefer main sources to test sources, for now by sorting by folder
		 * name (e.g. "/proj/src/main/java" < "/proj/src/test/java", "/proj/src" < "/proj/src-tests").
		 *
		 * similar, if they are in different projects and one is on the classpath of the other, create it in the
		 * project that cannot see the other.
		 */
		HashMap<String, ArrayList<MultiFixTarget>> packageToTarget= new HashMap<>();
		ArrayList<IPackageFragment> removeList= new ArrayList<>();
		for (MultiFixTarget problem : problems) {
			IPackageFragment packageFragment= (IPackageFragment) problem.getCompilationUnit().getParent();
			ArrayList<MultiFixTarget> list= packageToTarget.get(packageFragment.getElementName());
			if (list == null) {
				list= new ArrayList<>();
				list.add(problem);
				packageToTarget.put(packageFragment.getElementName(), list);
			} else {
				IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) packageFragment.getParent();
				boolean needToAdd= true;
				for (int i= 0; i < list.size(); i++) {
					MultiFixTarget previous= list.get(i);
					IPackageFragmentRoot previousPackageFragmentRoot= (IPackageFragmentRoot) previous.getCompilationUnit().getParent().getParent();
					if (Objects.equals(packageFragmentRoot.getJavaProject(), previousPackageFragmentRoot.getJavaProject())) {
						if (packageFragmentRoot.getResource().getProjectRelativePath().toString().compareTo(previousPackageFragmentRoot.getResource().getProjectRelativePath().toString()) < 0) {
							list.remove(i);
							i--;
						} else {
							needToAdd= false;
						}
					} else {
						if (packageFragmentRoot.getJavaProject().isOnClasspath(previousPackageFragmentRoot.getJavaProject())) {
							needToAdd= false;
							removeList.add(packageFragment);
						} else if (previousPackageFragmentRoot.getJavaProject().isOnClasspath(packageFragmentRoot.getJavaProject())) {
							list.remove(i);
							i--;
							removeList.add((IPackageFragment) previous.getCompilationUnit().getParent());
						}
					}
				}
				if (needToAdd) {
					list.add(problem);
				}
			}
		}
		CompositeChange compositeChange= new CompositeChange(this.getName());
		for (Entry<String, ArrayList<MultiFixTarget>> entry : packageToTarget.entrySet()) {
			ArrayList<MultiFixTarget> list= entry.getValue();
			for (MultiFixTarget multiFixTarget : list) {
				CreatePackageInfoWithDefaultNullnessProposal createPackageInfoProposal= createFor(this.fProblemId, this.getName(),
						(IPackageFragment) multiFixTarget.getCompilationUnit().getParent());
				compositeChange.add(createPackageInfoProposal.getChange());
			}
		}
		PerformChangeOperation operation= new PerformChangeOperation(compositeChange);
		operation.setUndoManager(RefactoringCore.getUndoManager(), compositeChange.getName());
		operation.run(monitor);
		// for package fragments on the removeList, a package-info.java has been created in another project on the classpath.
		// in an incremental build, the markers are not deleted, so delete them here.
		for (IPackageFragment packageFragment : removeList) {
			packageFragment.getResource().deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
		}
	}
}
