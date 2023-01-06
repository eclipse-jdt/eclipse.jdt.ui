/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package inner;

public class Anon2 {
    final static String v15 = "Anon2";
    final static String v14 = "Anon2";

    void instanceMethod() {
        new Object() {
            final static String v15 = "Anon2$3";
            final static String v14 = "Anon2$7";
        };
        new Object() {
            final static String v15 = "Anon2$4";
            final static String v14 = "Anon2$8";
        };
    }

    Anon2() {
        new Object() {
            final static String v15 = "Anon2$5";
            final static String v14 = "Anon2$9";
        };
        new Object() {
            final static String v15 = "Anon2$6";
            final static String v14 = "Anon2$10";
        };
    }

    static void staticMethod() {
        new Object() {
            final static String v15 = "Anon2$7";
            final static String v14 = "Anon2$11";
        };
        new Object() {
            final static String v15 = "Anon2$8";
            final static String v14 = "Anon2$12";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon2$1";
            final static String v14 = "Anon2$1";
        };

        new Object() {
            final static String v15 = "Anon2$2";
            final static String v14 = "Anon2$2";
        };
    }

    class A2 {
        final static String v15 = "Anon2$A2";
        final static String v14 = "Anon2$A2";

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon2$A2$3";
                final static String v14 = "Anon2$5";
            };
            new Object() {
                final static String v15 = "Anon2$A2$4";
                final static String v14 = "Anon2$6";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon2$A2$1";
                final static String v14 = "Anon2$3";
            };
            new Object() {
                final static String v15 = "Anon2$A2$2";
                final static String v14 = "Anon2$4";
            };
        }
    }
}
