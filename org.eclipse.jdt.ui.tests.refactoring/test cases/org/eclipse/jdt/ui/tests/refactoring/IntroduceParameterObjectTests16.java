/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hélios GILLES - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.core.refactoring.descriptors.IntroduceParameterObjectDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

/**
 * Tests Introduce Parameter Object on the constructors of records: it is not available on a
 * canonical constructor, and telling it must not block the UI thread.
 */
public class IntroduceParameterObjectTests16 extends GenericRefactoringTest {

	public IntroduceParameterObjectTests16() {
		rts= new Java16Setup();
	}

	private static CompilationUnit createAST(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Returns the canonical constructors of the given compilation unit, after checking for each of
	 * its constructors that the availability of the refactoring and the compiler tell the same.
	 */
	private List<String> getCanonicalConstructors(ICompilationUnit cu) throws Exception {
		CompilationUnit ast= createAST(cu);
		List<String> canonicalConstructors= new ArrayList<>();
		for (IType type : cu.getAllTypes()) {
			for (IMethod method : type.getMethods()) {
				if (method.isConstructor()) {
					String label= type.getElementName() + Stream.of(method.getParameterTypes()).map(Signature::toString).collect(Collectors.joining(", ", "(", ")"));
					boolean isCanonical= RefactoringAvailabilityTesterCore.isCanonicalConstructor(method);
					IMethodBinding binding= ASTNodeSearchUtil.getMethodDeclarationNode(method, ast).resolveBinding();
					assertEquals(label, binding.isCanonicalConstructor(), isCanonical);
					assertEquals(label, !isCanonical, isIntroduceParameterObjectAvailable(method));
					if (isCanonical) {
						canonicalConstructors.add(label);
					}
				}
			}
		}
		return canonicalConstructors;
	}

	@Test
	public void testCanonicalConstructor() throws Exception {
		ICompilationUnit cu= createCU(getPackageP(), "A.java", """
				package p;
				public class A {
					record Explicit(int count, String name) {
						Explicit(int count, String name) {
							this.count= count;
							this.name= name;
						}
						Explicit(int count) {
							this(count, "");
						}
						Explicit(String name, int count) {
							this(count, name);
						}
					}
					record Implicit(int count, String name) {
						Implicit(int count) {
							this(count, "");
						}
					}
					static class NotARecord {
						NotARecord(int count, String name) {
						}
					}
				}
				""");

		assertEquals(List.of("Explicit(int, String)"), getCanonicalConstructors(cu));
	}

	@Test
	public void testCompactCanonicalConstructor() throws Exception {
		ICompilationUnit cu= createCU(getPackageP(), "A.java", """
				package p;
				import java.util.List;
				public record A(int count, List<String> names) {
					public A {
						if (count < 0) {
							throw new IllegalArgumentException();
						}
					}
					public A(int count) {
						this(count, null);
					}
				}
				""");

		assertEquals(List.of("A(int, List<String>)"), getCanonicalConstructors(cu));
	}

	@Test
	public void testCanonicalConstructorWithGenericTypes() throws Exception {
		ICompilationUnit cu= createCU(getPackageP(), "A.java", """
				package p;
				import java.util.List;
				public record A<T>(T value, List<? extends T> others) {
					public A(T value, List<? extends T> others) {
						this.value= value;
						this.others= others;
					}
					public A(List<? extends T> others, T value) {
						this(value, others);
					}
				}
				""");

		assertEquals(List.of("A(T, List<? extends T>)"), getCanonicalConstructors(cu));
	}

	/**
	 * The availability is evaluated in the UI thread when the selection changes: it must not wait
	 * for the AST of the active editor, which is not available while the editor is reconciled.
	 * Once the editor is reconciled, its AST tells the canonical constructor.
	 */
	@Test
	public void testCanonicalConstructorWhileReconciling() throws Exception {
		ICompilationUnit cu= createRecord();
		IMethod canonicalConstructor= getCanonicalConstructor(cu);
		IMethod otherConstructor= cu.getType("A").getMethod("A", new String[] { "I" });
		assertTrue(otherConstructor.exists());

		CoreASTProvider astProvider= CoreASTProvider.getInstance();
		ITypeRoot activeJavaElement= astProvider.getActiveJavaElement();
		// the active editor is about to be reconciled, and its reconciler does not complete
		astProvider.setActiveJavaElement(cu);
		astProvider.aboutToBeReconciled(cu);
		try {
			CompletableFuture<Boolean> canonicalAvailable= CompletableFuture.supplyAsync(() -> isIntroduceParameterObjectAvailable(canonicalConstructor));
			CompletableFuture<Boolean> otherAvailable= CompletableFuture.supplyAsync(() -> isIntroduceParameterObjectAvailable(otherConstructor));
			// not known without the AST: the refactoring rejects the canonical constructor when it starts
			assertTrue(canonicalAvailable.get(10, TimeUnit.SECONDS));
			assertTrue(otherAvailable.get(10, TimeUnit.SECONDS));

			astProvider.reconciled(createAST(cu), cu, null);
			assertFalse(isIntroduceParameterObjectAvailable(canonicalConstructor));
			assertTrue(isIntroduceParameterObjectAvailable(otherConstructor));
		} finally {
			// releases the threads that wait for the AST
			astProvider.reconciled(null, cu, null);
			astProvider.clearReconciliation();
			astProvider.setActiveJavaElement(activeJavaElement);
		}
	}

	/**
	 * When the active editor has no AST and is not reconciled, e.g. when it has just been
	 * activated, the compilation unit is parsed, once: the AST is kept as the shared one.
	 */
	@Test
	public void testCanonicalConstructorOfActiveEditorWithoutAST() throws Exception {
		ICompilationUnit cu= createRecord();
		IMethod canonicalConstructor= getCanonicalConstructor(cu);

		CoreASTProvider astProvider= CoreASTProvider.getInstance();
		ITypeRoot activeJavaElement= astProvider.getActiveJavaElement();
		astProvider.clearReconciliation();
		astProvider.setActiveJavaElement(cu);
		astProvider.disposeAST();
		try {
			assertFalse(isIntroduceParameterObjectAvailable(canonicalConstructor));
			CompilationUnit ast= astProvider.getCachedAST();
			assertNotNull("the AST of the active editor is kept", ast);

			assertFalse(isIntroduceParameterObjectAvailable(canonicalConstructor));
			assertSame("the compilation unit is not parsed again", ast, astProvider.getCachedAST());
		} finally {
			astProvider.disposeAST();
			astProvider.setActiveJavaElement(activeJavaElement);
		}
	}

	/**
	 * The refactoring may be started on a canonical constructor while its availability is not
	 * known, and from the editor, where it is always available: it rejects it.
	 */
	@Test
	public void testRefactoringRejectsCanonicalConstructor() throws Exception {
		ICompilationUnit cu= createRecord();
		IMethod canonicalConstructor= getCanonicalConstructor(cu);
		IMethod otherConstructor= cu.getType("A").getMethod("A", new String[] { "I" });

		RefactoringStatus status= checkInitialConditions(canonicalConstructor);
		assertTrue(status.hasFatalError());
		assertEquals(RefactoringCoreMessages.IntroduceParameterObjectRefactoring_error_canonical_constructor, status.getMessageMatchingSeverity(RefactoringStatus.FATAL));
		assertFalse(checkInitialConditions(otherConstructor).hasFatalError());
	}

	private ICompilationUnit createRecord() throws Exception {
		return createCU(getPackageP(), "A.java", """
				package p;
				public record A(int count, String name) {
					public A(int count, String name) {
						this.count= count;
						this.name= name;
					}
					public A(int count) {
						this(count, "");
					}
				}
				""");
	}

	private static IMethod getCanonicalConstructor(ICompilationUnit cu) {
		IMethod canonicalConstructor= cu.getType("A").getMethod("A", new String[] { "I", "QString;" });
		assertTrue(canonicalConstructor.exists());
		return canonicalConstructor;
	}

	private RefactoringStatus checkInitialConditions(IMethod constructor) throws Exception {
		IntroduceParameterObjectDescriptor descriptor= RefactoringSignatureDescriptorFactory.createIntroduceParameterObjectDescriptor();
		descriptor.setMethod(constructor);
		descriptor.setParameters(IntroduceParameterObjectDescriptor.createParameters(constructor));
		Refactoring refactoring= createRefactoring(descriptor);
		return refactoring.checkInitialConditions(new NullProgressMonitor());
	}

	private static boolean isIntroduceParameterObjectAvailable(IMethod method) {
		try {
			return RefactoringAvailabilityTester.isIntroduceParameterObjectAvailable(new StructuredSelection(method));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
