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

public class Anon1_1 {
    final static String v15 = "Anon1_1";
    final static String v14 = "Anon1_1";

    static {
        new Object() {
            final static String v15 = "Anon1_1$1";
            final static String v14 = "Anon1_1$1";
        };
    }

    Anon1_1() {
        new Object() {
            final static String v15 = "Anon1_1$4";
            final static String v14 = "Anon1_1$12";
        };
    }

    {
        new Object() {
            final static String v15 = "Anon1_1$2";
            final static String v14 = "Anon1_1$2";
        };
    }

    void instanceMethod() {
        new Object() {
            final static String v15 = "Anon1_1$5";
            final static String v14 = "Anon1_1$13";
        };
        new Object() {
            final static String v15 = "Anon1_1$6";
            final static String v14 = "Anon1_1$14";
        };
    }

    static void staticMethod() {
        new Object() {
            final static String v15 = "Anon1_1$7";
            final static String v14 = "Anon1_1$15";
        };
        new Object() {
            final static String v15 = "Anon1_1$8";
            final static String v14 = "Anon1_1$16";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon1_1$3";
            final static String v14 = "Anon1_1$3";
        };
    }

    static class A2 {
        final static String v15 = "Anon1_1$A2";
        final static String v14 = "Anon1_1$A2";

        static {
            new Object() {
                final static String v15 = "Anon1_1$A2$1";
                final static String v14 = "Anon1_1$4";
            };
        }

        A2(){
            new Object() {
                final static String v15 = "Anon1_1$A2$4";
                final static String v14 = "Anon1_1$7";
            };
        }

        static {
            new Object() {
                final static String v15 = "Anon1_1$A2$2";
                final static String v14 = "Anon1_1$5";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon1_1$A2$3";
                final static String v14 = "Anon1_1$6";
            };
        }

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon1_1$A2$5";
                final static String v14 = "Anon1_1$8";
            };
            new Object() {
                final static String v15 = "Anon1_1$A2$6";
                final static String v14 = "Anon1_1$9";
            };
        }

        static void staticMethod() {
            new Object() {
                final static String v15 = "Anon1_1$A2$7";
                final static String v14 = "Anon1_1$10";
            };
            new Object() {
                final static String v15 = "Anon1_1$A2$8";
                final static String v14 = "Anon1_1$11";
            };
        }

    }
}
