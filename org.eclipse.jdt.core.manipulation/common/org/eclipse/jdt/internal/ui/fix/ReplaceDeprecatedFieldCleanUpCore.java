/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.ReplaceDeprecatedFieldFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class ReplaceDeprecatedFieldCleanUpCore extends AbstractMultiFix {

	public ReplaceDeprecatedFieldCleanUpCore() {
		this(Collections.emptyMap());
	}

	public ReplaceDeprecatedFieldCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REPLACE_DEPRECATED_FIELDS);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REPLACE_DEPRECATED_FIELDS)) {
			return new String[] { MultiFixMessages.ReplaceDeprecatedFieldsCleanUp_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("Class E {\n"); //$NON-NLS-1$
		bld.append("  /**\n"); //$NON-NLS-1$
		bld.append("   * @deprecated use {@link K#field2} instead\n"); //$NON-NLS-1$
		bld.append("   */\n"); //$NON-NLS-1$
		bld.append("  @Deprecated\n"); //$NON-NLS-1$
		bld.append("  public int field1;\n"); //$NON-NLS-1$
		bld.append("}\n\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.REPLACE_DEPRECATED_FIELDS)) {
			bld.append("return K.field2;\n"); //$NON-NLS-1$
		} else {
			bld.append("return E.field1;\n"); //$NON-NLS-1$
		}
		return bld.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REPLACE_DEPRECATED_FIELDS)) {
			return null;
		}
		if (compilationUnit == null)
			return null;

		final List<ReplaceDeprecatedFieldFixCore.ReplaceDeprecatedFieldProposalOperation> deprecatedFields= new ArrayList<>();
		ASTVisitor visitor= new ASTVisitor() {
			@Override
			public boolean visit(QualifiedName node) {
				String replacement= QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node);
				if (replacement != null) {
					deprecatedFields.add(new ReplaceDeprecatedFieldFixCore.ReplaceDeprecatedFieldProposalOperation(node, replacement));
				}
				return false;
			}
			@Override
			public boolean visit(SimpleName node) {
				if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY) {
					return true;
				}
				String replacement= QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node);
				if (replacement != null) {
					deprecatedFields.add(new ReplaceDeprecatedFieldFixCore.ReplaceDeprecatedFieldProposalOperation(node, replacement));
				}
				return false;
			}
			@Override
			public boolean visit(FieldAccess node) {
				String replacement= QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node);
				if (replacement != null) {
					deprecatedFields.add(new ReplaceDeprecatedFieldFixCore.ReplaceDeprecatedFieldProposalOperation(node, replacement));
				}
				return false;
			}
			@Override
			public boolean visit(SuperFieldAccess node) {
				String replacement= QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node);
				if (replacement != null) {
					deprecatedFields.add(new ReplaceDeprecatedFieldFixCore.ReplaceDeprecatedFieldProposalOperation(node, replacement));
				}
				return false;
			}
		};
		compilationUnit.accept(visitor);
		if (deprecatedFields.isEmpty()) {
			return null;
		}
		return new ReplaceDeprecatedFieldFixCore(getPreview(), compilationUnit, deprecatedFields.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		if (!isEnabled(CleanUpConstants.REPLACE_DEPRECATED_FIELDS)) {
			return 0;
		}
		if (compilationUnit == null)
			return 0;

		final List<ASTNode> deprecatedFields= new ArrayList<>();
		ASTVisitor visitor= new ASTVisitor() {
			@Override
			public boolean visit(QualifiedName node) {
				if (QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node) != null) {
					deprecatedFields.add(node);
				}
				return true;
			}
			@Override
			public boolean visit(FieldAccess node) {
				if (QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node) != null) {
					deprecatedFields.add(node);
				}
				return true;
			}
			@Override
			public boolean visit(SuperFieldAccess node) {
				if (QuickAssistProcessorUtil.getDeprecatedFieldReplacement(node) != null) {
					deprecatedFields.add(node);
				}
				return true;
			}
		};
		compilationUnit.accept(visitor);
		return deprecatedFields.size();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

}
