/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTagElement;
import org.eclipse.jdt.core.dom.AbstractTextElement;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AbstractUnnamedTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.ModulePackageAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Pattern;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;

public class HierarchicalASTVisitorTest {
	private static class TestHierarchicalASTVisitor extends HierarchicalASTVisitor {

		//---- BEGIN <REGION TO BE UPDATED IN RESPONSE TO ASTNode HIERARCHY CHANGES> ---------------------
		/* ******************************************************************************
		 * Whereas the other parts of this test should be relatively static,
		 * this portion of the file must be maintained in response to
		 * changes that occur in the ASTNode hierarchy (and, thus, the ASTVisitor).
		 * Such changes would include addition or removal of node types to or from
		 * the hierarchy, or changes in the superclass/subclass relationships
		 * among node classes.  Such changes necessitate, also, changes in the
		 * HierarchicalASTVisitor itself, whose structure and behaviour this test
		 * verifies.
		 *
		 * The changes that must be made to this file in response to such changes in
		 * the ASTNode hierarchy are localized here and limited to maintenance of the
		 * following set of visit(XX node), superVisit(XX node), endVisit(XX node),
		 * and superEndVisit(XX node) implementations.
		 * There should be one such quadruple for each non-leaf ASTNode descendant class,
		 * including ASTNode itself.
		 *
		 * *****************************************************************************/

		/* Here, for each non-leaf ASTNode descendant class, the visit(XX) method
		 * is overridden to call registerCall(XX.class), and a void superVisit
		 * (XX node) method is provided, which simply calls super.visit(XX).
		 * Accordingly for endVisit(XX) and superEndVisit(XX).
		 */

		@Override
		public boolean visit(ASTNode node) {
			registerCall(ASTNode.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(ASTNode node) {
			super.visit(node);
		}
		@Override
		public void endVisit(ASTNode node) {
			registerCall(ASTNode.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(ASTNode node) {
			super.visit(node);
		}

		@Override
		public boolean visit(BodyDeclaration node) {
			registerCall(BodyDeclaration.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(BodyDeclaration node) {
			super.visit(node);
		}
		@Override
		public void endVisit(BodyDeclaration node) {
			registerCall(BodyDeclaration.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(BodyDeclaration node) {
			super.visit(node);
		}

		@Override
		public boolean visit(AbstractTypeDeclaration node) {
			registerCall(AbstractTypeDeclaration.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(AbstractTypeDeclaration node) {
			super.visit(node);
		}
		@Override
		public void endVisit(AbstractTypeDeclaration node) {
			registerCall(AbstractTypeDeclaration.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(AbstractTypeDeclaration node) {
			super.visit(node);
		}

		@Override
		public boolean visit(AbstractTagElement node) {
			registerCall(AbstractTagElement.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(AbstractTagElement node) {
			super.visit(node);
		}
		@Override
		public void endVisit(AbstractTagElement node) {
			registerCall(AbstractTagElement.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(AbstractTagElement node) {
			super.visit(node);
		}

		@Override
		public boolean visit(AbstractTextElement node) {
			registerCall(AbstractTextElement.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(AbstractTextElement node) {
			super.visit(node);
		}
		@Override
		public void endVisit(AbstractTextElement node) {
			registerCall(AbstractTextElement.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(AbstractTextElement node) {
			super.visit(node);
		}

		@Override
		public boolean visit(AbstractUnnamedTypeDeclaration node) {
			registerCall(AbstractUnnamedTypeDeclaration.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(AbstractUnnamedTypeDeclaration node) {
			super.visit(node);
		}
		@Override
		public void endVisit(AbstractUnnamedTypeDeclaration node) {
			registerCall(AbstractUnnamedTypeDeclaration.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(AbstractUnnamedTypeDeclaration node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Comment node) {
			registerCall(Comment.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Comment node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Comment node) {
			registerCall(Comment.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Comment node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Expression node) {
			registerCall(Expression.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Expression node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Expression node) {
			registerCall(Expression.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Expression node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Annotation node) {
			registerCall(Annotation.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Annotation node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Annotation node) {
			registerCall(Annotation.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Annotation node) {
			super.visit(node);
		}

		@Override
		public boolean visit(MethodReference node) {
			registerCall(MethodReference.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(MethodReference node) {
			super.visit(node);
		}
		@Override
		public void endVisit(MethodReference node) {
			registerCall(MethodReference.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(MethodReference node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Name node) {
			registerCall(Name.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Name node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Name node) {
			registerCall(Name.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Name node) {
			super.visit(node);
		}

		@Override
		public boolean visit(ModuleDirective node) {
			registerCall(ModuleDirective.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(ModuleDirective node) {
			super.visit(node);
		}
		@Override
		public void endVisit(ModuleDirective node) {
			registerCall(ModuleDirective.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(ModuleDirective node) {
			super.visit(node);
		}

		@Override
		public boolean visit(ModulePackageAccess node) {
			registerCall(ModulePackageAccess.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(ModulePackageAccess node) {
			super.visit(node);
		}
		@Override
		public void endVisit(ModulePackageAccess node) {
			registerCall(ModulePackageAccess.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(ModulePackageAccess node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Pattern node) {
			registerCall(Pattern.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Pattern node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Pattern node) {
			registerCall(Pattern.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Pattern node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Statement node) {
			registerCall(Statement.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Statement node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Statement node) {
			registerCall(Statement.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Statement node) {
			super.visit(node);
		}

		@Override
		public boolean visit(Type node) {
			registerCall(Type.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(Type node) {
			super.visit(node);
		}
		@Override
		public void endVisit(Type node) {
			registerCall(Type.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(Type node) {
			super.visit(node);
		}

		@Override
		public boolean visit(AnnotatableType node) {
			registerCall(AnnotatableType.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(AnnotatableType node) {
			super.visit(node);
		}
		@Override
		public void endVisit(AnnotatableType node) {
			registerCall(AnnotatableType.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(AnnotatableType node) {
			super.visit(node);
		}

		@Override
		public boolean visit(VariableDeclaration node) {
			registerCall(VariableDeclaration.class);
			return false;
		}
		@SuppressWarnings("unused") // called reflectively
		public void superVisit(VariableDeclaration node) {
			super.visit(node);
		}
		@Override
		public void endVisit(VariableDeclaration node) {
			registerCall(VariableDeclaration.class);
		}
		@SuppressWarnings("unused") // called reflectively
		public void superEndVisit(VariableDeclaration node) {
			super.visit(node);
		}

		//---- END <REGION TO BE UPDATED IN RESPONSE TO ASTNode HIERARCHY CHANGES> ----------------------

		/**
		 * Verifies that the visit(XX) method in HierarchicalASTVisitor calls
		 * visit(YY), where XX is the name of <code>clazz</code> and YY is the
		 * name of the superclass of clazz.
		 *
		 * <code>clazz</code> must be a <b>proper</b> descendant of ASTNode (<code>clazz</code> is not ASTNode).
		 */
		private static void checkMethodCallsSuperclassMethod(Class<? extends ASTNode> clazz, boolean isLeaf, boolean isEndVisit) {
			Assert.isTrue(ASTNode.class.isAssignableFrom(clazz));
			Assert.isTrue(!ASTNode.class.equals(clazz));

			TestHierarchicalASTVisitor visitor= new TestHierarchicalASTVisitor();
			visitor._checkMethodCallsSuperclassMethod(clazz, isLeaf, isEndVisit);
		}

		/**
		 * This class must have certain methods corresponding to the
		 * ASTNode descendant class <code>clazz</code>.
		 * This method reflectively verifies that they are present.
		 */
		private static void checkRequiredMethodsForNonLeaf(Class<? extends ASTNode> clazz, boolean isEndVisit) {
			Assert.isTrue(ASTNode.class.isAssignableFrom(clazz));
			try {
				TestHierarchicalASTVisitor.class.getDeclaredMethod(getVisitMethodName(isEndVisit), clazz);
			} catch (NoSuchMethodException e) {
				fail("Test must be updated since TestHierarchicalASTVisitor (declared within test class), is missing a method corresponding to non-leaf node class '" + getSimpleName(clazz) + "'");
			}
			try {
				TestHierarchicalASTVisitor.class.getDeclaredMethod(getSuperVisitName(isEndVisit), clazz);
			} catch (NoSuchMethodException e) {
				fail("Test must be updated since TestHierarchicalASTVisitor (declared within test class), is missing a method corresponding to non-leaf node class '" + getSimpleName(clazz) + "'");
			}
		}

		private Class<? extends ASTNode> fNodeClassForCalledMethod= null;

		private void _checkMethodCallsSuperclassMethod(Class<? extends ASTNode> clazz, boolean isLeaf, boolean isEndVisit) {
			/* Invoke a method which will result in the execution of
			 * HierarchicalASTVisitor's implementation of visit(XX), where
			 * XX is the name of clazz.
			 *
			 * If clazz is a leaf, we can invoke visit(XX) directly.
			 * Otherwise, we must invoke superVisit(XX), (in this class)
			 * which calls super.visit(XX), since visit(XX) is overridden in
			 * this class.
			 *
			 * The parameter passed to visit(XX) or superVisit(XX)
			 * is null.  If there ever was any requirement that the
			 * parameter to visit(XX) methods, be non-null, we would simply have
			 * to reflectively instantiate an appropriately typed node.
			 */
			try {
				Method method= TestHierarchicalASTVisitor.class.getMethod(isLeaf ? getVisitMethodName(isEndVisit) : "superVisit", clazz);
				method.invoke(this, new Object[] { null });
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				/* NoSuchMethodException should have already been discovered by
				 * hasRequiredMethodsForNonLeaf(..)
				 */
				e.printStackTrace();
				Assert.isTrue(false);
			}

			/*
			 * Verify that the method invokation caused
			 * a call to visit(YY), where YY is the name of the superclass of
			 * clazz. (Also, verify that no other visit(ZZ) method was called).
			 */
			checkSuperclassMethodCalled(clazz, isEndVisit);
		}
		private void checkSuperclassMethodCalled(Class<? extends ASTNode> clazz, boolean isEndVisit) {
			Assert.isNotNull(clazz.getSuperclass());
			/*
			 * This class' implementations of the visit(YY) methods (for non-
			 * leaf YY) cause fNodeClassForCalledMethod to be set to YY.class.
			 * Such an implementation will be the one executed when a visit(XX)
			 * implementation in HierarchicalASTVisitor calls the visit(YY)
			 * method corresponding to XX's superclass, YY. We check here that
			 * fNodeClassForCalledMethod was set to the superclass of clazz.
			 */
			assertEquals(getSuperMethodNotCalledMessageFor(clazz, isEndVisit), clazz.getSuperclass(), fNodeClassForCalledMethod);
		}
		private String getSuperMethodNotCalledMessageFor(Class<? extends ASTNode> clazz, boolean isEndVisit) {
			return getMethodSignatureFor(clazz, isEndVisit) + " in HierarchicalASTVisitor should call " + getMethodSignatureFor(clazz.getSuperclass(), isEndVisit) + ", the visitor method for its superclass.";
		}

		private void registerCall(Class<? extends ASTNode> nodeClassForMethod) {
			assertNull("""
				The invocation of a visit(XX) method in HierarchicalASTVisitor has caused \
				more than one other visit(XX) method to be called.  Every visit(XX) method in \
				HierarchicalASTVisitor, except visit(ASTNode), should simply call visit(YY), \
				where YY is the superclass of XX.""", fNodeClassForCalledMethod);
			fNodeClassForCalledMethod= nodeClassForMethod;
		}
	}

	private Set<Class<? extends ASTNode>> fLeaves;

	@Test
	public void test() {
		fLeaves= getLeafASTNodeDescendants();
		Set<Class<? extends ASTNode>> allASTNodeDescendants= computeAllDescendantsFromLeaves(fLeaves.iterator(), ASTNode.class);

		checkAllMethodsForHierarchyExist(allASTNodeDescendants.iterator(), false);
		checkAllMethodsForHierarchyExist(allASTNodeDescendants.iterator(), true);
		checkMethodsCallSuperclassMethod(allASTNodeDescendants.iterator(), false);
		checkMethodsCallSuperclassMethod(allASTNodeDescendants.iterator(), true);
	}

	private boolean isLeaf(Class<? extends ASTNode> clazz) {
		return fLeaves.contains(clazz);
	}

	/**
	 * For both HierarchicalASTVisitor and a subsequent part of this test to be correct,
	 * HierarchicalASTVisitor and TestHierarchicalASTVisitor must declare certain methods,
	 * each one corresponding to a class in the ASTNode hierarchy.  Specifically,
	 * HierarchicalASTVisitor must declare a method corresponding to each class in the hierarchy,
	 * whereas TestHierarchicalASTVisitor must declare a pair of methods for each non-leaf
	 * class in the ASTNode hierarchy.
	 *
	 * This method verifies that these required methods exist, and suggests the updates
	 * that are needed to properly maintain the set of methods.
	 */
	private void checkAllMethodsForHierarchyExist(Iterator<Class<? extends ASTNode>> hierarchyClasses, boolean isEndVisit) {
		while (hierarchyClasses.hasNext()) {
			Class<? extends ASTNode> descendant= hierarchyClasses.next();
			checkHierarchicalASTVisitorMethodExistsFor(descendant, isEndVisit);
			if (!isLeaf(descendant))
				TestHierarchicalASTVisitor.checkRequiredMethodsForNonLeaf(descendant, isEndVisit);
		}
	}

	/**
	 * All visit(XX) implementations in HierarchicalASTVisitor, each
	 * corresponding to a class XX, must call visit(YY), where YY is the
	 * superclass of YY, unless XX is ASTNode. This method verifies this using
	 * reflection and a contrived subclass of HierarchicalASTVisitor,
	 * TestHierarchicalASTVisitor.
	 */
	private void checkMethodsCallSuperclassMethod(Iterator<Class<? extends ASTNode>> hierarchyClasses, boolean isEndVisit) {
		while (hierarchyClasses.hasNext()) {
			Class<? extends ASTNode> descendant= hierarchyClasses.next();
			if (!ASTNode.class.equals(descendant))
				TestHierarchicalASTVisitor.checkMethodCallsSuperclassMethod(descendant, isLeaf(descendant), isEndVisit);
		}
	}

	private void checkHierarchicalASTVisitorMethodExistsFor(Class<? extends ASTNode> nodeClass, boolean isEndVisit) {
		Assert.isTrue(ASTNode.class.isAssignableFrom(nodeClass));
		try {
			HierarchicalASTVisitor.class.getDeclaredMethod(getVisitMethodName(isEndVisit), nodeClass);
		} catch (NoSuchMethodException e) {
			String signature= getVisitMethodName(isEndVisit) + "(" + getSimpleName(nodeClass) + ")";
			fail("HierarchicalASTVisitor must be updated to reflect a change in the ASTNode hierarchy.  No method " + signature + " was found in HierarchicalASTVisitor.");
		}
	}

	private static String getVisitMethodName(boolean isEndVisit) {
		return isEndVisit ? "endVisit" : "visit";
	}

	private static String getSuperVisitName(boolean isEndVisit) {
		return isEndVisit ? "superEndVisit" : "superVisit";
	}

	private static String getSimpleName(Class<?> clazz) {
		String qualified= clazz.getName();
		return qualified.substring(qualified.lastIndexOf('.') + 1);
	}

	private static String getMethodSignatureFor(Class<?> clazz, boolean isEndVisit) {
		return getVisitMethodName(isEndVisit) + "(" + getSimpleName(clazz) + ")";
	}

	/**
	 * Finds the set of all descendants of <code>root</code> which are not proper descendants
	 * of a class in the sequence <code>leaves</code>.  This will include <code>root</code>
	 * and all the elements of <code>leaves</code>.
	 */
	private static Set<Class<? extends ASTNode>> computeAllDescendantsFromLeaves(Iterator<Class<? extends ASTNode>> leaves, Class<? extends ASTNode> root) {
		Set<Class<? extends ASTNode>> all= new HashSet<>();
		while (leaves.hasNext()) {
			Class<? extends ASTNode> leaf= leaves.next();
			addAllAncestorsInclusive(leaf, root, all);
		}
		return all;
	}

	private static void addAllAncestorsInclusive(Class<? extends ASTNode> from, Class<? extends ASTNode> to, Set<Class<? extends ASTNode>> set) {
		Assert.isTrue(to.isAssignableFrom(from));
		Assert.isTrue(!from.isInterface());
		Assert.isTrue(!to.isInterface());

		Class<?> ancestor= from;
		while (!ancestor.equals(to)) {
			@SuppressWarnings("unchecked")
			Class<? extends ASTNode> nodeType= (Class<? extends ASTNode>) ancestor;
			set.add(nodeType);
			ancestor= ancestor.getSuperclass();
			if (ancestor == null) {
				Assert.isTrue(false);
				/* not expected, given assertions passed above */
			}
		}
		set.add(to);
	}

	/**
	 * Returns all the leaf node classes (classes with no subclasses) in the
	 * ASTNode . Since every non-leaf ASTNode descendant (incl. ASTNode)
	 * is abstract, the set of leaf ASTNode descendants is the set of concrete
	 * ASTNode descendants.
	 *
	 * If ASTVisitor is maintained, this set will be the set of classes for which
	 * ASTVisitor has visit(..) methods.  We use this property to compute the set,
	 * which means that we are as up-to-date as ASTVisitor (to be more
	 * "up-to-date" would be to require something that HierarchicalASTVisitor would,
	 * semantically, not be able to provide anyway!).
	 */
	private static Set<Class<? extends ASTNode>> getLeafASTNodeDescendants() {
		Set<Class<? extends ASTNode>> result= new HashSet<>();
		for (Method method : ASTVisitor.class.getMethods()) {
			if (isVisitMethod(method)) {
				@SuppressWarnings("unchecked")
					Class<? extends ASTNode> nodeType= (Class<? extends ASTNode>) method.getParameterTypes()[0];
				result.add(nodeType);
			}
		}
		return result;
	}
	private static boolean isVisitMethod(Method method) {
		if (!"visit".equals(method.getName()))
			return false;

		Class<?>[] parameters= method.getParameterTypes();
		return parameters.length == 1 && ASTNode.class.isAssignableFrom(parameters[0]);
	}
}
