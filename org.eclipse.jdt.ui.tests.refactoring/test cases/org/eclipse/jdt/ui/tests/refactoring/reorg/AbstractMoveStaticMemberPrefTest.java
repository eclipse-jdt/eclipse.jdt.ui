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

import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public class AbstractMoveStaticMemberPrefTest extends RepeatingRefactoringPerformanceTestCase {

	public AbstractMoveStaticMemberPrefTest(String name) {
		super(name);
	}

	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(numberOfCus, numberOfRefs);
		IType type= cunit.findPrimaryType();
		IMember member= type.getField("VALUE");
		IMember[] elements= new IMember[] {member};
		MoveStaticMembersProcessor processor= (RefactoringAvailabilityTester.isMoveStaticMembersAvailable(elements) ? new MoveStaticMembersProcessor(elements, JavaPreferencesSettings.getCodeGenerationSettings(cunit.getJavaProject())) : null);
		IPackageFragment destPack= fTestProject.getSourceFolder().createPackageFragment("destination", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package destination;\n");
		buf.append("public class Dest {\n");
		buf.append("}\n");
		ICompilationUnit destination= destPack.createCompilationUnit("Dest.java", buf.toString(), false, null);

		processor.setDestinationTypeFullyQualifiedName(destination.findPrimaryType().getFullyQualifiedName());
		executeRefactoring(new MoveRefactoring(processor), measure);
	}

	private ICompilationUnit generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment source= fTestProject.getSourceFolder().createPackageFragment("source", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package source;\n");
		buf.append("public class A {\n");
		buf.append("    public static final int VALUE= 10;\n");
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
		buf.append("import source.A;\n");
		buf.append("public class Ref" + index + " {\n");
		for (int i= 0; i < numberOfRefs - 1; i++) {
			buf.append("    int field" + i +"= A.VALUE;\n");
		}
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
