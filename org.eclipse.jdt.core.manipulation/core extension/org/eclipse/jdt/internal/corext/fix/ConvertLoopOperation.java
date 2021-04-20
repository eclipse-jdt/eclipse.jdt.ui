/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;

public abstract class ConvertLoopOperation extends CompilationUnitRewriteOperation {

	protected static final String FOR_LOOP_ELEMENT_IDENTIFIER= "element"; //$NON-NLS-1$

	protected static final IStatus ERROR_STATUS= new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, ""); //$NON-NLS-1$

	private static final Map<String, String> IRREG_NOUNS= Stream.of(
			new AbstractMap.SimpleImmutableEntry<>("Children", "Child"), //$NON-NLS-1$ //$NON-NLS-2$
			new AbstractMap.SimpleImmutableEntry<>("Entries", "Entry"), //$NON-NLS-1$ //$NON-NLS-2$
			new AbstractMap.SimpleImmutableEntry<>("Proxies", "Proxy"), //$NON-NLS-1$ //$NON-NLS-2$
			new AbstractMap.SimpleImmutableEntry<>("Indices", "Index")) //$NON-NLS-1$ //$NON-NLS-2$
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

	private static final Set<String> NO_BASE_TYPES	= Stream.of(
			"integers", //$NON-NLS-1$
			"floats", //$NON-NLS-1$
			"doubles", //$NON-NLS-1$
			"booleans", //$NON-NLS-1$
			"bytes", //$NON-NLS-1$
			"chars", //$NON-NLS-1$
			"shorts", //$NON-NLS-1$
			"longs") //$NON-NLS-1$
			.collect(Collectors.toSet());

	private static final Set<String> CUT_PREFIX= Stream.of("all") //$NON-NLS-1$
			.collect(Collectors.toSet());

	private static final Set<String> IRREG_ENDINGS= Stream.of(
			"xes", //$NON-NLS-1$
			"ies", //$NON-NLS-1$
			"oes", //$NON-NLS-1$
			"ses", //$NON-NLS-1$
			"hes", //$NON-NLS-1$
			"zes", //$NON-NLS-1$
			"ves", //$NON-NLS-1$
			"ces", //$NON-NLS-1$
			"ss", //$NON-NLS-1$
			"is", //$NON-NLS-1$
			"us", //$NON-NLS-1$
			"os", //$NON-NLS-1$
			"as") //$NON-NLS-1$
			.collect(Collectors.toSet());

	private final ForStatement fStatement;
	private ConvertLoopOperation fOperation;
	private ConvertLoopOperation fChildLoopOperation;
	private final String[] fUsedNames;

	public ConvertLoopOperation(ForStatement statement, String[] usedNames) {
		fStatement= statement;
		fUsedNames= usedNames;
	}

	public void setBodyConverter(ConvertLoopOperation operation) {
		fOperation= operation;
	}

	public void setChildLoopOperation(ConvertLoopOperation operation) {
		fChildLoopOperation= operation;
	}

	public ConvertLoopOperation getChildLoopOperation() {
		return fChildLoopOperation;
	}

	public abstract String getIntroducedVariableName();

	public abstract IStatus satisfiesPreconditions();

	protected abstract Statement convert(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModelCore positionGroups) throws CoreException;

	protected ForStatement getForStatement() {
		return fStatement;
	}

	protected Statement getBody(CompilationUnitRewrite cuRewrite, TextEditGroup group, LinkedProposalModelCore positionGroups) throws CoreException {
		if (fOperation != null) {
			return fOperation.convert(cuRewrite, group, positionGroups);
		} else {
			return (Statement)cuRewrite.getASTRewrite().createMoveTarget(getForStatement().getBody());
		}
	}

	protected String[] getUsedVariableNames() {
		final List<String> results= new ArrayList<>();

		ForStatement forStatement= getForStatement();
		CompilationUnit root= (CompilationUnit)forStatement.getRoot();

		Collection<String> variableNames= new ScopeAnalyzer(root).getUsedVariableNames(forStatement.getStartPosition(), forStatement.getLength());
		results.addAll(variableNames);

		forStatement.accept(new GenericVisitor() {
			@Override
			public boolean visit(SingleVariableDeclaration node) {
				results.add(node.getName().getIdentifier());
				return super.visit(node);
			}

			@Override
			public boolean visit(VariableDeclarationFragment fragment) {
				results.add(fragment.getName().getIdentifier());
				return super.visit(fragment);
			}
		});

		results.addAll(Arrays.asList(fUsedNames));

		return results.toArray(new String[results.size()]);
	}

	@Override
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore positionGroups) throws CoreException {
		TextEditGroup group= createTextEditGroup(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, cuRewrite);
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		TightSourceRangeComputer rangeComputer;
		if (rewrite.getExtendedSourceRangeComputer() instanceof TightSourceRangeComputer) {
			rangeComputer= (TightSourceRangeComputer)rewrite.getExtendedSourceRangeComputer();
		} else {
			rangeComputer= new TightSourceRangeComputer();
		}
		rangeComputer.addTightSourceNode(getForStatement());
		rewrite.setTargetSourceRangeComputer(rangeComputer);

		Statement statement= convert(cuRewrite, group, positionGroups);
		ASTNode node= getForStatement();
		ASTNodes.replaceButKeepComment(rewrite, node, statement, group);
	}

	public static String modifybasename(String suggestedName) {
		String name= suggestedName;
		for(String prefix : CUT_PREFIX) {
			if(prefix.length() >= suggestedName.length()) {
				continue;
			}
			char afterPrefix= suggestedName.charAt(prefix.length());
			if(Character.isUpperCase(afterPrefix) || afterPrefix == '_') {
				if(suggestedName.toLowerCase().startsWith(prefix)) {
					String nameWithoutPrefix= suggestedName.substring(prefix.length());
					if(nameWithoutPrefix.startsWith("_") && nameWithoutPrefix.length() > 1) { //$NON-NLS-1$
						name= nameWithoutPrefix.substring(1);
					} else {
						name= nameWithoutPrefix;
					}
					if(name.length() == 1) {
						return name;
					}
					break;
				}
			}
		}
		for(Map.Entry<String, String> entry : IRREG_NOUNS.entrySet()) {
			String suffix = entry.getKey();
			if(name.toLowerCase().endsWith(suffix.toLowerCase())) {
				String firstPart= name.substring(0, name.length() - suffix.length());
				return firstPart + entry.getValue();
			}
		}
		for(String varname : NO_BASE_TYPES) {
			if(name.equalsIgnoreCase(varname)) {
				return FOR_LOOP_ELEMENT_IDENTIFIER;
			}
		}
		for(String suffix : IRREG_ENDINGS) {
			if(name.toLowerCase().endsWith(suffix)) {
				return FOR_LOOP_ELEMENT_IDENTIFIER;
			}
		}
		if(name.length() > 2 && name.endsWith("s")) { //$NON-NLS-1$
			return name.substring(0, name.length() - 1);
		}
		return FOR_LOOP_ELEMENT_IDENTIFIER;
	}

}