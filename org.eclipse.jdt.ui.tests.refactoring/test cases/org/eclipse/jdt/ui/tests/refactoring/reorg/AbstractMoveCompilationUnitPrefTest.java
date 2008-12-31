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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;


public class AbstractMoveCompilationUnitPrefTest extends RepeatingRefactoringPerformanceTestCase {

	public AbstractMoveCompilationUnitPrefTest(String name) {
		super(name);
	}

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
		StringBuffer buf= new StringBuffer();
		buf.append("package source;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit result= source.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) {
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("public class Ref" + index + " {\n");
		for (int i= 0; i < numberOfRefs - 1; i++) {
			buf.append("    source.A field" + i +";\n");
		}
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
