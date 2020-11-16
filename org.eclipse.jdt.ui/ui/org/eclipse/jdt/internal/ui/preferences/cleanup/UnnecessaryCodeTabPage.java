/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Fabrice TIERCELIN - AutoBoxing usage
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.fix.ArraysFillCleanUp;
import org.eclipse.jdt.internal.ui.fix.AutoboxingCleanUp;
import org.eclipse.jdt.internal.ui.fix.CollectionCloningCleanUp;
import org.eclipse.jdt.internal.ui.fix.DoubleNegationCleanUp;
import org.eclipse.jdt.internal.ui.fix.EmbeddedIfCleanUp;
import org.eclipse.jdt.internal.ui.fix.EvaluateNullableCleanUp;
import org.eclipse.jdt.internal.ui.fix.HashCleanUp;
import org.eclipse.jdt.internal.ui.fix.JoinCleanUp;
import org.eclipse.jdt.internal.ui.fix.MapCloningCleanUp;
import org.eclipse.jdt.internal.ui.fix.MapMethodCleanUp;
import org.eclipse.jdt.internal.ui.fix.ObjectsEqualsCleanUp;
import org.eclipse.jdt.internal.ui.fix.OverriddenAssignmentCleanUp;
import org.eclipse.jdt.internal.ui.fix.PatternMatchingForInstanceofCleanUp;
import org.eclipse.jdt.internal.ui.fix.PushDownNegationCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantComparisonStatementCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantModifiersCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantSemicolonsCleanUp;
import org.eclipse.jdt.internal.ui.fix.RedundantSuperCallCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.TryWithResourceCleanUp;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnboxingCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryArrayCreationCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UselessContinueCleanUp;
import org.eclipse.jdt.internal.ui.fix.UselessReturnCleanUp;

public final class UnnecessaryCodeTabPage extends AbstractCleanUpTabPage {
	public static final String ID= "org.eclipse.jdt.ui.cleanup.tabpage.unnecessary_code"; //$NON-NLS-1$

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] {
				new UnusedCodeCleanUp(values),
				new UnnecessaryCodeCleanUp(values),
				new StringCleanUp(values),
				new TypeParametersCleanUp(values),
				new HashCleanUp(values),
				new JoinCleanUp(values),
				new ArraysFillCleanUp(values),
				new EvaluateNullableCleanUp(values),
				new AutoboxingCleanUp(values),
				new UnboxingCleanUp(values),
				new PushDownNegationCleanUp(values),
				new DoubleNegationCleanUp(values),
				new RedundantComparisonStatementCleanUp(values),
				new RedundantSuperCallCleanUp(values),
				new MapMethodCleanUp(values),
				new CollectionCloningCleanUp(values),
				new MapCloningCleanUp(values),
				new OverriddenAssignmentCleanUp(values),
				new RedundantModifiersCleanUp(values),
				new EmbeddedIfCleanUp(values),
				new PatternMatchingForInstanceofCleanUp(values),
				new RedundantSemicolonsCleanUp(values),
				new UnnecessaryArrayCreationCleanUp(values),
				new UselessReturnCleanUp(values),
				new UselessContinueCleanUp(values),
				new ObjectsEqualsCleanUp(values),
				new TryWithResourceCleanUp(values)
		};
	}

    @Override
	protected void doCreatePreferences(Composite composite, int numColumns) {
    	Group unusedCodeGroup= createGroup(5, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnusedCode);

    	CheckboxPreference removeImports= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedImports, CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(removeImports);

    	final CheckboxPreference unusedMembersPref= createCheckboxPref(unusedCodeGroup, 5, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMembers, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpModifyDialog.FALSE_TRUE);
		intent(unusedCodeGroup);
		final CheckboxPreference typesPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedTypes, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference constructorPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedConstructors, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference fieldsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedFields, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpModifyDialog.FALSE_TRUE);
		final CheckboxPreference methodsPref= createCheckboxPref(unusedCodeGroup, 1, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedMethods, CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpModifyDialog.FALSE_TRUE);
		registerSlavePreference(unusedMembersPref, new CheckboxPreference[] {typesPref, constructorPref, fieldsPref, methodsPref});

    	CheckboxPreference removeLocals= createCheckboxPref(unusedCodeGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnusedLocalVariables, CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(removeLocals);

    	Group unnecessaryGroup= createGroup(numColumns, composite, CleanUpMessages.UnnecessaryCodeTabPage_GroupName_UnnecessaryCode);

    	CheckboxPreference casts= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryCasts, CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(casts);

    	CheckboxPreference nls= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryNLSTags, CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpModifyDialog.FALSE_TRUE);
    	registerPreference(nls);

		CheckboxPreference typeArgs= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantTypeArguments, CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(typeArgs);

		CheckboxPreference hash= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_Hash, CleanUpConstants.MODERNIZE_HASH, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(hash);

		CheckboxPreference join= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_Join, CleanUpConstants.JOIN, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(join);

		CheckboxPreference arraysFill= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_ArraysFill, CleanUpConstants.ARRAYS_FILL, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(arraysFill);

		CheckboxPreference evaluateNullable= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_EvaluateNullable, CleanUpConstants.EVALUATE_NULLABLE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(evaluateNullable);

		CheckboxPreference autoboxing= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_Autoboxing, CleanUpConstants.USE_AUTOBOXING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(autoboxing);

		CheckboxPreference unboxing= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_Unboxing, CleanUpConstants.USE_UNBOXING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(unboxing);

		CheckboxPreference pushDownNegation= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_PushDownNegation, CleanUpConstants.PUSH_DOWN_NEGATION,
				CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(pushDownNegation);

		CheckboxPreference doubleNegation= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_DoubleNegation, CleanUpConstants.DOUBLE_NEGATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(doubleNegation);

		CheckboxPreference comparisonStatement= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_ComparisonStatement, CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(comparisonStatement);

		CheckboxPreference redundantSuperCall= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantSuperCall, CleanUpConstants.REDUNDANT_SUPER_CALL, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(redundantSuperCall);

		CheckboxPreference mapMethod= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UseDirectlyMapMethod,
				CleanUpConstants.USE_DIRECTLY_MAP_METHOD, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(mapMethod);

		CheckboxPreference collectionCloning= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_CollectionCloning, CleanUpConstants.COLLECTION_CLONING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(collectionCloning);

		CheckboxPreference mapCloning= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_MapCloning, CleanUpConstants.MAP_CLONING, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(mapCloning);

		CheckboxPreference overriddenAssignment= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_OverriddenAssignment, CleanUpConstants.OVERRIDDEN_ASSIGNMENT, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(overriddenAssignment);

		CheckboxPreference modifiers= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantModifiers, CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(modifiers);

		CheckboxPreference embeddedIf= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_EmbeddedIf, CleanUpConstants.RAISE_EMBEDDED_IF, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(embeddedIf);

		// Disabled as long as it is buggy
		//CheckboxPreference patternMatchingForInstanceof= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_PatternMatchingForInstanceof, CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF, CleanUpModifyDialog.FALSE_TRUE);
		//registerPreference(patternMatchingForInstanceof);

		CheckboxPreference semicolons= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_RedundantSemicolons, CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(semicolons);

		CheckboxPreference arrayCreation= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UnnecessaryVarargsArrayCreation, CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(arrayCreation);

		CheckboxPreference uselessReturn= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UselessReturn, CleanUpConstants.REMOVE_USELESS_RETURN, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(uselessReturn);

		CheckboxPreference uselessContinue= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_UselessContinue, CleanUpConstants.REMOVE_USELESS_CONTINUE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(uselessContinue);

		CheckboxPreference objectsEquals= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.UnnecessaryCodeTabPage_CheckboxName_ObjectsEquals, CleanUpConstants.USE_OBJECTS_EQUALS, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(objectsEquals);

		CheckboxPreference tryWithResource= createCheckboxPref(unnecessaryGroup, numColumns, CleanUpMessages.JavaFeatureTabPage_CheckboxName_TryWithResource, CleanUpConstants.TRY_WITH_RESOURCE, CleanUpModifyDialog.FALSE_TRUE);
		registerPreference(tryWithResource);
    }
}
