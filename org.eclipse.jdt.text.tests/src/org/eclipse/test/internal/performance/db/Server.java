/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.db;

import java.sql.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server {

    public static void main(String[] args) throws Exception {

        try {
            System.out.println("Starting Network Server");
            System.setProperty("cloudscape.drda.startNetworkServer", "true");
            System.setProperty("cloudscape.system.home", "/tmp/cloudscape");

            Class.forName("com.ihost.cs.jdbc.CloudscapeDriver").newInstance();

            /*
            com.ihost.cs.drda.NetworkServerControl server= new com.ihost.cs.drda.NetworkServerControl();
            System.out.println("Testing if Network Server is up and running!");
            for (int i= 0; i < 10; i++) {
                try {
                    server.ping();
                    break;
                } catch (Exception e) {
                    System.out.println("Try #" + i + " " + e.toString());
                    if (i == 9) {
                        System.out.println("Giving up trying to connect to Network Server!");
                        throw e;
                    }
                 }
                Thread.sleep(5000);
            }
            System.out.println("Cloudscape Network Server now running");
            */
            
        } catch (Exception e) {
            System.out.println("Failed to start NetworkServer: " + e);
            System.exit(1);
        }

        Connection conn= null;
        try {
            String dbUrl= "jdbc:cloudscape:" + DB.DB_NAME + ";create=true;";
            conn= DriverManager.getConnection(dbUrl);
            System.out.println("Got an embedded connection.");
            
            System.out.println("Press [Enter] to stop Server");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            
        } catch (SQLException sqle) {
            System.out.println("Failure making connection: " + sqle);
            sqle.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
            try {
                 DriverManager.getConnection("jdbc:cloudscape:;shutdown=true");
                 System.out.println("Server stopped");
            } catch (SQLException se) {
            }
        }
    }
}
