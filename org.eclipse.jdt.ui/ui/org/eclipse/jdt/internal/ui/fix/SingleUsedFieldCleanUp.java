/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that refactors a field into a local variable if its use is only local:
 * <ul>
 * <li>The previous value should not be read,</li>
 * <li>The field should be private,</li>
 * <li>The field should not be final,</li>
 * <li>The field should be primitive,</li>
 * <li>The field should not have annotations.</li>
 * </ul>
 */
public class SingleUsedFieldCleanUp extends AbstractMultiFix {
	private static final class FieldUseVisitor extends ASTVisitor {
		private final SimpleName field;
		private final Set<SimpleName> occurrences= new LinkedHashSet<>();

		private FieldUseVisitor(final SimpleName field) {
			this.field= field;
		}

		@Override
		public boolean visit(final SimpleName aVariable) {
			if (field != aVariable
					&& ASTNodes.isSameVariable(field, aVariable)) {
				occurrences.add(aVariable);
			}

			return true;
		}

		private Set<SimpleName> getOccurrences() {
			return occurrences;
		}
	}

	public SingleUsedFieldCleanUp() {
		this(Collections.emptyMap());
	}

	public SingleUsedFieldCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.SINGLE_USED_FIELD);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.SINGLE_USED_FIELD)) {
			return new String[] { MultiFixMessages.SingleUsedFieldCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("public class MyClass {\n"); //$NON-NLS-1$

		if (!isEnabled(CleanUpConstants.SINGLE_USED_FIELD)) {
			bld.append("    private long singleUsedField;\n"); //$NON-NLS-1$
			bld.append("\n"); //$NON-NLS-1$
		}

		bld.append("    public void myMethod() {\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.SINGLE_USED_FIELD)) {
			bld.append("        long singleUsedField = 123;\n"); //$NON-NLS-1$
		} else {
			bld.append("        singleUsedField = 123;\n"); //$NON-NLS-1$
		}

		bld.append("        System.out.println(singleUsedField);\n"); //$NON-NLS-1$
		bld.append("    }\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.SINGLE_USED_FIELD)) {
			bld.append("\n\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SINGLE_USED_FIELD)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final TypeDeclaration visited) {
				for (FieldDeclaration field : visited.getFields()) {
					if (!maybeReplaceFieldByLocalVariable(visited, field)) {
						return false;
					}
				}

				return true;
			}

			private boolean maybeReplaceFieldByLocalVariable(final TypeDeclaration visited, final FieldDeclaration field) {
				if (Modifier.isPrivate(field.getModifiers())
						&& !Modifier.isFinal(field.getModifiers())
						&& !hasAnnotation(field)
						&& field.getType().isPrimitiveType()) {
					for (Object object : field.fragments()) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment) object;

						if (!maybeReplaceFragmentByLocalVariable(visited, field, fragment)) {
							return false;
						}
					}
				}

				return true;
			}

			private boolean maybeReplaceFragmentByLocalVariable(final TypeDeclaration visited, final FieldDeclaration field,
					final VariableDeclarationFragment fragment) {
				if (fragment.getInitializer() != null && !ASTNodes.isPassiveWithoutFallingThrough(fragment.getInitializer())) {
					return true;
				}

				FieldUseVisitor fieldUseVisitor= new FieldUseVisitor(fragment.getName());
				visited.getRoot().accept(fieldUseVisitor);
				Set<SimpleName> occurrences= fieldUseVisitor.getOccurrences();

				MethodDeclaration oneMethodDeclaration= null;

				for (SimpleName occurrence : occurrences) {
					MethodDeclaration currentMethodDeclaration= ASTNodes.getTypedAncestor(occurrence, MethodDeclaration.class);

					if (isVariableDeclaration(occurrence)
							|| isExternalField(occurrence)
							|| currentMethodDeclaration == null
							|| oneMethodDeclaration != null && currentMethodDeclaration != oneMethodDeclaration) {
						return true;
					}

					oneMethodDeclaration= currentMethodDeclaration;
				}

				if (oneMethodDeclaration == null) {
					return true;
				}

				boolean isReassigned= isAlwaysErased(occurrences);

				if (isReassigned) {
					SimpleName reassignment= findReassignment(occurrences);

					if (reassignment != null) {
						ASTNode parent= reassignment.getParent();

						if (parent instanceof FieldAccess
								&& reassignment.getLocationInParent() == FieldAccess.NAME_PROPERTY
								&& parent.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
							parent= parent.getParent();
						}

						if (parent instanceof Assignment) {
							rewriteOperations.add(new SingleUsedFieldOperation(field, fragment, reassignment, occurrences));
							return false;
						}
					}
				}

				return true;
			}

			private SimpleName findReassignment(final Set<SimpleName> occurrences) {
				for (SimpleName reassignment : occurrences) {
					if (isReassigned(reassignment) && isReassignmentForAll(reassignment, occurrences)) {
						return reassignment;
					}
				}

				return null;
			}

			private boolean isReassignmentForAll(final SimpleName reassignment, final Set<SimpleName> occurrences) {
				for (SimpleName occurrence : occurrences) {
					if (reassignment != occurrence) {
						ASTNode astNode= ASTNodes.getFirstAncestorOrNull(occurrence, Statement.class, LambdaExpression.class, AnonymousClassDeclaration.class);

						if (!(astNode instanceof Statement)) {
							return false;
						}

						Statement statement= (Statement) astNode;
						boolean isReassigned= false;

						while (statement != null) {
							Assignment assignment= ASTNodes.asExpression(statement, Assignment.class);

							if (assignment != null
									&& ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								SimpleName field= ASTNodes.getField(assignment.getLeftHandSide());

								if (field == reassignment) {
									isReassigned= true;
									break;
								}
							}

							statement= ASTNodes.getPreviousStatement(statement);
						}

						if (!isReassigned) {
							return false;
						}
					}
				}

				return true;
			}

			private boolean isAlwaysErased(final Set<SimpleName> occurrences) {
				for (SimpleName occurrence : occurrences) {
					if (!isReassigned(occurrence)) {
						Statement statement= ASTNodes.getTypedAncestor(occurrence, Statement.class);
						boolean isReassigned= false;

						while (statement != null) {
							statement= ASTNodes.getPreviousStatement(statement);
							Assignment assignment= ASTNodes.asExpression(statement, Assignment.class);

							if (assignment != null
									&& ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								SimpleName field= ASTNodes.getField(assignment.getLeftHandSide());

								if (ASTNodes.areSameVariables(field, occurrence)) {
									isReassigned= true;
									break;
								}
							}
						}

						if (!isReassigned) {
							return false;
						}
					}
				}

				return true;
			}

			private boolean isReassigned(final SimpleName occurrence) {
				Expression expression= occurrence;

				if (expression.getParent() instanceof FieldAccess) {
					expression= (FieldAccess) expression.getParent();
				}

				return expression.getParent() instanceof Assignment
						&& expression.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY
						&& ASTNodes.hasOperator((Assignment) expression.getParent(), Assignment.Operator.ASSIGN);
			}

			private boolean isExternalField(final SimpleName occurrence) {
				FieldAccess fieldAccess= ASTNodes.as(occurrence, FieldAccess.class);

				if (fieldAccess != null) {
					ThisExpression thisExpression= ASTNodes.as(fieldAccess.getExpression(), ThisExpression.class);

					if (thisExpression == null || thisExpression.getQualifier() != null) {
						return true;
					}
				}

				return ASTNodes.is(occurrence, QualifiedName.class);
			}

			private boolean isVariableDeclaration(final SimpleName occurrence) {
				switch (occurrence.getParent().getNodeType()) {
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
				case ASTNode.VARIABLE_DECLARATION_STATEMENT:
					return occurrence.getLocationInParent() == SingleVariableDeclaration.NAME_PROPERTY;

				case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
					return occurrence.getLocationInParent() == VariableDeclarationExpression.FRAGMENTS_PROPERTY;

				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					return occurrence.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY;

				default:
					return false;
				}
			}

			private boolean hasAnnotation(final FieldDeclaration field) {
				List<IExtendedModifier> modifiers= field.modifiers();
				return modifiers.stream().anyMatch(IExtendedModifier::isAnnotation);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.SingleUsedFieldCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class SingleUsedFieldOperation extends CompilationUnitRewriteOperation {
		private final FieldDeclaration field;
		private final VariableDeclarationFragment fragment;
		private final SimpleName reassignment;
		private final Set<SimpleName> occurrences;

		public SingleUsedFieldOperation(final FieldDeclaration field, final VariableDeclarationFragment fragment, final SimpleName reassignment, Set<SimpleName> occurrences) {
			this.field= field;
			this.fragment= fragment;
			this.reassignment= reassignment;
			this.occurrences= occurrences;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup groupOldFieldDeclaration= createTextEditGroup(MultiFixMessages.SingleUsedFieldCleanUp_description_old_field_declaration, cuRewrite);
			TextEditGroup groupNewLocalVar= createTextEditGroup(MultiFixMessages.SingleUsedFieldCleanUp_description_new_local_var_declaration, cuRewrite);
			TextEditGroup groupUsesOfTheVar= createTextEditGroup(MultiFixMessages.SingleUsedFieldCleanUp_description_uses_of_the_var, cuRewrite);

			boolean isFieldKept= field.fragments().size() != 1;

			if (isFieldKept) {
				rewrite.remove(fragment, groupOldFieldDeclaration);
				ASTNodes.replaceButKeepComment(rewrite, field.getType(), rewrite.createCopyTarget(field.getType()), groupOldFieldDeclaration);
			} else {
				rewrite.remove(field, groupOldFieldDeclaration);
			}

			Assignment reassignmentAssignment= ASTNodes.getTypedAncestor(reassignment, Assignment.class);

			VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
			newFragment.setName(ASTNodes.createMoveTarget(rewrite, reassignment));
			newFragment.setInitializer(ASTNodes.createMoveTarget(rewrite, reassignmentAssignment.getRightHandSide()));
			List<Dimension> extraDimensions= fragment.extraDimensions();
			List<Dimension> newExtraDimensions= newFragment.extraDimensions();
			newExtraDimensions.addAll(ASTNodes.createMoveTarget(rewrite, extraDimensions));

			VariableDeclarationStatement newDeclareStatement= ast.newVariableDeclarationStatement(newFragment);
			newDeclareStatement.setType(isFieldKept ? ASTNodes.createMoveTarget(rewrite, field.getType()) : (Type) rewrite.createCopyTarget(field.getType()));
			List<IExtendedModifier> modifiers= field.modifiers();
			List<IExtendedModifier> newModifiers= newDeclareStatement.modifiers();

			for (IExtendedModifier iExtendedModifier : modifiers) {
				Modifier modifier= (Modifier) iExtendedModifier;

				if (!modifier.isPrivate() && !modifier.isStatic()) {
					newModifiers.add(isFieldKept ? ASTNodes.createMoveTarget(rewrite, modifier) : (Modifier) rewrite.createCopyTarget(modifier));
				}
			}

			ASTNodes.replaceButKeepComment(rewrite, ASTNodes.getTypedAncestor(reassignmentAssignment, Statement.class),
					newDeclareStatement, groupNewLocalVar);

			for (SimpleName occurrence : occurrences) {
				if (occurrence != reassignment && occurrence.getParent() instanceof FieldAccess) {
					ASTNodes.replaceButKeepComment(rewrite, occurrence.getParent(), occurrence, groupUsesOfTheVar);
				}
			}
		}
	}
}
