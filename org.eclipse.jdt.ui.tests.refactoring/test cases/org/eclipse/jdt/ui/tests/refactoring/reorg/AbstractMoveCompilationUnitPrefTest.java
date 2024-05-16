/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;


public abstract class AbstractMoveCompilationUnitPrefTest extends RepeatingRefactoringPerformanceTestCaseCommon {

	@Override
	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(numberOfCus, numberOfRefs);
		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy((new IResource[0]), (new IJavaElement[] {cunit}));
		JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
		IPackageFragment destination= fTestProject.getSourceFolder().createPackageFragment("destination", false, null);
		processor.setDestination(ReorgDestinationFactory.createDestination(destination));
		processor.setReorgQueries(new MockReorgQueries());
		processor.setUpdateReferences(true);
		executeRefactoring(new MoveRefactoring(processor), measure, RefactoringStatus.WARNING, false);
	}

	private ICompilationUnit generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment source= fTestProject.getSourceFolder().createPackageFragment("source", false, null);
		String str= """
			package source;
			public class A {
			}
			""";
		ICompilationUnit result= source.createCompilationUnit("A.java", str, false, null);

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) {
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("public class Ref" + index + " {\n");
		for (int i= 0; i < numberOfRefs - 1; i++) {
			buf.append("    source.A field" + i +";\n");
		}
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
