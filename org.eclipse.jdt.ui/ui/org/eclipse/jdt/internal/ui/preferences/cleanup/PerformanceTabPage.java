/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.BooleanLiteralCleanUp;
import org.eclipse.jdt.internal.ui.fix.BreakLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.LazyLogicalCleanUp;
import org.eclipse.jdt.internal.ui.fix.NoStringCreationCleanUp;
import org.eclipse.jdt.internal.ui.fix.PatternCleanUp;
import org.eclipse.jdt.internal.ui.fix.PlainReplacementCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveComparisonCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveParsingCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveRatherThanWrapperCleanUp;
import org.eclipse.jdt.internal.ui.fix.PrimitiveSerializationCleanUp;
import org.eclipse.jdt.internal.ui.fix.SingleUsedFieldCleanUp;
import org.eclipse.jdt.internal.ui.fix.StaticInnerClassCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringBufferToStringBuilderCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringBuilderCleanUp;
import org.eclipse.jdt.internal.ui.fix.UseStringIsBlankCleanUp;
import org.eclipse.jdt.internal.ui.fix.ValueOfRatherThanInstantiationCleanUp;

public final class PerformanceTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.performance"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new SingleUsedFieldCleanUp(values),
				new BreakLoopCleanUp(values),
				new StaticInnerClassCleanUp(values),
				new StringBuilderCleanUp(values),
				new PlainReplacementCleanUp(values),
				new UseStringIsBlankCleanUp(values),
				new LazyLogicalCleanUp(values),
				new ValueOfRatherThanInstantiationCleanUp(values),
				new PrimitiveComparisonCleanUp(values),
				new PrimitiveParsingCleanUp(values),
				new PrimitiveSerializationCleanUp(values),
				new PrimitiveRatherThanWrapperCleanUp(values),
				new PatternCleanUp(values),
				new StringBufferToStringBuilderCleanUp(values),
				new NoStringCreationCleanUp(values),
				new BooleanLiteralCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
		Group performanceGroup= createGroup(numColumns, composite, CleanUpMessages.PerformanceTabPage_GroupName_Performance);

		final CheckboxPreference singleUsedFieldPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_SingleUsedField, CleanUpConstants.SINGLE_USED_FIELD, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(singleUsedFieldPref);

		final CheckboxPreference breakLoopPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_BreakLoop, CleanUpConstants.BREAK_LOOP, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(breakLoopPref);

		final CheckboxPreference staticInnerClassPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_StaticInnerClass, CleanUpConstants.STATIC_INNER_CLASS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(staticInnerClassPref);

		final CheckboxPreference stringBuilderPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_StringBuilder, CleanUpConstants.STRINGBUILDER, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(stringBuilderPref);

		final CheckboxPreference plainReplacementPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PlainReplacement, CleanUpConstants.PLAIN_REPLACEMENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(plainReplacementPref);

		final CheckboxPreference useStringIsBlankPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_UseStringIsBlank, CleanUpConstants.USE_STRING_IS_BLANK,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(useStringIsBlankPref);

		final CheckboxPreference useLazyLogicalPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_UseLazyLogicalOperator,
				CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(useLazyLogicalPref);

		final CheckboxPreference valueOfRatherThanInstantiationPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_ValueOfRatherThanInstantiation, CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(valueOfRatherThanInstantiationPref);

		final CheckboxPreference primitiveComparisonPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PrimitiveComparison, CleanUpConstants.PRIMITIVE_COMPARISON, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(primitiveComparisonPref);

		final CheckboxPreference primitiveParsingPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PrimitiveParsing, CleanUpConstants.PRIMITIVE_PARSING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(primitiveParsingPref);

		final CheckboxPreference primitiveSerializationPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PrimitiveSerialization, CleanUpConstants.PRIMITIVE_SERIALIZATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(primitiveSerializationPref);

		final CheckboxPreference primitiveRatherThanWrapperPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PrimitiveRatherThanWrapper, CleanUpConstants.PRIMITIVE_RATHER_THAN_WRAPPER, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(primitiveRatherThanWrapperPref);

		final CheckboxPreference precompileRegExPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_PrecompileRegEx, CleanUpConstants.PRECOMPILE_REGEX,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(precompileRegExPref);

		final CheckboxPreference stringBufferToStringBuilderPref= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_StringBufferToStringBuilder, CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(stringBufferToStringBuilderPref);
		intent(performanceGroup);
		CheckboxPreference onlyLocalsPref= createCheckboxPref(performanceGroup, 1, CleanUpMessages.PerformanceTabPage_CheckboxName_StringBufferToStringBuilderLocalsOnly, CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(stringBufferToStringBuilderPref, new CheckboxPreference[] {onlyLocalsPref});

		final CheckboxPreference noStringCreation= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_NoStringCreation, CleanUpConstants.NO_STRING_CREATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(noStringCreation);

		final CheckboxPreference booleanLiteral= createCheckboxPref(performanceGroup, numColumns, CleanUpMessages.PerformanceTabPage_CheckboxName_BooleanLiteral, CleanUpConstants.PREFER_BOOLEAN_LITERAL, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(booleanLiteral);
	}
}
