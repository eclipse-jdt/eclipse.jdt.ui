/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.AutoboxingCleanUp;
import org.eclipse.jdt.internal.ui.fix.ComparingOnCriteriaCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConstantsForSystemPropertyCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.HashCleanUp;
import org.eclipse.jdt.internal.ui.fix.JoinCleanUp;
import org.eclipse.jdt.internal.ui.fix.LambdaExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.MultiCatchCleanUp;
import org.eclipse.jdt.internal.ui.fix.ObjectsEqualsCleanUp;
import org.eclipse.jdt.internal.ui.fix.PatternMatchingForInstanceofCleanUp;
import org.eclipse.jdt.internal.ui.fix.SwitchExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.TryWithResourceCleanUp;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnboxingCleanUp;
import org.eclipse.jdt.internal.ui.fix.VarCleanUp;

public final class JavaFeatureTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.java_feature"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(final Map<String, String> values) {
		return new AbstractCleanUp[] {
				new PatternMatchingForInstanceofCleanUp(values),
				new SwitchExpressionsCleanUp(values),
				new VarCleanUp(values),
				new LambdaExpressionsCleanUp(values),
				new ComparingOnCriteriaCleanUp(values),
				new JoinCleanUp(values),
				new TryWithResourceCleanUp(values),
				new MultiCatchCleanUp(values),
				new TypeParametersCleanUp(values),
				new HashCleanUp(values),
				new ObjectsEqualsCleanUp(values),
				new ConvertLoopCleanUp(values),
				new AutoboxingCleanUp(values),
				new UnboxingCleanUp(values),
				new ConstantsForSystemPropertyCleanUp(values)
		};
	}

	@Override
	protected void doCreatePreferences(final Composite composite, final int numColumns) {
		Group java16Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java16);

		CheckboxPreference patternMatchingForInstanceof= createCheckboxPref(java16Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_PatternMatchingForInstanceof, CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(patternMatchingForInstanceof);

		Group java14Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java14);

		CheckboxPreference convertToSwitchExpressions= createCheckboxPref(java14Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConvertToSwitchExpressions, CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(convertToSwitchExpressions);

		Group java10Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java10);

		CheckboxPreference useVarPref= createCheckboxPref(java10Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_UseVar, CleanUpConstants.USE_VAR,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(useVarPref);

		Group java1d8Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java1d8);

		CheckboxPreference convertFunctionalInterfaces= createCheckboxPref(java1d8Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConvertFunctionalInterfaces, CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES, CleanUpModifyDialog.FALSE_TRUE);
		intent(java1d8Group);
		RadioPreference useLambdaPref= createRadioPref(java1d8Group, 1, CleanUpMessages.JavaFeatureTabPage_RadioName_UseLambdaWherePossible, CleanUpConstants.USE_LAMBDA, CleanUpModifyDialog.FALSE_TRUE);
		RadioPreference useAnonymousPref= createRadioPref(java1d8Group, 1, CleanUpMessages.JavaFeatureTabPage_RadioName_UseAnonymous, CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(convertFunctionalInterfaces, new RadioPreference[] { useLambdaPref, useAnonymousPref });

		CheckboxPreference comparingOnCriteria= createCheckboxPref(java1d8Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ComparingOnCriteria, CleanUpConstants.COMPARING_ON_CRITERIA, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(comparingOnCriteria);

		CheckboxPreference join= createCheckboxPref(java1d8Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_Join, CleanUpConstants.JOIN, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(join);

		Group java1d7Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java1d7);

		CheckboxPreference tryWithResource= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_TryWithResource, CleanUpConstants.TRY_WITH_RESOURCE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(tryWithResource);

		CheckboxPreference multiCatch= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_MultiCatch, CleanUpConstants.MULTI_CATCH, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(multiCatch);

		CheckboxPreference typeArgs= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_RedundantTypeArguments, CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(typeArgs);

		CheckboxPreference hash= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_Hash, CleanUpConstants.MODERNIZE_HASH, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(hash);

		CheckboxPreference objectsEquals= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ObjectsEquals, CleanUpConstants.USE_OBJECTS_EQUALS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(objectsEquals);

		CheckboxPreference systemconstants= createCheckboxPref(java1d7Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(systemconstants);
		intent(java1d7Group);
		CheckboxPreference systemconstantsFileSeparator= createCheckboxPref(java1d7Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_FileSeparator, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR, CleanUpModifyDialog.FALSE_TRUE);
		CheckboxPreference systemconstantsPathSeparator= createCheckboxPref(java1d7Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_PathSeparator, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR, CleanUpModifyDialog.FALSE_TRUE);
		CheckboxPreference systemconstantsLineSeparator= createCheckboxPref(java1d7Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_LineSeparator, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR, CleanUpModifyDialog.FALSE_TRUE);
		intent(java1d7Group);
		CheckboxPreference systemconstantsFileEncoding= createCheckboxPref(java1d7Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_FileEncoding, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING, CleanUpModifyDialog.FALSE_TRUE);
		CheckboxPreference systemconstantsBooleanProperty= createCheckboxPref(java1d7Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConstantsForSystemProperty_BooleanProperty, CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOOLEAN, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(systemconstants, new CheckboxPreference[] {systemconstantsFileSeparator,systemconstantsPathSeparator,systemconstantsLineSeparator,systemconstantsFileEncoding,systemconstantsBooleanProperty});

		Group java1d5Group= createGroup(numColumns, composite, CleanUpMessages.JavaFeatureTabPage_GroupName_Java1d5);

		CheckboxPreference convertLoop= createCheckboxPref(java1d5Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConvertForLoopToEnhanced, CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(convertLoop);
		intent(java1d5Group);
		CheckboxPreference convertLoopOnlyIfLoopVariableUsed= createCheckboxPref(java1d5Group, 1, CleanUpMessages.JavaFeatureTabPage_CheckboxName_ConvertLoopOnlyIfLoopVarUsed, CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(convertLoop, new CheckboxPreference[] {convertLoopOnlyIfLoopVariableUsed});

		CheckboxPreference autoboxing= createCheckboxPref(java1d5Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_Autoboxing, CleanUpConstants.USE_AUTOBOXING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(autoboxing);

		CheckboxPreference unboxing= createCheckboxPref(java1d5Group, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_Unboxing, CleanUpConstants.USE_UNBOXING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(unboxing);
	}
}
