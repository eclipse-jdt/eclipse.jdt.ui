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
package org.eclipse.jdt.ui.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.eclipse.jdt.ui.tests.core.ASTNodesInsertTest;
import org.eclipse.jdt.ui.tests.core.ASTProviderTest;
import org.eclipse.jdt.ui.tests.core.AddImportTest;
import org.eclipse.jdt.ui.tests.core.BindingLabels18Test;
import org.eclipse.jdt.ui.tests.core.BindingLabelsTest;
import org.eclipse.jdt.ui.tests.core.BindingsNameTest;
import org.eclipse.jdt.ui.tests.core.CallHierarchyTest;
import org.eclipse.jdt.ui.tests.core.ClassPathDetectorTest;
import org.eclipse.jdt.ui.tests.core.CodeFormatterMigrationTest;
import org.eclipse.jdt.ui.tests.core.CodeFormatterTest;
import org.eclipse.jdt.ui.tests.core.CodeFormatterTest9;
import org.eclipse.jdt.ui.tests.core.CodeFormatterUtilTest;
import org.eclipse.jdt.ui.tests.core.HierarchicalASTVisitorTest;
import org.eclipse.jdt.ui.tests.core.ImportOrganizeTest;
import org.eclipse.jdt.ui.tests.core.ImportOrganizeTest18;
import org.eclipse.jdt.ui.tests.core.IndentManipulationTest;
import org.eclipse.jdt.ui.tests.core.JDTFlagsTest18;
import org.eclipse.jdt.ui.tests.core.JavaElementLabelsTest;
import org.eclipse.jdt.ui.tests.core.JavaElementLabelsTest18;
import org.eclipse.jdt.ui.tests.core.JavaElementPropertyTesterTest;
import org.eclipse.jdt.ui.tests.core.JavaModelUtilTest;
import org.eclipse.jdt.ui.tests.core.MethodOverrideTest;
import org.eclipse.jdt.ui.tests.core.MethodOverrideTest18;
import org.eclipse.jdt.ui.tests.core.NameProposerTest;
import org.eclipse.jdt.ui.tests.core.OverrideTest;
import org.eclipse.jdt.ui.tests.core.PartialASTTest;
import org.eclipse.jdt.ui.tests.core.ScopeAnalyzerTest;
import org.eclipse.jdt.ui.tests.core.SelectionHistoryTest;
import org.eclipse.jdt.ui.tests.core.StringsTest;
import org.eclipse.jdt.ui.tests.core.TemplateStoreTest;
import org.eclipse.jdt.ui.tests.core.TypeHierarchyTest;
import org.eclipse.jdt.ui.tests.core.TypeInfoTest;
import org.eclipse.jdt.ui.tests.core.TypeRulesTest;
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
ImportOrganizeTest18.class,
JavaElementLabelsTest.class,
JavaElementLabelsTest18.class,
BindingLabelsTest.class,
BindingLabels18Test.class,
JavaElementPropertyTesterTest.class,
JavaModelUtilTest.class,
MethodOverrideTest.class,
MethodOverrideTest18.class,
NameProposerTest.class,
OverrideTest.class,
PartialASTTest.class,
ScopeAnalyzerTest.class,
TemplateStoreTest.class,
TypeHierarchyTest.class,
TypeRulesTest.class,
TypeInfoTest.class,
StringsTest.class,
IndentManipulationTest.class,
SelectionHistoryTest.class,
ASTProviderTest.class,
JDTFlagsTest18.class,
})
public class CoreTestsSuite {
}