/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.SIMPLIFY_STATUS_CLEANUP;
import static org.eclipse.jdt.internal.ui.fix.MultiFixMessages.PlatformStatusCleanUpFix_refactor;
import static org.eclipse.jdt.internal.ui.fix.MultiFixMessages.PlatformStatusCleanUp_description;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.SimplifyPlatformStatusFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

public class SimplifyPlatformStatusCleanUpCore extends AbstractCleanUpCore {
	public SimplifyPlatformStatusCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public SimplifyPlatformStatusCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(SIMPLIFY_STATUS_CLEANUP);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null) {
			return null;
		}
		EnumSet<SimplifyPlatformStatusFixCore> computeFixSet= computeFixSet();
		if (!isEnabled(SIMPLIFY_STATUS_CLEANUP) || computeFixSet.isEmpty()
				|| !JavaModelUtil.is9OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return null;
		}
		Set<CompilationUnitRewriteOperation> operations= new LinkedHashSet<>();
		Set<ASTNode> nodesprocessed= new HashSet<>();
		for (var i : computeFixSet) {
			i.findOperations(compilationUnit, operations, nodesprocessed);
		}
		if (operations.isEmpty()) {
			return null;
		}
		return new CompilationUnitRewriteOperationsFixCore(PlatformStatusCleanUpFix_refactor, compilationUnit,
				operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(SIMPLIFY_STATUS_CLEANUP)) {
			String with= ""; //$NON-NLS-1$
			result.add(
					Messages.format(PlatformStatusCleanUp_description,
							new Object[] { String.join(",", computeFixSet().stream() //$NON-NLS-1$
									.map(SimplifyPlatformStatusFixCore::toString).collect(Collectors.toList())),
									with }));
		}
		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		StringBuilder sb= new StringBuilder();
		EnumSet<SimplifyPlatformStatusFixCore> computeFixSet= computeFixSet();
		EnumSet.allOf(SimplifyPlatformStatusFixCore.class)
		.forEach(e -> sb.append(e.getPreview(computeFixSet.contains(e))));
		return sb.toString();
	}

	private EnumSet<SimplifyPlatformStatusFixCore> computeFixSet() {
		EnumSet<SimplifyPlatformStatusFixCore> fixSet= EnumSet.noneOf(SimplifyPlatformStatusFixCore.class);

		if (isEnabled(SIMPLIFY_STATUS_CLEANUP)) {
			fixSet= EnumSet.allOf(SimplifyPlatformStatusFixCore.class);
		}
		return fixSet;
	}
}
