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
package org.eclipse.jdt.ui.tests.refactoring.typeconstraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractJunit4CUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class TypeEnvironmentTests extends AbstractJunit4CUTestCase {

	private static final boolean BUG_83616_core_wildcard_assignments= true;

	private static class MyTestSetup extends RefactoringTestSetup {
		private static IPackageFragment fSignaturePackage;
		private static IPackageFragment fGenericPackage;

		@Override
		public void before() throws Exception {
			super.before();
			fSignaturePackage= getDefaultSourceFolder().createPackageFragment("signature", true, null);
			fGenericPackage= getDefaultSourceFolder().createPackageFragment("generic", true, null);
		}
		public static IPackageFragment getSignaturePackage() {
			return fSignaturePackage;
		}
		public static IPackageFragment getGenericPackage() {
			return fGenericPackage;
		}
	}

	private static class CreationChecker extends HierarchicalASTVisitor {
		private TypeEnvironment fTypeEnvironment;
		public CreationChecker() {
			fTypeEnvironment= new TypeEnvironment();
		}
		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof ITypeBinding))
				return true;
			checkTypeBinding(binding);
			return true;
		}
		private void checkTypeBinding(IBinding binding) {
			ITypeBinding type= (ITypeBinding)binding;
			if (!type.isPrimitive() || !"void".equals(type.getName())) {
				TType refType= fTypeEnvironment.create(type);
				assertNotNull("Refactoring type is null", refType);
				assertEquals("Not same name", type.getName(), refType.getName());
				assertEquals("Not same signature", PrettySignatures.get(type), refType.getPrettySignature());
				assertSame("Not same type", refType, fTypeEnvironment.create(type));
			}
		}
		@Override
		public boolean visit(org.eclipse.jdt.core.dom.Type node) {
			checkTypeBinding(node.resolveBinding());
			return true;
		}
	}

	private static class TypeBindingCollector extends ASTVisitor {
		private List<ITypeBinding> fResult= new ArrayList<>();
		private List<ITypeBinding> fWildcards= new ArrayList<>();
		@Override
		public boolean visit(FieldDeclaration node) {
			List<VariableDeclarationFragment> fragments= node.fragments();
			VariableDeclarationFragment fragment= fragments.get(0);
			if ("NullType".equals(fragment.getName().getIdentifier())) {
				fResult.add(fragment.getInitializer().resolveTypeBinding());
			} else {
				fResult.add(fragment.resolveBinding().getType());
			}
			return false;
		}
		@Override
		public void endVisit(CompilationUnit node) {
			for (ITypeBinding binding : fResult) {
				if (binding.isParameterizedType()) {
					for (ITypeBinding arg : binding.getTypeArguments()) {
						if (arg.isWildcardType()) {
							fWildcards.add(arg);
						}
					}
				}
			}
		}
		public ITypeBinding[] getResult() {
			return fResult.toArray(new ITypeBinding[fResult.size()]);
		}
		public ITypeBinding[] getWildcards() {
			return fWildcards.toArray(new ITypeBinding[fWildcards.size()]);
		}
	}

	private static class CaptureTypeBindingCollector extends ASTVisitor {
		private List<ITypeBinding> fResult= new ArrayList<>();
		@Override
		public boolean visit(Assignment node) {
			Expression expression= node.getRightHandSide();
			ITypeBinding typeBinding= expression.resolveTypeBinding();
			fResult.add(typeBinding);
			collectTypeArgumentBindings(typeBinding, fResult);
			return false;
		}
		private void collectTypeArgumentBindings(ITypeBinding typeBinding, List<ITypeBinding> result) {
			if (! typeBinding.isParameterizedType())
				return;
			for (ITypeBinding typeArgument : typeBinding.getTypeArguments()) {
				if (BUG_83616_core_wildcard_assignments && typeArgument.isParameterizedType() && typeArgument.getTypeArguments()[0].isWildcardType())
					continue;
				result.add(typeArgument);
				collectTypeArgumentBindings(typeArgument, result);
			}
		}
		public ITypeBinding[] getResult() {
			return fResult.toArray(new ITypeBinding[fResult.size()]);
		}
	}

	@Rule
	public MyTestSetup mts=new MyTestSetup();

	@Override
	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	@Override
	protected String getResourceLocation() {
		return "TypeEnvironment/TestProject/";
	}

	@Override
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	private ASTNode createAST(IPackageFragment pack) throws Exception {
		IJavaProject project= pack.getJavaProject();
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setProject(project);
		parser.setResolveBindings(true);
		ICompilationUnit unit= createCU(pack, getName());
		parser.setSource(unit);
		return parser.createAST(null);
	}

	//---- creation ----------------------------------------------------------

	private void performCreationTest() throws Exception {
		createAST(MyTestSetup.getSignaturePackage()).accept(new CreationChecker());
	}

	@Test
	public void testArrays() throws Exception {
		performCreationTest();
	}

	@Test
	public void testStandardTypes() throws Exception {
		performCreationTest();
	}

	@Test
	public void testRawTypes() throws Exception {
		performCreationTest();
	}

	@Test
	public void testGenericTypes() throws Exception {
		performCreationTest();
	}

	@Test
	public void testWildcardTypes() throws Exception {
		performCreationTest();
	}

	@Test
	public void testPrimitiveTypes() throws Exception {
		performCreationTest();
	}

	@Test
	public void testTypeVariables() throws Exception {
		performCreationTest();
	}

	//---- generic assigment test ----------------------------------------------

	private void performGenericAssignmentTest() throws Exception {
		ASTNode node= createAST(MyTestSetup.getGenericPackage());
		TypeBindingCollector collector= new TypeBindingCollector();
		node.accept(collector);
		testBindings(collector.getResult());
		testAssignment(collector.getWildcards());
	}

	private void testBindings(ITypeBinding[] bindings) throws Exception {
		TType[] types= new TType[bindings.length];
		TypeEnvironment environment= new TypeEnvironment();
		for (int i= 0; i < bindings.length; i++) {
			types[i]= environment.create(bindings[i]);
			assertEquals("Not same name", bindings[i].getName(), types[i].getName());
			assertEquals("Not same signature", PrettySignatures.get(bindings[i]), types[i].getPrettySignature());
			assertEquals("Not same modifiers", bindings[i].getModifiers(), types[i].getModifiers());
			testFlags(bindings[i], types[i]);
			assertTrue("Not same erasure", types[i].getErasure().isEqualTo(bindings[i].getErasure()));
			assertTrue("Not same type declaration", types[i].getTypeDeclaration().isEqualTo(bindings[i].getTypeDeclaration()));
			assertSame("Not same type", types[i], environment.create(bindings[i]));

		}
		for (int o= 0; o < bindings.length; o++) {
			for (int i= 0; i < bindings.length; i++) {
				checkCanAssignTo(bindings[o], bindings[i], types[o], types[i]);
			}
		}
		TypeEnvironment secondEnvironment= new TypeEnvironment();
		for (int i= 0; i < bindings.length; i++) {
			assertEquals("Equal to second environment", types[i], secondEnvironment.create(bindings[i]));
		}
		ITypeBinding[] restoredBindings= TypeEnvironment.createTypeBindings(types, mts.getProject());
		assertEquals("Not same length", restoredBindings.length, bindings.length);
		for (int i= 0; i < restoredBindings.length; i++) {
			assertTrue("Not same binding", bindings[i].isEqualTo(restoredBindings[i]));
		}
	}

	private void checkCanAssignTo(ITypeBinding rhsBinding, ITypeBinding lhsBinding, TType rhs, TType lhs) {
		boolean coreResult= rhsBinding.isAssignmentCompatible(lhsBinding);
		boolean uiResult= rhs.canAssignTo(lhs);
		assertEquals("Different assignment rule(" +
			PrettySignatures.get(lhsBinding) + "= " + PrettySignatures.get(rhsBinding) +
			"): ", coreResult, uiResult);
	}

	private void testAssignment(ITypeBinding[] bindings) {
		TType[] types= new TType[bindings.length];
		TypeEnvironment environment= new TypeEnvironment();
		for (int i= 0; i < bindings.length; i++) {
			types[i]= environment.create(bindings[i]);
		}
		for (int o= 0; o < bindings.length; o++) {
			for (int i= 0; i < bindings.length; i++) {
				ITypeBinding oBinding= bindings[o];
				ITypeBinding iBinding= bindings[i];
				boolean coreResult= oBinding.isAssignmentCompatible(iBinding);
				TType oType= types[o];
				TType iType= types[i];
				boolean uiResult= oType.canAssignTo(iType);
				if (!BUG_83616_core_wildcard_assignments) {
					if (coreResult != uiResult && !oType.isWildcardType()) {
						System.out.println("Different assignment rule(" +
								PrettySignatures.get(iBinding) + "= " + PrettySignatures.get(oBinding) +
								"): Bindings<" + coreResult +
								"> TType<" + uiResult + ">");
					}
				}
			}
		}
	}

	private void testFlags(ITypeBinding binding, TType type) {
		assertEquals("Different class flag", binding.isClass(), type.isClass());
		assertEquals("Different enum flag", binding.isEnum(), type.isEnum());
		assertEquals("Different interface  flag", binding.isInterface(), type.isInterface());
		assertEquals("Different annotation flag", binding.isAnnotation(), type.isAnnotation());

		assertEquals("Different top level flag", binding.isTopLevel(), type.isTopLevel());
		assertEquals("Different nested flag", binding.isNested(), type.isNested());
		assertEquals("Different local flag", binding.isLocal(), type.isLocal());
		assertEquals("Different member flag", binding.isMember(), type.isMember());
		assertEquals("Different anonymous flag", binding.isAnonymous(), type.isAnonymous());
	}

	@Test
	public void testStandardAssignments() throws Exception {
		performGenericAssignmentTest();
	}

	@Test
	public void testWildcardAssignments() throws Exception {
		performGenericAssignmentTest();
	}

	@Test
	public void testTypeVariableAssignments() throws Exception {
		performGenericAssignmentTest();
	}

	@Test
	public void testCaptureAssignments() throws Exception {
		ASTNode node= createAST(MyTestSetup.getGenericPackage());
		CaptureTypeBindingCollector collector= new CaptureTypeBindingCollector();
		node.accept(collector);
		testBindings(collector.getResult());
	}

	public void _testAssignment() throws Exception {
		ASTNode node= createAST(MyTestSetup.getGenericPackage());
		TypeBindingCollector collector= new TypeBindingCollector();
		node.accept(collector);
		ITypeBinding[] bindings= collector.getResult();
		TType[] types= new TType[bindings.length];
		TypeEnvironment environment= new TypeEnvironment();
		for (int i= 0; i < bindings.length; i++) {
			types[i]= environment.create(bindings[i]);
		}
		System.out.println(PrettySignatures.get(bindings[0]) + "= " + PrettySignatures.get(bindings[1]) +
			": " + bindings[1].isAssignmentCompatible(bindings[0]));
		// types[1].canAssignTo(types[0]);
	}

	public void _testParameterizedToGeneric() throws Exception {
		ASTNode node= createAST(MyTestSetup.getGenericPackage());
		TypeBindingCollector collector= new TypeBindingCollector();
		node.accept(collector);
		ITypeBinding[] bindings= collector.getResult();
		bindings[0]= bindings[0].getTypeDeclaration();
		System.out.println(PrettySignatures.get(bindings[0]) + "= " + PrettySignatures.get(bindings[1]) +
			": " + bindings[1].isAssignmentCompatible(bindings[0]));
		System.out.println(PrettySignatures.get(bindings[0]) + "= " + PrettySignatures.get(bindings[0]) +
			": " + bindings[0].isAssignmentCompatible(bindings[0]));
		bindings[1]= bindings[1].getTypeDeclaration();
		System.out.println(PrettySignatures.get(bindings[0]) + "= " + PrettySignatures.get(bindings[1]) +
			": " + bindings[1].isAssignmentCompatible(bindings[0]));
	}
}
