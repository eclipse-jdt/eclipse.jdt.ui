/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.IRefactoringSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class InlineConstantRefactoring extends Refactoring {

	private static class InlineTargetCompilationUnit {

		private static class ClassQualification extends StringInsertion {

			public static class ClassQualificationCannotBePerformed extends Exception {

				// not really serializable
				private static final long serialVersionUID= 1L;

				public ClassQualificationCannotBePerformed(String message) {
					super(message);
				}

				public void fillInStatus(RefactoringStatus status, RefactoringStatusContext context) {
					status.addInfo(getMessage(), context);
				}
			}

			public static ClassQualification create(SimpleName toQualify, IntegerMapping nodeToTargetPositionMap) throws ClassQualificationCannotBePerformed {
				ITypeBinding declaringClass= getDeclaringClassBinding(toQualify);
				String declaringClassName= getDeclaringClassName(toQualify);

				if (declaringClassName == null)
					return null;

				return new ClassQualification(declaringClassName, nodeToTargetPositionMap.map(toQualify.getStartPosition()), declaringClass);
			}

			private static String getClassNameQualifiedToTopLevel(ITypeBinding clazz) throws ClassQualificationCannotBePerformed {
				if (clazz.isAnonymous())
					throw new ClassQualificationCannotBePerformed(RefactoringCoreMessages.getString("InlineConstantRefactoring.members_declared_in_anonymous")); //$NON-NLS-1$

				ITypeBinding declaring= clazz.getDeclaringClass();

				String qualifier= declaring == null ? "" : getClassNameQualifiedToTopLevel(declaring) + "."; //$NON-NLS-1$ //$NON-NLS-2$
				return qualifier + clazz.getName();
			}

			private static ITypeBinding getDeclaringClassBinding(SimpleName memberName) {

				IBinding binding= memberName.resolveBinding();
				if (binding instanceof IMethodBinding)
					return ((IMethodBinding) binding).getDeclaringClass();

				if (binding instanceof IVariableBinding)
					return ((IVariableBinding) binding).getDeclaringClass();

				if (binding instanceof ITypeBinding)
					return ((ITypeBinding) binding).getDeclaringClass();

				Assert.isTrue(false);
				return null;

			}

			private static String getDeclaringClassName(SimpleName memberName) throws ClassQualificationCannotBePerformed {
				ITypeBinding declaring= getDeclaringClassBinding(memberName);
				if (declaring == null)
					return null;

				return getClassNameQualifiedToTopLevel(declaring);
			}

			private final ITypeBinding fQualifyingClass;

			private ClassQualification(String qualifier, int insertionPosition, ITypeBinding qualifyingClass) {
				super(insertionPosition, qualifier + "."); //$NON-NLS-1$
				fQualifyingClass= qualifyingClass;
			}

			public ITypeBinding getQualifyingClass() {
				return fQualifyingClass;
			}

		}

		private static class InitializerExpressionRelocationPreparer {

			// ---- Begin InlineTargetCompilationUnit.InitializerExpressionRelocationPreparer.InitializerTraversal
			private static class InitializerTraversal extends HierarchicalASTVisitor {

				private static boolean areInSameClassOrInterface(ASTNode one, ASTNode other) {
					ASTNode onesContainer= getContainingClassOrInterfaceDeclaration(one);
					ASTNode othersContainer= getContainingClassOrInterfaceDeclaration(other);

					if (onesContainer == null || othersContainer == null)
						return false;

					ITypeBinding onesContainerBinding= getTypeBindingForClassOrInterfaceDeclaration(onesContainer);
					ITypeBinding othersContainerBinding= getTypeBindingForClassOrInterfaceDeclaration(othersContainer);

					Assert.isNotNull(onesContainerBinding);
					Assert.isNotNull(othersContainerBinding);

					String onesKey= onesContainerBinding.getKey();
					String othersKey= othersContainerBinding.getKey();

					if (onesKey == null || othersKey == null)
						return false;

					return onesKey.equals(othersKey);
				}

				private static void checkMemberAcceptable(SimpleName memberName) {
					IBinding binding= memberName.resolveBinding();
					Assert.isTrue(binding instanceof IVariableBinding || binding instanceof IMethodBinding || binding instanceof ITypeBinding);

					if (binding instanceof ITypeBinding)
						return;

					if (binding instanceof IVariableBinding)
						Assert.isTrue(((IVariableBinding) binding).isField());

					int modifiers= binding.getModifiers();
					Assert.isTrue(Modifier.isStatic(modifiers), "Relocation of non-static initializer expressions is not currently supported"); //$NON-NLS-1$
				}

				private static ASTNode getContainingClassOrInterfaceDeclaration(ASTNode node) {
					while (node != null && !(node instanceof AbstractTypeDeclaration) && !(node instanceof AnonymousClassDeclaration)) {
						node= node.getParent();
					}
					return node;
				}

				private static ITypeBinding getTypeBindingForClassOrInterfaceDeclaration(ASTNode declaration) {
					if (declaration instanceof AnonymousClassDeclaration)
						return ((AnonymousClassDeclaration) declaration).resolveBinding();

					if (declaration instanceof AbstractTypeDeclaration)
						return ((AbstractTypeDeclaration) declaration).resolveBinding();

					Assert.isTrue(false);
					return null;
				}

				private boolean fCanBePrepared= true;

				private final Expression fInitializer3;// use name other than fInitializer to avoid hiding

				// cache:
				private Set fNamesDeclaredLocallyAtNewLocation;

				private final Expression fNewLocation;

				private final ICompilationUnit fNewLocationCU;

				private List fQualifications= new ArrayList();

				private final RefactoringStatus fStatus2;

				public InitializerTraversal(Expression initializer, Expression newLocation, ICompilationUnit newLocationCU, RefactoringStatus status) {
					fStatus2= status;
					fInitializer3= initializer;
					fNewLocation= newLocation;
					fNewLocationCU= newLocationCU;

					perform(initializer);
				}

				public boolean canInitializerBePrepared() {
					return fCanBePrepared;
				}

				/**
				 * @param scope not a TypeDeclaration
				 * @return Set containing Strings representing simple names
				 */
				private Set getLocallyDeclaredNames(BodyDeclaration scope) {
					Assert.isTrue(!(scope instanceof AbstractTypeDeclaration));

					final Set result= new HashSet();

					if (scope instanceof FieldDeclaration)
						return result;

					scope.accept(new HierarchicalASTVisitor() {

						public boolean visit(AbstractTypeDeclaration node) {
							Assert.isTrue(node.getParent() instanceof TypeDeclarationStatement);

							result.add(node.getName().getIdentifier());
							return false;
						}

						public boolean visit(AnonymousClassDeclaration anonDecl) {
							return false;
						}

						public boolean visit(VariableDeclaration varDecl) {
							result.add(varDecl.getName().getIdentifier());
							return false;
						}
					});
					return result;
				}

				private Set getNamesDeclaredLocallyAtNewLocation() {
					if (fNamesDeclaredLocallyAtNewLocation != null)
						return fNamesDeclaredLocallyAtNewLocation;

					BodyDeclaration enclosingBodyDecl= (BodyDeclaration) ASTNodes.getParent(fNewLocation, BodyDeclaration.class);
					Assert.isTrue(!(enclosingBodyDecl instanceof AbstractTypeDeclaration));

					return fNamesDeclaredLocallyAtNewLocation= getLocallyDeclaredNames(enclosingBodyDecl);
				}

				public ClassQualification[] getQualifications() {
					return (ClassQualification[]) fQualifications.toArray(new ClassQualification[fQualifications.size()]);
				}

				private boolean mayBeShadowedByLocalDeclaration(SimpleName memberName) {
					return getNamesDeclaredLocallyAtNewLocation().contains(memberName.getIdentifier());
				}

				private void perform(Expression initializer) {
					initializer.accept(this);
				}

				private void qualifyMemberName(SimpleName memberName) {
					checkMemberAcceptable(memberName);
					qualifyToTopLevelClass(memberName);
				}

				private void qualifyToTopLevelClass(SimpleName name) {
					try {
						ClassQualification qualification= ClassQualification.create(name, new IntegerMapping() {

							public int map(int position) {
								return position - fInitializer3.getStartPosition();
							}
						});
						if (qualification != null)
							fQualifications.add(qualification);
					} catch (ClassQualification.ClassQualificationCannotBePerformed e) {
						e.fillInStatus(fStatus2, JavaStatusContext.create(fNewLocationCU, fNewLocation));
						fCanBePrepared= false;
					}
				}

				private void qualifyUnqualifiedMemberNameIfNecessary(SimpleName memberName) {
					if (shouldQualify(memberName))
						qualifyMemberName(memberName);
				}

				private boolean shouldQualify(SimpleName memberName) {
					if (!areInSameClassOrInterface(fInitializer3, fNewLocation))
						return true;

					return mayBeShadowedByLocalDeclaration(memberName);
				}

				public boolean visit(ASTNode node) {
					return fCanBePrepared;
				}

				public boolean visit(FieldAccess fieldAccess) {
					fieldAccess.getExpression().accept(this);
					return false;
				}

				public boolean visit(MethodInvocation invocation) {
					if (invocation.getExpression() == null)
						qualifyUnqualifiedMemberNameIfNecessary(invocation.getName());
					else
						invocation.getExpression().accept(this);

					for (Iterator it= invocation.arguments().iterator(); it.hasNext();)
						((Expression) it.next()).accept(this);

					return false;
				}

				public boolean visit(Name name) {
					SimpleName leftmost= getLeftmost(name);

					IBinding leftmostBinding= leftmost.resolveBinding();
					if (leftmostBinding instanceof IVariableBinding || leftmostBinding instanceof IMethodBinding || leftmostBinding instanceof ITypeBinding)
						qualifyUnqualifiedMemberNameIfNecessary(leftmost);

					return false;
				}
			}

			public static String prepareInitializerForLocation(Expression initializer, ICompilationUnit initializerCU, Expression location, ICompilationUnit locationCU, Set newTypes, RefactoringStatus status) throws JavaModelException {
				return new InitializerExpressionRelocationPreparer(initializer, initializerCU, location, locationCU, newTypes, status).prepareInitializer();
			}

			// ---- End InlineTargetCompilationUnit.InitializerExpressionRelocationPreparer.InitializerTraversal

			private final Expression fInitializer2;// use name other than fInitializer to avoid hiding

			private final ICompilationUnit fInitializerCU;

			private final Expression fLocation;

			private final ICompilationUnit fLocationCU;

			private final Set fNewTypes;

			private final RefactoringStatus fStatus;

			private InitializerExpressionRelocationPreparer(Expression initializer, ICompilationUnit initializerCU, Expression location, ICompilationUnit locationCU, Set newTypes, RefactoringStatus status) {
				fInitializer2= initializer;
				fInitializerCU= initializerCU;
				fLocation= location;
				fLocationCU= locationCU;
				fNewTypes= newTypes;
				fStatus= status;
			}

			private String prepareInitializer() throws JavaModelException {
				InitializerTraversal traversal= new InitializerTraversal(fInitializer2, fLocation, fLocationCU, fStatus);

				if (!traversal.canInitializerBePrepared())
					return null;

				ClassQualification[] qualifications= traversal.getQualifications();
				for (int i= 0; i < qualifications.length; i++)
					fNewTypes.add(qualifications[i].getQualifyingClass());

				String originalInitializerString= fInitializerCU.getBuffer().getText(fInitializer2.getStartPosition(), fInitializer2.getLength());
				String result= new MultiInsertionStringEdit(qualifications).applyTo(originalInitializerString);

				if (shouldParenthesizeSubstitute(fInitializer2, fLocation))
					result= "(" + result + ")"; //$NON-NLS-1$ //$NON-NLS-2$

				return result;
			}
			
			private static boolean shouldParenthesizeSubstitute(Expression substitute, Expression location) {
				if (substitute instanceof Assignment)// for esthetic reasons
					return true;

				return ASTNodes.substituteMustBeParenthesized(substitute, location);
			}
		}

		private static interface IntegerMapping {

			public int map(int x);
		}

		private static class MultiInsertionStringEdit extends StringEdit {

			private final StringInsertion[] fOrderedInsertions;

			public MultiInsertionStringEdit(StringInsertion[] insertions) {
				orderInsertions(insertions);
				checkInsertions(insertions);
				fOrderedInsertions= insertions;
			}

			public String applyTo(String target) {
				String result= target;
				for (int i= fOrderedInsertions.length - 1; i >= 0; i--) {
					result= fOrderedInsertions[i].applyTo(result);
				}
				return result;
			}

			private void checkInsertions(StringInsertion[] insertions) {
				for (int i= 1; i < insertions.length; i++) {
					StringInsertion one= insertions[i - 1], other= insertions[i];
					Assert.isTrue(one.compareTo(other) != 0);
				}
			}

			private StringInsertion[] orderInsertions(StringInsertion[] insertions) {
				Arrays.sort(insertions);
				return insertions;
			}
		}

		private abstract static class StringEdit {
			public StringEdit() {}
			public abstract String applyTo(String target);
		}

		private static class StringInsertion extends StringEdit implements Comparable {

			private final int fOffset;

			private final String fToInsert;

			public StringInsertion(int offset, String toInsert) {
				fOffset= offset;
				fToInsert= toInsert;
			}

			public String applyTo(String target) {
				return target.substring(0, fOffset) + fToInsert + target.substring(fOffset);
			}

			/**
			 * 
			 * @see java.lang.Comparable#compareTo(java.lang.Object)
			 */
			public int compareTo(Object other) {
				Assert.isTrue(other instanceof StringInsertion);

				StringInsertion otherInsertion= (StringInsertion) other;

				if (fOffset < otherInsertion.fOffset)
					return -1;
				if (fOffset == otherInsertion.fOffset)
					return 0;
				Assert.isTrue(fOffset > otherInsertion.fOffset);
				return 1;
			}
		}

		private static class TypeReferenceFinder extends HierarchicalASTVisitor {

			public static ITypeBinding[] getReferencedTopLevelTypes(ASTNode tree) {
				return new TypeReferenceFinder().getTopLevelTypesReferenced(tree);
			}

			private List fTypes;

			private ITypeBinding[] getTopLevelTypesReferenced(ASTNode tree) {
				reset();
				tree.accept(this);
				return (ITypeBinding[]) fTypes.toArray(new ITypeBinding[fTypes.size()]);
			}

			private void reset() {
				fTypes= new ArrayList();
			}

			public boolean visit(Name name) {
				SimpleName leftmost= getLeftmost(name);

				IBinding binding= leftmost.resolveBinding();
				if (binding instanceof ITypeBinding)
					fTypes.add(binding);

				return false;
			}
		}

		private final Expression fInitializer;
		private final ICompilationUnit fInitializerUnit;

		/** The references in this compilation unit, represented as AST Nodes in the parsed representation of the compilation unit */
		private final Expression[] fReferences;

		private final CompilationUnitRewrite fCuRewrite;
		
		private final InlineConstantRefactoring fRefactoring;

		private InlineTargetCompilationUnit(CompilationUnitRewrite cuRewrite, Name singleReference, InlineConstantRefactoring refactoring) throws JavaModelException {
			Expression initializer= refactoring.getInitializer();
			ICompilationUnit initializerUnit= refactoring.getDeclaringCompilationUnit();
			
			fReferences= new Expression[] { getQualifiedReference(singleReference)};
			fCuRewrite= cuRewrite;
			fRefactoring= refactoring;
			fInitializer= initializer;
			fInitializerUnit= initializerUnit;
		}

		private InlineTargetCompilationUnit(CompilationUnitRewrite cuRewrite, SearchMatch[] references, InlineConstantRefactoring refactoring) throws JavaModelException {
			Expression initializer= refactoring.getInitializer();
			ICompilationUnit initializerUnit= refactoring.getDeclaringCompilationUnit();
			
			fCuRewrite= cuRewrite;
			fRefactoring= refactoring;
			fInitializer= initializer;
			fInitializerUnit= initializerUnit;

			fReferences= new Expression[references.length];

			CompilationUnit cuNode= fCuRewrite.getRoot();
			for (int i= 0; i < references.length; i++) {
				ASTNode node= NodeFinder.perform(cuNode, references[i].getOffset(), references[i].getLength());
				Assert.isTrue(node instanceof Name);
				fReferences[i]= getQualifiedReference((Name) node);
			}

		}

		private static Expression getQualifiedReference(Name fieldName) {
			if (doesParentQualify(fieldName))
				return (Expression) fieldName.getParent();

			return fieldName;
		}

		private static boolean doesParentQualify(Name fieldName) {
			ASTNode parent= fieldName.getParent();
			Assert.isNotNull(parent);

			if (parent instanceof FieldAccess && ((FieldAccess) parent).getName() == fieldName)
				return true;

			if (parent instanceof QualifiedName && ((QualifiedName) parent).getName() == fieldName)
				return true;

			if (parent instanceof MethodInvocation && ((MethodInvocation) parent).getName() == fieldName)
				return true;

			return false;
		}

		public CompilationUnitChange getChange() throws CoreException {
			for (int i= 0; i < fReferences.length; i++)
				inlineReference(fReferences[i]);
			
			if (fCuRewrite.getCu().equals(fInitializerUnit))
				removeConstantDeclarationIfNecessary();
			
			fCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				public SourceRange computeSourceRange(ASTNode node) {
					return new TargetSourceRangeComputer.SourceRange(node.getStartPosition(), node.getLength());
				}
			});
			return fCuRewrite.createChange();
		}

		private void inlineReference(Expression reference) throws CoreException {
			Set newTypes= new HashSet();
			
			ASTNode importDecl= ASTNodes.getParent(reference, ImportDeclaration.class);
			if (importDecl != null)
				return; // don't inline into static imports

			String modifiedInitializer= prepareInitializerFor(reference, newTypes, new RefactoringStatus()); //TODO: collect problems?
			if (modifiedInitializer == null)
				return;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.getString("InlineConstantRefactoring.Inline")); //$NON-NLS-1$
			ASTNode newReference= fCuRewrite.getASTRewrite().createStringPlaceholder(modifiedInitializer, reference.getNodeType());
			fCuRewrite.getASTRewrite().replace(reference, newReference, msg);
			
			addImportsForTypesInOriginalInitializer();
			addImportsForNewTypes(newTypes);
		}

		private String prepareInitializerFor(Expression reference, Set newTypes, RefactoringStatus status) throws JavaModelException {
			return InitializerExpressionRelocationPreparer.prepareInitializerForLocation(fInitializer, fInitializerUnit, reference, fCuRewrite.getCu(), newTypes, status);
		}
		
		private void addImportsForNewTypes(Set newTypes) {
			for (Iterator it= newTypes.iterator(); it.hasNext();)
				fCuRewrite.getImportRewrite().addImport(((ITypeBinding) it.next()));
		}

		private void addImportsForTypesInOriginalInitializer() {
			ITypeBinding[] types= TypeReferenceFinder.getReferencedTopLevelTypes(fInitializer);
			for (int i= 0; i < types.length; i++)
				fCuRewrite.getImportRewrite().addImport(types[i]);
		}

		private void removeConstantDeclarationIfNecessary() throws CoreException {
			if (! fRefactoring.getRemoveDeclaration())
				return;

			VariableDeclarationFragment declaration= fRefactoring.getDeclaration();
			FieldDeclaration parentDeclaration= (FieldDeclaration) declaration.getParent();
			ASTNode toRemove;
			if (parentDeclaration.fragments().size() == 1)
				toRemove= parentDeclaration;
			else
				toRemove= declaration;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.getString("InlineConstantRefactoring.remove_declaration")); //$NON-NLS-1$
			fCuRewrite.getASTRewrite().remove(toRemove, msg);
		}
	}

	// ---- End InlineTargetCompilationUnit ----------------------------------------------------------------------------------------------

	private static SimpleName getLeftmost(Name name) {
		if (name instanceof SimpleName)
			return (SimpleName) name;

		return getLeftmost(((QualifiedName) name).getQualifier());
	}

	private final int fSelectionStart;
	private final int fSelectionLength;
	
	private final ICompilationUnit fSelectionCu;
	private CompilationUnitRewrite fSelectionCuRewrite;
	private Name fSelectedConstantName;
	
	IField fField;
	private CompilationUnitRewrite fDeclarationCuRewrite;
	private VariableDeclarationFragment fDeclaration;
	private boolean fDeclarationSelected;
	private boolean fDeclarationSelectedChecked= false;
	private boolean fInitializerAllStaticFinal;
	private boolean fInitializerChecked= false;

	private boolean fRemoveDeclaration= false;
	private boolean fReplaceAllReferences= true;

	private CompilationUnitChange[] fChanges;
	
	private InlineConstantRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionCu= cu;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionCuRewrite= new CompilationUnitRewrite(fSelectionCu);
		fSelectedConstantName= findConstantNameNode();
	}

	private Name findConstantNameNode() {
		ASTNode node= NodeFinder.perform(fSelectionCuRewrite.getRoot(), fSelectionStart, fSelectionLength);
		if (node == null)
			return null;
		if (node instanceof FieldAccess)
			node= ((FieldAccess) node).getName();
		if (node.getParent() instanceof EnumConstantDeclaration)
			return null;
		if (!(node instanceof Name))
			return null;
		Name name= (Name) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return null;
		IVariableBinding variableBinding= (IVariableBinding) binding;
		if (!variableBinding.isField() || variableBinding.isEnumConstant())
			return null;
		int modifiers= binding.getModifiers();
		if (! (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)))
			return null;

		return name;
	}

	public static InlineConstantRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
		InlineConstantRefactoring ref= new InlineConstantRefactoring(cu, selectionStart, selectionLength);
		if (ref.checkStaticFinalConstantNameSelected().hasFatalError())
			return null;
		return ref;
	}

	private RefactoringStatus checkStaticFinalConstantNameSelected() {
		if (fSelectedConstantName == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.static_final_field"), null, Corext.getPluginId(), RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED, null); //$NON-NLS-1$

		return new RefactoringStatus();
	}

	public static boolean isAvailable(IField field) throws JavaModelException {
		return Checks.isAvailable(field) && JdtFlags.isStatic(field) && JdtFlags.isFinal(field) && !JdtFlags.isEnum(field);
	}

	public String getName() {
		return RefactoringCoreMessages.getString("InlineConstantRefactoring.name"); //$NON-NLS-1$
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 3); //$NON-NLS-1$

			if (!fSelectionCu.isStructureKnown())
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.syntax_errors"), null, Corext.getPluginId(), RefactoringStatusCodes.SYNTAX_ERRORS, null); //$NON-NLS-1$

			RefactoringStatus result= checkStaticFinalConstantNameSelected();
			if (result.hasFatalError())
				return result;
			
			result.merge(findField());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(findDeclaration());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(checkInitializer());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			return result;
			
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus findField() throws JavaModelException {
		fField= Bindings.findField((IVariableBinding) fSelectedConstantName.resolveBinding(), fSelectionCu.getJavaProject());
		if (fField != null && ! fField.exists())
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.local_anonymous_unsupported"), null, Corext.getPluginId(), RefactoringStatusCodes.LOCAL_AND_ANONYMOUS_NOT_SUPPORTED, null); //$NON-NLS-1$
		
		return null;
	}

	private RefactoringStatus findDeclaration() throws JavaModelException {
		fDeclarationSelectedChecked= true;
		fDeclarationSelected= false;
		ASTNode parent= fSelectedConstantName.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment parentDeclaration= (VariableDeclarationFragment) parent;
			if (parentDeclaration.getName() == fSelectedConstantName) {
				fDeclarationSelected= true;
				fDeclarationCuRewrite= fSelectionCuRewrite;
				fDeclaration= (VariableDeclarationFragment) fSelectedConstantName.getParent();
				return null;
			}
		}
		
		VariableDeclarationFragment declaration= (VariableDeclarationFragment) fSelectionCuRewrite.getRoot().findDeclaringNode(fSelectedConstantName.resolveBinding());
		if (declaration != null) {
			fDeclarationCuRewrite= fSelectionCuRewrite;
			fDeclaration= declaration;
			return null;
		}

		if (fField.getCompilationUnit() == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.binary_file"), null, Corext.getPluginId(), RefactoringStatusCodes.DECLARED_IN_CLASSFILE, null); //$NON-NLS-1$
		
		fDeclarationCuRewrite= new CompilationUnitRewrite(fField.getCompilationUnit());
		fDeclaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fField, fDeclarationCuRewrite.getRoot());
		return null;
	}

	private RefactoringStatus checkInitializer() {
		Expression initializer= getInitializer();
		if (initializer == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.blank_finals"), null, Corext.getPluginId(), RefactoringStatusCodes.CANNOT_INLINE_BLANK_FINAL, null); //$NON-NLS-1$

		fInitializerAllStaticFinal= ConstantChecks.isStaticFinalConstant((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(initializer));
		fInitializerChecked= true;
		return new RefactoringStatus();
	}

	private VariableDeclarationFragment getDeclaration() throws JavaModelException {
		return fDeclaration;
	}

	private Expression getInitializer() {
		return fDeclaration.getInitializer();
	}
	
	private ICompilationUnit getDeclaringCompilationUnit() throws JavaModelException {
		return fField.getCompilationUnit();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		
		try {
			List/*<CompilationUnitChange>*/changes= new ArrayList();

			// prepareTargets:
			if (getReplaceAllReferences()) {
				SearchResultGroup[] searchResultGroups= findReferences(pm, result);
				for (int i= 0; i < searchResultGroups.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					SearchResultGroup group= searchResultGroups[i];
					ICompilationUnit cu= group.getCompilationUnit();

					SearchMatch[] searchResults= group.getSearchResults();
					InlineTargetCompilationUnit targetCompilationUnit= new InlineTargetCompilationUnit(
							getCuRewrite(cu), searchResults, this);
					changes.add(targetCompilationUnit.getChange());
				}

			} else {
				Assert.isTrue(! isDeclarationSelected());
				InlineTargetCompilationUnit targetForOnlySelectedReference= new InlineTargetCompilationUnit(
						fSelectionCuRewrite, fSelectedConstantName, this);
				changes.add(targetForOnlySelectedReference.getChange());
			}

			if (result.hasFatalError())
				return result;

			if (getRemoveDeclaration()) {
				boolean declarationRemoved= false;
				for (Iterator iter= changes.iterator(); iter.hasNext();) {
					CompilationUnitChange change= (CompilationUnitChange) iter.next();
					if (change.getCompilationUnit().equals(fDeclarationCuRewrite.getCu())) {
						declarationRemoved= true;
						break;
					}
				}
				if (! declarationRemoved) {
					InlineTargetCompilationUnit targetForDeclaration= new InlineTargetCompilationUnit(
							fDeclarationCuRewrite, new SearchMatch[0], this);
					changes.add(targetForDeclaration.getChange());
				}
			}

			ICompilationUnit[] cus= new ICompilationUnit[changes.size()];
			for (int i= 0; i < changes.size(); i++) {
				CompilationUnitChange change= (CompilationUnitChange) changes.get(i);
				cus[i]= change.getCompilationUnit();
			}
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(cus), getValidationContext()));

			pm.worked(1);

			fChanges= (CompilationUnitChange[]) changes.toArray(new CompilationUnitChange[changes.size()]);

			return result;
			
		} finally {
			fSelectionCuRewrite= null;
			fSelectedConstantName= null;
			fDeclarationCuRewrite= null;
			fDeclaration= null;
			pm.done();
		}
	}

	private CompilationUnitRewrite getCuRewrite(ICompilationUnit cu) {
		CompilationUnitRewrite cuRewrite;
		if (cu.equals(fSelectionCu))
			cuRewrite= fSelectionCuRewrite;
		else if (cu.equals(fField.getCompilationUnit()))
			cuRewrite= fDeclarationCuRewrite;
		else
			cuRewrite= new CompilationUnitRewrite(cu);
		return cuRewrite;
	}

	private SearchResultGroup[] findReferences(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fField, IJavaSearchConstants.REFERENCES));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(fField));
		engine.setStatus(status);
		engine.setRequestor(new IRefactoringSearchRequestor() {
			public boolean acceptSearchMatch(SearchMatch match) {
				return ! match.isInsideDocComment();
			}
		});
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("InlineConstantRefactoring.preview"), 2); //$NON-NLS-1$
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("InlineConstantRefactoring.inline")); //$NON-NLS-1$
			result.addAll(fChanges);
			return result;
		} finally {
			pm.done();
			fChanges= null;
		}
	}
	

	private void checkInvariant() {
		if (isDeclarationSelected())
			Assert.isTrue(fReplaceAllReferences);
		if (fRemoveDeclaration)
			Assert.isTrue(fReplaceAllReferences);
	}

	public boolean getRemoveDeclaration() {
		checkInvariant();
		return fRemoveDeclaration;
	}

	public boolean getReplaceAllReferences() {
		checkInvariant();
		return fReplaceAllReferences;
	}

	public boolean isDeclarationSelected() {
		Assert.isTrue(fDeclarationSelectedChecked);
		return fDeclarationSelected;
	}

	public boolean isInitializerAllStaticFinal() {
		Assert.isTrue(fInitializerChecked);
		return fInitializerAllStaticFinal;
	}

	public void setRemoveDeclaration(boolean removeDeclaration) {
		fRemoveDeclaration= removeDeclaration;
		checkInvariant();
	}

	public void setReplaceAllReferences(boolean replaceAllReferences) {
		fReplaceAllReferences= replaceAllReferences;
		checkInvariant();
	}
}