/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_RUNTIME_VERSION;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_SPECIFICATION_VERSION;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.FixMessages.ConstantsCleanUpFix_refactor;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.BOOLEAN_PROPERTY;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.FILE_ENCODING;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.FILE_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.INTEGER_PROPERTY;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.JAVA_RUNTIME_VERSION;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.JAVA_SPEC_VERSION;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.LINE_SEPARATOR;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.LONG_PROPERTY;
import static org.eclipse.jdt.internal.corext.fix.UpdateProperty.PATH_SEPARATOR;
import static org.eclipse.jdt.internal.ui.fix.MultiFixMessages.ConstantsCleanUp_description;

import java.util.Collections;
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

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UpdateProperty;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ConstantsForSystemPropertiesCleanUpCore extends AbstractCleanUp {
	public ConstantsForSystemPropertiesCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public ConstantsForSystemPropertiesCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY) && !computeFixSet().isEmpty();
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null) {
			return null;
		}

		EnumSet<UpdateProperty> computeFixSet= computeFixSet();
		if(!isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY) || computeFixSet.isEmpty()) {
			return null;
		}

		Set<CompilationUnitRewriteOperation> operations= new LinkedHashSet<>();
		Set<ASTNode> nodesprocessed= new HashSet<>();
		computeFixSet.forEach(i->i.findOperations(compilationUnit,operations,nodesprocessed));

		if (operations.isEmpty()) {
			return null;
		}
		return new CompilationUnitRewriteOperationsFixCore(ConstantsCleanUpFix_refactor,
				compilationUnit, operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]));
	}

	private EnumSet<UpdateProperty> computeFixSet() {
		EnumSet<UpdateProperty> fixSet= EnumSet.noneOf(UpdateProperty.class);

		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR)) {
			fixSet.add(FILE_SEPARATOR);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR)) {
			fixSet.add(PATH_SEPARATOR);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING)) {
			fixSet.add(FILE_ENCODING);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR)) {
			fixSet.add(LINE_SEPARATOR);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_BOXED)) {
			fixSet.add(BOOLEAN_PROPERTY);
			fixSet.add(INTEGER_PROPERTY);
			fixSet.add(LONG_PROPERTY);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_RUNTIME_VERSION)) {
			fixSet.add(JAVA_RUNTIME_VERSION);
		}
		if(isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY_JAVA_SPECIFICATION_VERSION)) {
			fixSet.add(JAVA_SPEC_VERSION);
		}
		return fixSet;
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result;

		if (isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY)) {
			result= computeFixSet().stream().map(e->(Messages.format(ConstantsCleanUp_description,e.toString()))).collect(Collectors.toList());
		} else {
			result= Collections.emptyList();
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		StringBuilder sb= new StringBuilder();
		boolean isEnabled= isEnabled(CONSTANTS_FOR_SYSTEM_PROPERTY);
		EnumSet<UpdateProperty> computeFixSet= computeFixSet();

		if (isEnabled && computeFixSet.contains(UpdateProperty.FILE_SEPARATOR)) {
			sb.append("String fs = FileSystems.getDefault().getSeparator(); /* on JVM 1.6 this will be File.separator; */ \n"); //$NON-NLS-1$
		} else {
			sb.append("String fs = System.getProperty(\"file.separator\");\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.PATH_SEPARATOR)) {
			sb.append("String ps = File.pathSeparator;\n"); //$NON-NLS-1$
		} else {
			sb.append("String ps = System.getProperty(\"path.separator\");\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.LINE_SEPARATOR)) {
			sb.append("String ls = System.lineSeparator();\n"); //$NON-NLS-1$
		} else {
			sb.append("String ls = System.getProperty(\"line.separator\");\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.FILE_ENCODING)) {
			sb.append("String fe = Charset.defaultCharset().displayName();\n"); //$NON-NLS-1$
		} else {
			sb.append("String fe = System.getProperty(\"file.encoding\");\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.BOOLEAN_PROPERTY)) {
			sb.append("Boolean b = Boolean.getBoolean(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Boolean b = Boolean.parseBoolean(System.getProperty(\"arbitrarykey\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.BOOLEAN_PROPERTY)) {
			sb.append("Boolean b2 = Boolean.getBoolean(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Boolean b2 = Boolean.parseBoolean(System.getProperty(\"arbitrarykey\", \"false\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.INTEGER_PROPERTY)) {
			sb.append("Integer i = Integer.getInteger(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Integer i = Integer.parseInt(System.getProperty(\"arbitrarykey\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.INTEGER_PROPERTY)) {
			sb.append("Integer i2 = Integer.getInteger(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Integer i2 = Integer.parseInt(System.getProperty(\"arbitrarykey\",\"0\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.INTEGER_PROPERTY)) {
			sb.append("Integer i3 = Integer.getInteger(\"arbitrarykey\", 15);\n"); //$NON-NLS-1$
		} else {
			sb.append("Integer i3 = Integer.parseInt(System.getProperty(\"arbitrarykey\",\"15\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.LONG_PROPERTY)) {
			sb.append("Long l = Long.getLong(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Long l = Long.parseLong(System.getProperty(\"arbitrarykey\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.LONG_PROPERTY)) {
			sb.append("Long l2 = Long.getLong(\"arbitrarykey\");\n"); //$NON-NLS-1$
		} else {
			sb.append("Long l2 = Long.parseLong(System.getProperty(\"arbitrarykey\" ,\"0\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.LONG_PROPERTY)) {
			sb.append("Long l3 = Long.getLong(\"arbitrarykey\", 15);\n"); //$NON-NLS-1$
		} else {
			sb.append("Long l3 = Long.parseLong(System.getProperty(\"arbitrarykey\" ,\"15\"));\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.JAVA_RUNTIME_VERSION)) {
			sb.append("String jv = Runtime.version().toString();\n"); //$NON-NLS-1$
		} else {
			sb.append("String jv = System.getProperty(\"java.runtime.version\");\n"); //$NON-NLS-1$
		}

		if (isEnabled && computeFixSet.contains(UpdateProperty.JAVA_SPEC_VERSION)) {
			sb.append("String jsv = Runtime.version().feature();\n"); //$NON-NLS-1$
		} else {
			sb.append("String jsv = System.getProperty(\"java.specification.version\");\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
