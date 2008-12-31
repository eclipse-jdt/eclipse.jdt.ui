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
package org.eclipse.jdt.ui.tests.refactoring.typeconstraints;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractCUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

public class TypeEnvironmentTests extends AbstractCUTestCase {

	private static final boolean BUG_83616_core_wildcard_assignments= true;

	private static class MyTestSetup extends RefactoringTestSetup {
		private static IPackageFragment fSignaturePackage;
		private static IPackageFragment fGenericPackage;
		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			super.setUp();
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
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof ITypeBinding))
				return true;
			checkTypeBinding(binding);
			return true;
		}
		private void checkTypeBinding(IBinding binding) {
			ITypeBinding type= (ITypeBinding)binding;
			if (!(type.isPrimitive() && type.getName().equals("void"))) {
				TType refType= fTypeEnvironment.create(type);
				assertNotNull("Refactoring type is null", refType);
				assertEquals("Not same name", type.getName(), refType.getName());
				assertEquals("Not same signature", PrettySignatures.get(type), refType.getPrettySignature());
				assertTrue("Not same type", refType == fTypeEnvironment.create(type));
			}
		}
		public boolean visit(org.eclipse.jdt.core.dom.Type node) {
			checkTypeBinding(node.resolveBinding());
			return true;
		}
	}

	private static class TypeBindingCollector extends ASTVisitor {
		private List fResult= new ArrayList();
		private List fWildcards= new ArrayList();
		public boolean visit(FieldDeclaration node) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment)node.fragments().get(0);
			if (fragment.getName().getIdentifier().equals("NullType")) {
				fResult.add(fragment.getInitializer().resolveTypeBinding());
			} else {
				fResult.add(fragment.resolveBinding().getType());
			}
			return false;
		}
		public void endVisit(CompilationUnit node) {
			for (Iterator iter= fResult.iterator(); iter.hasNext();) {
				ITypeBinding binding= (ITypeBinding)iter.next();
				if (binding.isParameterizedType()) {
					ITypeBinding[] args= binding.getTypeArguments();
					for (int i= 0; i < args.length; i++) {
						if (args[i].isWildcardType()) {
							fWildcards.add(args[i]);
						}
					}
				}
			}
		}
		public ITypeBinding[] getResult() {
			return (ITypeBinding[])fResult.toArray(new ITypeBinding[fResult.size()]);
		}
		public ITypeBinding[] getWildcards() {
			return (ITypeBinding[])fWildcards.toArray(new ITypeBinding[fWildcards.size()]);
		}
	}

	private static class CaptureTypeBindingCollector extends ASTVisitor {
		private List fResult= new ArrayList();
		public boolean visit(Assignment node) {
			Expression expression= node.getRightHandSide();
			ITypeBinding typeBinding= expression.resolveTypeBinding();
			fResult.add(typeBinding);
			collectTypeArgumentBindings(typeBinding, fResult);
			return false;
		}
		private void collectTypeArgumentBindings(ITypeBinding typeBinding, List result) {
			if (! typeBinding.isParameterizedType())
				return;
			ITypeBinding[] typeArguments= typeBinding.getTypeArguments();
			for (int i= 0; i < typeArguments.length; i++) {
				ITypeBinding typeArgument= typeArguments[i];
				if (BUG_83616_core_wildcard_assignments && typeArgument.isParameterizedType() && typeArgument.getTypeArguments()[0].isWildcardType())
					continue;
				result.add(typeArgument);
				collectTypeArgumentBindings(typeArgument, result);
			}
		}
		public ITypeBinding[] getResult() {
			return (ITypeBinding[])fResult.toArray(new ITypeBinding[fResult.size()]);
		}
	}


	public TypeEnvironmentTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MyTestSetup(new TestSuite(TypeEnvironmentTests.class));
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	protected String getResourceLocation() {
		return "TypeEnvironment/TestProject/";
	}

	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	private ASTNode createAST(IPackageFragment pack) throws Exception {
		IJavaProject project= pack.getJavaProject();
		ASTParser parser= ASTParser.newParser(AST.JLS3);
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

	public void testArrays() throws Exception {
		performCreationTest();
	}

	public void testStandardTypes() throws Exception {
		performCreationTest();
	}

	public void testRawTypes() throws Exception {
		performCreationTest();
	}

	public void testGenericTypes() throws Exception {
		performCreationTest();
	}

	public void testWildcardTypes() throws Exception {
		performCreationTest();
	}

	public void testPrimitiveTypes() throws Exception {
		performCreationTest();
	}

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
			assertTrue("Not same type", types[i] == environment.create(bindings[i]));

		}
		for (int o= 0; o < bindings.length; o++) {
			for (int i= 0; i < bindings.length; i++) {
				checkCanAssignTo(bindings[o], bindings[i], types[o], types[i]);
			}
		}
		TypeEnvironment secondEnvironment= new TypeEnvironment();
		for (int i= 0; i < bindings.length; i++) {
			assertTrue("Equal to second environment", types[i].equals(secondEnvironment.create(bindings[i])));
		}
		ITypeBinding[] restoredBindings= TypeEnvironment.createTypeBindings(types, RefactoringTestSetup.getProject());
		assertEquals("Not same length", restoredBindings.length, bindings.length);
		for (int i= 0; i < restoredBindings.length; i++) {
			assertTrue("Not same binding", bindings[i].isEqualTo(restoredBindings[i]));
		}
	}

	private void checkCanAssignTo(ITypeBinding rhsBinding, ITypeBinding lhsBinding, TType rhs, TType lhs) {
		boolean coreResult= rhsBinding.isAssignmentCompatible(lhsBinding);
		boolean uiResult= rhs.canAssignTo(lhs);
		if (coreResult != uiResult) {
			if (lhs.isCaptureType() || rhs.isCaptureType()) { // see bug 93082
				System.out.println("Different assignment rule(" +
					PrettySignatures.get(lhsBinding) + "= " + PrettySignatures.get(rhsBinding) +
					"): Bindings<" + coreResult +
					"> TType<" + uiResult + ">");
				return;
			}
		}

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
				if (coreResult != uiResult && !oType.isWildcardType() && ! BUG_83616_core_wildcard_assignments) { // see bug 83616
					System.out.println("Different assignment rule(" +
						PrettySignatures.get(iBinding) + "= " + PrettySignatures.get(oBinding) +
						"): Bindings<" + coreResult +
						"> TType<" + uiResult + ">");
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

	public void testStandardAssignments() throws Exception {
		performGenericAssignmentTest();
	}

	public void testWildcardAssignments() throws Exception {
		performGenericAssignmentTest();
	}

	public void testTypeVariableAssignments() throws Exception {
		performGenericAssignmentTest();
	}

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
