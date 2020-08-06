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
 *     IBM Corporation - this file is based upon CoreTests.java
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.eclipse.jdt.ui.tests.core.source.SourceActionTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
AddImportTest.class,
SourceActionTests.class,
ASTNodesInsertTest.class,
BindingsNameTest.class,
CallHierarchyTest.class,
ClassPathDetectorTest.class,
CodeFormatterUtilTest.class,
CodeFormatterTest.class,
CodeFormatterTest9.class,
CodeFormatterMigrationTest.class,
HierarchicalASTVisitorTest.class,
ImportOrganizeTest.class,
ImportOrganizeTest1d8.class,
JavaElementLabelsTest.class,
JavaElementLabelsTest1d8.class,
BindingLabelsTest.class,
BindingLabels18Test.class,
JavaElementPropertyTesterTest.class,
JavaModelUtilTest.class,
MethodOverrideTest.class,
MethodOverrideTest1d8.class,
NameProposerTest.class,
OverrideTest.class,
PartialASTTest.class,
ScopeAnalyzerTest.class,
TemplateStoreTest.class,
TypeHierarchyTest.class,
TypeHierarchyViewPartTest.class,
TypeRulesTest.class,
TypeInfoTest.class,
StringsTest.class,
IndentManipulationTest.class,
SelectionHistoryTest.class,
ASTProviderTest.class,
JDTFlagsTest18.class,
})
public class CoreTestSuite {
}