/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat Inc. and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 12.
 */
public class CleanUpTest12 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java12ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testSwitch() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static final int CONSTANT_1 = 0;\n" //
				+ "    public static final int CONSTANT_2 = 1;\n" //
				+ "\n" //
				+ "    public int i2 = 0;\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnParameter(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (2 == i1) {\n" //
				+ "            i = 20;\n" //
				+ "        } else if (i1 == 3) {\n" //
				+ "            i = 25;\n" //
				+ "            i = 30;\n" //
				+ "        } else if (i1 == 4)\n" //
				+ "            i = 40;\n" //
				+ "        else if ((i1 == 5) || (i1 == 6)) {\n" //
				+ "            i = 60;\n" //
				+ "        } else if ((i1 == 7) ^ (i1 == 8)) {\n" //
				+ "            i = 80;\n" //
				+ "        } else if ((i1 == 9) | (i1 == 10)) {\n" //
				+ "            i = 100;\n" //
				+ "        } else if ((i1 == 11) || i1 == 12 || (i1 == 13)) {\n" //
				+ "            i = 130;\n" //
				+ "        } else if (14 == i1) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                i = 140;\n" //
				+ "            }\n" //
				+ "        } else if (i2 == 2) {\n" //
				+ "            i = 150;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchUsingConstants(int date) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (date == CONSTANT_1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (CONSTANT_2 == date) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (date == 3) {\n" //
				+ "            i = 60;\n" //
				+ "        } else if (date == 4) {\n" //
				+ "            i = 80;\n" //
				+ "        } else {\n" //
				+ "            i = 150;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnLocalVariable() {\n" //
				+ "        int i1 = 0;\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (2 == i1) {\n" //
				+ "            i = 20;\n" //
				+ "        } else if (i1 == 3) {\n" //
				+ "            i = 25;\n" //
				+ "            i = 30;\n" //
				+ "        } else if (i1 == 5) {\n" //
				+ "            // Do nothing\n" //
				+ "        } else if (i1 == 4)\n" //
				+ "            i = 40;\n" //
				+ "        else {\n" //
				+ "            i = 50;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnField() {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i2 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (i2 == 1) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (i2 == 2) {\n" //
				+ "            i = 20;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWithSwitchOnField() {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (this.i2 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (this.i2 == 1) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (this.i2 == 2) {\n" //
				+ "            i = 20;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnCharacter(char character) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        if (character == 'a') {\n" //
				+ "            // Keep this comment too\n" //
				+ "            i = 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (character == 'b')\n" //
				+ "            i = 10;\n" //
				+ "        else if ('c' == character) {\n" //
				+ "            i = 20;\n" //
				+ "        } else if (character == 'd') {\n" //
				+ "            i = 30;\n" //
				+ "        } else\n" //
				+ "            i = 40;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfRemoveDuplicateConditions(char aCharacter) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (aCharacter == 'a') {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (aCharacter == 'b') {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (aCharacter == 'a') {\n" //
				+ "            i = 20;\n" //
				+ "        } else if (aCharacter == 'b') {\n" //
				+ "            i = 30;\n" //
				+ "        } else if ('c' == aCharacter) {\n" //
				+ "            i = 40;\n" //
				+ "        } else if (aCharacter == 'd' || aCharacter == 'b' || ('c' == aCharacter)) {\n" //
				+ "            i = 50;\n" //
				+ "        } else {\n" //
				+ "            i = 60;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSeveralConditions(char myCharacter) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (myCharacter == 'a') {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (myCharacter == 'z') {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (myCharacter == 'a') {\n" //
				+ "            i = 20;\n" //
				+ "        } else if ((myCharacter == 'd') || (myCharacter == 'b') || ('c' == myCharacter) || ('f' == myCharacter)) {\n" //
				+ "            i = 50;\n" //
				+ "        } else {\n" //
				+ "            i = 60;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfKeepExistingControlFlowBreaks(byte i1) {\n" //
				+ "        byte j = 0;\n" //
				+ "        loop: for (byte i = 0; i < 10; i++) {\n" //
				+ "            if (i1 == 0) {\n" //
				+ "                j = 0;\n" //
				+ "                return;\n" //
				+ "            } else if (i1 == 1) {\n" //
				+ "                j = 10;\n" //
				+ "                continue;\n" //
				+ "            } else if (2 == i1) {\n" //
				+ "                j = 20;\n" //
				+ "                break loop;\n" //
				+ "            } else if (i1 == 3) {\n" //
				+ "                j = 25;\n" //
				+ "                j = 30;\n" //
				+ "            } else if (4 == i1) {\n" //
				+ "                j = 40;\n" //
				+ "                throw new RuntimeException();\n" //
				+ "            } else if (5 == i1) {\n" //
				+ "                j = 50;\n" //
				+ "                if (i == 5) {\n" //
				+ "                    throw new RuntimeException();\n" //
				+ "                } else {\n" //
				+ "                    return;\n" //
				+ "                }\n" //
				+ "            } else if (6 == i1) {\n" //
				+ "                j = 60;\n" //
				+ "                if (i == 5) {\n" //
				+ "                    throw new RuntimeException();\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWithInnerLoopBreak(short i1) {\n" //
				+ "        short j = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            j = 0;\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            j = 10;\n" //
				+ "            short k = 0;\n" //
				+ "            do {\n" //
				+ "                if (j == i1) {\n" //
				+ "                    break;\n" //
				+ "                }\n" //
				+ "                k++;\n" //
				+ "            } while (k < j);\n" //
				+ "        } else if (2 == i1) {\n" //
				+ "            j = 20;\n" //
				+ "            for (short l = 0; l < j; l++) {\n" //
				+ "                if (j == i1) {\n" //
				+ "                    break;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        } else if (i1 == 3) {\n" //
				+ "            j = 25;\n" //
				+ "            j = 30;\n" //
				+ "            short m = 0;\n" //
				+ "            while (m < j) {\n" //
				+ "                if (j == i1) {\n" //
				+ "                    break;\n" //
				+ "                }\n" //
				+ "                m++;\n" //
				+ "            }\n" //
				+ "        } else if (4 == i1) {\n" //
				+ "            j = 40;\n" //
				+ "            for (short o : new short[] { 1, 2, 3 }) {\n" //
				+ "                if (o == i1) {\n" //
				+ "                    break;\n" //
				+ "                }\n" //
				+ "            }\n" //
				+ "        } else if (5 == i1) {\n" //
				+ "            j = 50;\n" //
				+ "            switch (j) {\n" //
				+ "            case 0 :\n" //
				+ "                j = 0;\n" //
				+ "                break;\n" //
				+ "            case 1 :\n" //
				+ "                j = 10;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWhenNoVariableNameConflictExists(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            int newVariable1 = 0;\n" //
				+ "            i = newVariable1;\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            int newVariable2 = 10;\n" //
				+ "            i = newVariable2;\n" //
				+ "        } else if (2 == i1) {\n" //
				+ "            char newVariable3 = 'a';\n" //
				+ "            i = newVariable3;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWhenOutOfScopeVariableNameConflicts(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            for (int l = 0; l < i; l++) {\n" //
				+ "                int integer1 = 0;\n" //
				+ "                i = integer1;\n" //
				+ "            }\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            int integer1 = 10;\n" //
				+ "            i = integer1;\n" //
				+ "        } else if (i1 == 2) {\n" //
				+ "            int i2 = 20;\n" //
				+ "            i = i2;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceIfSuite(int i1) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            return 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        }\n" //
				+ "        if (i1 == 1) {\n" //
				+ "            return 10;\n" //
				+ "        }\n" //
				+ "        if (2 == i1) {\n" //
				+ "            return 20;\n" //
				+ "        }\n" //
				+ "        if (i1 == 3) {\n" //
				+ "            return 30;\n" //
				+ "        }\n" //
				+ "        if (i1 == 4)\n" //
				+ "            return 40;\n" //
				+ "        if ((i1 == 5) || (i1 == 6)) {\n" //
				+ "            return 60;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 7) ^ (i1 == 8)) {\n" //
				+ "            return 80;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 9) | (i1 == 10)) {\n" //
				+ "            return 100;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {\n" //
				+ "            return 130;\n" //
				+ "        }\n" //
				+ "        if (14 == i1) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 140;\n" //
				+ "            }\n" //
				+ "            return 145;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceSuiteThatDoNotFallThrough(int i1) {\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            return 10;\n" //
				+ "            // Keep this comment also\n" //
				+ "        }\n" //
				+ "        if (2 == i1) {\n" //
				+ "            return 20;\n" //
				+ "        }\n" //
				+ "        if (i1 == 3) {\n" //
				+ "            return 30;\n" //
				+ "        }\n" //
				+ "        if (i1 == 4)\n" //
				+ "            return 40;\n" //
				+ "        if ((i1 == 5) || (i1 == 6)) {\n" //
				+ "            return 60;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 7) ^ (i1 == 8)) {\n" //
				+ "            return 80;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 9) | (i1 == 10)) {\n" //
				+ "            return 100;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {\n" //
				+ "            return 130;\n" //
				+ "        }\n" //
				+ "        if (14 == i1) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 140;\n" //
				+ "            }\n" //
				+ "            return 145;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceSuiteIgnoring(int i1) {\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            return 0;\n" //
				+ "        } else if (i2 == 1) {\n" //
				+ "            return 140;\n" //
				+ "        }\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 1) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            return 10;\n" //
				+ "            // Keep this comment also\n" //
				+ "        }\n" //
				+ "        if (2 == i1) {\n" //
				+ "            return 20;\n" //
				+ "        }\n" //
				+ "        if (i1 == 3) {\n" //
				+ "            return 30;\n" //
				+ "        }\n" //
				+ "        if (i1 == 4)\n" //
				+ "            return 40;\n" //
				+ "        if ((i1 == 5) || (i1 == 6)) {\n" //
				+ "            return 60;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 7) ^ (i1 == 8)) {\n" //
				+ "            return 80;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 9) | (i1 == 10)) {\n" //
				+ "            return 100;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {\n" //
				+ "            return 130;\n" //
				+ "        }\n" //
				+ "        if (14 == i1) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 140;\n" //
				+ "            }\n" //
				+ "            return 145;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWhenVariableTypesConflict(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            int integer1 = 0;\n" //
				+ "            i = integer1;\n" //
				+ "        } else if (i1 == 2) {\n" //
				+ "            char integer1 = 'a';\n" //
				+ "            i = integer1;\n" //
				+ "        } else if (i1 == 3) {\n" //
				+ "            char c = 'a';\n" //
				+ "            i = c;\n" //
				+ "        } else {\n" //
				+ "            char c = 'b';\n" //
				+ "            i = c;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceMeltCases(int i1) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            // Keep this comment too\n" //
				+ "            return 0;\n" //
				+ "            // Keep this comment also\n" //
				+ "        } else if (i1 == 1) {\n" //
				+ "            return 10;\n" //
				+ "        } else if (2 == i1) {\n" //
				+ "            return 20;\n" //
				+ "        } else if (i1 == 3) {\n" //
				+ "            return 30;\n" //
				+ "        }\n" //
				+ "        if (i1 == 4)\n" //
				+ "            return 40;\n" //
				+ "        if ((i1 == 5) || (i1 == 6)) {\n" //
				+ "            return 60;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 7) ^ (i1 == 8)) {\n" //
				+ "            return 80;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 9) | (i1 == 10)) {\n" //
				+ "            return 100;\n" //
				+ "        }\n" //
				+ "        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {\n" //
				+ "            return 130;\n" //
				+ "        }\n" //
				+ "        if (14 == i1) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 140;\n" //
				+ "            }\n" //
				+ "            return 145;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.USE_SWITCH);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public static final int CONSTANT_1 = 0;\n" //
				+ "    public static final int CONSTANT_2 = 1;\n" //
				+ "\n" //
				+ "    public int i2 = 0;\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnParameter(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case 1 :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 2 :\n" //
				+ "                i = 20;\n" //
				+ "                break;\n" //
				+ "            case 3 :\n" //
				+ "                i = 25;\n" //
				+ "                i = 30;\n" //
				+ "                break;\n" //
				+ "            case 4 :\n" //
				+ "                i = 40;\n" //
				+ "                break;\n" //
				+ "            case 5 :\n" //
				+ "            case 6 :\n" //
				+ "                i = 60;\n" //
				+ "                break;\n" //
				+ "            case 7 :\n" //
				+ "            case 8 :\n" //
				+ "                i = 80;\n" //
				+ "                break;\n" //
				+ "            case 9 :\n" //
				+ "            case 10 :\n" //
				+ "                i = 100;\n" //
				+ "                break;\n" //
				+ "            case 11 :\n" //
				+ "            case 12 :\n" //
				+ "            case 13 :\n" //
				+ "                i = 130;\n" //
				+ "                break;\n" //
				+ "            case 14 :\n" //
				+ "                if (i2 == 1) {\n" //
				+ "                    i = 140;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                if (i2 == 2) {\n" //
				+ "                    i = 150;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchUsingConstants(int date) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (date) {\n" //
				+ "            case CONSTANT_1 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case CONSTANT_2 :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 3 :\n" //
				+ "                i = 60;\n" //
				+ "                break;\n" //
				+ "            case 4 :\n" //
				+ "                i = 80;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                i = 150;\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnLocalVariable() {\n" //
				+ "        int i1 = 0;\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case 1 :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 2 :\n" //
				+ "                i = 20;\n" //
				+ "                break;\n" //
				+ "            case 3 :\n" //
				+ "                i = 25;\n" //
				+ "                i = 30;\n" //
				+ "                break;\n" //
				+ "            case 5 :\n" //
				+ "                break;\n" //
				+ "            case 4 :\n" //
				+ "                i = 40;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                i = 50;\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnField() {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i2) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case 1 :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 2 :\n" //
				+ "                i = 20;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWithSwitchOnField() {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (this.i2) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case 1 :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 2 :\n" //
				+ "                i = 20;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSwitchOnCharacter(char character) {\n" //
				+ "        int i = 0;\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (character) {\n" //
				+ "            case 'a' :\n" //
				+ "                // Keep this comment too\n" //
				+ "                i = 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "                break;\n" //
				+ "            case 'b' :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 'c' :\n" //
				+ "                i = 20;\n" //
				+ "                break;\n" //
				+ "            case 'd' :\n" //
				+ "                i = 30;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                i = 40;\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfRemoveDuplicateConditions(char aCharacter) {\n" //
				+ "        int i = 0;\n" //
				+ "        switch (aCharacter) {\n" //
				+ "            case 'a' :\n" //
				+ "                i = 0;\n" //
				+ "                break;\n" //
				+ "            case 'b' :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 'c' :\n" //
				+ "                i = 40;\n" //
				+ "                break;\n" //
				+ "            case 'd' :\n" //
				+ "                i = 50;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                i = 60;\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWithSeveralConditions(char myCharacter) {\n" //
				+ "        int i = 0;\n" //
				+ "        switch (myCharacter) {\n" //
				+ "            case 'a' :\n" //
				+ "                i = 0;\n" //
				+ "                break;\n" //
				+ "            case 'z' :\n" //
				+ "                i = 10;\n" //
				+ "                break;\n" //
				+ "            case 'd' :\n" //
				+ "            case 'b' :\n" //
				+ "            case 'c' :\n" //
				+ "            case 'f' :\n" //
				+ "                i = 50;\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                i = 60;\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfKeepExistingControlFlowBreaks(byte i1) {\n" //
				+ "        byte j = 0;\n" //
				+ "        loop: for (byte i = 0; i < 10; i++) {\n" //
				+ "            switch (i1) {\n" //
				+ "                case 0 :\n" //
				+ "                    j = 0;\n" //
				+ "                    return;\n" //
				+ "                case 1 :\n" //
				+ "                    j = 10;\n" //
				+ "                    continue;\n" //
				+ "                case 2 :\n" //
				+ "                    j = 20;\n" //
				+ "                    break loop;\n" //
				+ "                case 3 :\n" //
				+ "                    j = 25;\n" //
				+ "                    j = 30;\n" //
				+ "                    break;\n" //
				+ "                case 4 :\n" //
				+ "                    j = 40;\n" //
				+ "                    throw new RuntimeException();\n" //
				+ "                case 5 :\n" //
				+ "                    j = 50;\n" //
				+ "                    if (i == 5) {\n" //
				+ "                        throw new RuntimeException();\n" //
				+ "                    } else {\n" //
				+ "                        return;\n" //
				+ "                    }\n" //
				+ "                case 6 :\n" //
				+ "                    j = 60;\n" //
				+ "                    if (i == 5) {\n" //
				+ "                        throw new RuntimeException();\n" //
				+ "                    }\n" //
				+ "                    break;\n" //
				+ "                default :\n" //
				+ "                    break;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWithInnerLoopBreak(short i1) {\n" //
				+ "        short j = 0;\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                j = 0;\n" //
				+ "                break;\n" //
				+ "            case 1 : {\n" //
				+ "                j = 10;\n" //
				+ "                short k = 0;\n" //
				+ "                do {\n" //
				+ "                    if (j == i1) {\n" //
				+ "                        break;\n" //
				+ "                    }\n" //
				+ "                    k++;\n" //
				+ "                } while (k < j);\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 2 :\n" //
				+ "                j = 20;\n" //
				+ "                for (short l = 0; l < j; l++) {\n" //
				+ "                    if (j == i1) {\n" //
				+ "                        break;\n" //
				+ "                    }\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            case 3 : {\n" //
				+ "                j = 25;\n" //
				+ "                j = 30;\n" //
				+ "                short m = 0;\n" //
				+ "                while (m < j) {\n" //
				+ "                    if (j == i1) {\n" //
				+ "                        break;\n" //
				+ "                    }\n" //
				+ "                    m++;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 4 :\n" //
				+ "                j = 40;\n" //
				+ "                for (short o : new short[] { 1, 2, 3 }) {\n" //
				+ "                    if (o == i1) {\n" //
				+ "                        break;\n" //
				+ "                    }\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            case 5 :\n" //
				+ "                j = 50;\n" //
				+ "                switch (j) {\n" //
				+ "                case 0 :\n" //
				+ "                    j = 0;\n" //
				+ "                    break;\n" //
				+ "                case 1 :\n" //
				+ "                    j = 10;\n" //
				+ "                    break;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceIfWhenNoVariableNameConflictExists(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 : {\n" //
				+ "                int newVariable1 = 0;\n" //
				+ "                i = newVariable1;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 1 : {\n" //
				+ "                int newVariable2 = 10;\n" //
				+ "                i = newVariable2;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 2 : {\n" //
				+ "                char newVariable3 = 'a';\n" //
				+ "                i = newVariable3;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWhenOutOfScopeVariableNameConflicts(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                for (int l = 0; l < i; l++) {\n" //
				+ "                    int integer1 = 0;\n" //
				+ "                    i = integer1;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            case 1 : {\n" //
				+ "                int integer1 = 10;\n" //
				+ "                i = integer1;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 2 : {\n" //
				+ "                int i2 = 20;\n" //
				+ "                i = i2;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceIfSuite(int i1) {\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                return 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "            case 1 :\n" //
				+ "                return 10;\n" //
				+ "            case 2 :\n" //
				+ "                return 20;\n" //
				+ "            case 3 :\n" //
				+ "                return 30;\n" //
				+ "            case 4 :\n" //
				+ "                return 40;\n" //
				+ "            case 5 :\n" //
				+ "            case 6 :\n" //
				+ "                return 60;\n" //
				+ "            case 7 :\n" //
				+ "            case 8 :\n" //
				+ "                return 80;\n" //
				+ "            case 9 :\n" //
				+ "            case 10 :\n" //
				+ "                return 100;\n" //
				+ "            case 11 :\n" //
				+ "            case 12 :\n" //
				+ "            case 13 :\n" //
				+ "                return 130;\n" //
				+ "            case 14 :\n" //
				+ "                if (i2 == 1) {\n" //
				+ "                    return 140;\n" //
				+ "                }\n" //
				+ "                return 145;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceSuiteThatDoNotFallThrough(int i1) {\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            if (i2 == 1) {\n" //
				+ "                return 0;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 1 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                return 10;\n" //
				+ "                // Keep this comment also\n" //
				+ "            case 2 :\n" //
				+ "                return 20;\n" //
				+ "            case 3 :\n" //
				+ "                return 30;\n" //
				+ "            case 4 :\n" //
				+ "                return 40;\n" //
				+ "            case 5 :\n" //
				+ "            case 6 :\n" //
				+ "                return 60;\n" //
				+ "            case 7 :\n" //
				+ "            case 8 :\n" //
				+ "                return 80;\n" //
				+ "            case 9 :\n" //
				+ "            case 10 :\n" //
				+ "                return 100;\n" //
				+ "            case 11 :\n" //
				+ "            case 12 :\n" //
				+ "            case 13 :\n" //
				+ "                return 130;\n" //
				+ "            case 14 :\n" //
				+ "                if (i2 == 1) {\n" //
				+ "                    return 140;\n" //
				+ "                }\n" //
				+ "                return 145;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceSuiteIgnoring(int i1) {\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            return 0;\n" //
				+ "        } else if (i2 == 1) {\n" //
				+ "            return 140;\n" //
				+ "        }\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 1 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                return 10;\n" //
				+ "                // Keep this comment also\n" //
				+ "            case 2 :\n" //
				+ "                return 20;\n" //
				+ "            case 3 :\n" //
				+ "                return 30;\n" //
				+ "            case 4 :\n" //
				+ "                return 40;\n" //
				+ "            case 5 :\n" //
				+ "            case 6 :\n" //
				+ "                return 60;\n" //
				+ "            case 7 :\n" //
				+ "            case 8 :\n" //
				+ "                return 80;\n" //
				+ "            case 9 :\n" //
				+ "            case 10 :\n" //
				+ "                return 100;\n" //
				+ "            case 11 :\n" //
				+ "            case 12 :\n" //
				+ "            case 13 :\n" //
				+ "                return 130;\n" //
				+ "            case 14 :\n" //
				+ "                if (i2 == 1) {\n" //
				+ "                    return 140;\n" //
				+ "                }\n" //
				+ "                return 145;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void replaceWhenVariableTypesConflict(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 : {\n" //
				+ "                int integer1 = 0;\n" //
				+ "                i = integer1;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 2 : {\n" //
				+ "                char integer1 = 'a';\n" //
				+ "                i = integer1;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            case 3 : {\n" //
				+ "                char c = 'a';\n" //
				+ "                i = c;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "            default : {\n" //
				+ "                char c = 'b';\n" //
				+ "                i = c;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int replaceMeltCases(int i1) {\n" //
				+ "        // Keep this comment\n" //
				+ "        switch (i1) {\n" //
				+ "            case 0 :\n" //
				+ "                // Keep this comment too\n" //
				+ "                return 0;\n" //
				+ "                // Keep this comment also\n" //
				+ "            case 1 :\n" //
				+ "                return 10;\n" //
				+ "            case 2 :\n" //
				+ "                return 20;\n" //
				+ "            case 3 :\n" //
				+ "                return 30;\n" //
				+ "            case 4 :\n" //
				+ "                return 40;\n" //
				+ "            case 5 :\n" //
				+ "            case 6 :\n" //
				+ "                return 60;\n" //
				+ "            case 7 :\n" //
				+ "            case 8 :\n" //
				+ "                return 80;\n" //
				+ "            case 9 :\n" //
				+ "            case 10 :\n" //
				+ "                return 100;\n" //
				+ "            case 11 :\n" //
				+ "            case 12 :\n" //
				+ "            case 13 :\n" //
				+ "                return 130;\n" //
				+ "            case 14 :\n" //
				+ "                if (i2 == 1) {\n" //
				+ "                    return 140;\n" //
				+ "                }\n" //
				+ "                return 145;\n" //
				+ "            default :\n" //
				+ "                break;\n" //
				+ "        }\n" //
				+ "        return 155;\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_Switch_description)));
	}

	@Test
	public void testDoNotUseSwitch() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void doNotReplaceWithOuterLoopBreak(int i1) {\n" //
				+ "        int j = 0;\n" //
				+ "        for (int i = 0; i < 10; i++) {\n" //
				+ "            if (i1 == 0) {\n" //
				+ "                j = 0;\n" //
				+ "            } else if (i1 == 1) {\n" //
				+ "                j = 10;\n" //
				+ "            } else if (2 == i1) {\n" //
				+ "                j = 20;\n" //
				+ "            } else if (i1 == 3) {\n" //
				+ "                j = 25;\n" //
				+ "                j = 30;\n" //
				+ "            } else if (4 == i1) {\n" //
				+ "                j = 40;\n" //
				+ "            } else if (5 == i1) {\n" //
				+ "                j = 50;\n" //
				+ "                break;\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceIfWithoutElseIf(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            i = 0;\n" //
				+ "        } else {\n" //
				+ "            i = 10;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceIfWithoutElse(int i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            i = 10;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotReplaceWithSwitchOnPrimitiveWrapper(Integer i1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (i1 == 0) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (i1 == 10) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (i1 == 20) {\n" //
				+ "            i = 20;\n" //
				+ "        } else {\n" //
				+ "            i = 30;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotRefactorLongVar(long l1) {\n" //
				+ "        int i = 0;\n" //
				+ "        if (l1 == 0) {\n" //
				+ "            i = 0;\n" //
				+ "        } else if (l1 == 1) {\n" //
				+ "            i = 10;\n" //
				+ "        } else if (l1 == 2) {\n" //
				+ "            i = 20;\n" //
				+ "        } else if (l1 == 3) {\n" //
				+ "            i = 30;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
