package org.eclipse.test.internal.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQL {
    
    private Connection fConn;
    private PreparedStatement fInsertSession, fInsertSample, fInsertDraw, fInsertDataPoint, fInsertScalar;
    private PreparedStatement fQueryConfig, fInsertConfig;
    private PreparedStatement fQueryScenario, fInsertScenario;
    private PreparedStatement fQueryDimension, fInsertDimension;
    private PreparedStatement fQuery1;
    

    SQL(Connection con) {
        fConn= con;
    }
    
    void createPreparedStatements() throws SQLException {
        fInsertSession= fConn.prepareStatement(
                "insert into SESSION (VERSION, CONFIG_ID) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertConfig= fConn.prepareStatement(
                "insert into CONFIG (HOST, PLATFORM) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertScenario= fConn.prepareStatement(
                "insert into SCENARIO (NAME) values (?)", Statement.RETURN_GENERATED_KEYS);
        fInsertSample= fConn.prepareStatement(
                "insert into SAMPLE (SESSION_ID, SCENARIO_ID, VARIATION_ID) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        fInsertDraw= fConn.prepareStatement(
                "insert into DRAW (SAMPLE_ID, SEQ) values (?, 123)", Statement.RETURN_GENERATED_KEYS);
        fInsertDataPoint= fConn.prepareStatement(
                "insert into DATAPOINT (DRAW_ID, SEQ) values (?, 123)", Statement.RETURN_GENERATED_KEYS);
        fInsertDimension= fConn.prepareStatement(
                "insert into DIMENSION (NAME) values (?)", Statement.RETURN_GENERATED_KEYS);
        fInsertScalar= fConn.prepareStatement(
                "insert into SCALAR values (?, ?, ?)");

        fQueryConfig= fConn.prepareStatement(
                "select ID from CONFIG where HOST = ? and PLATFORM = ?");
        fQueryScenario= fConn.prepareStatement(
                "select ID from SCENARIO where NAME = ?");
        fQueryDimension= fConn.prepareStatement(
                "select ID from DIMENSION where NAME = ?");
        
        fQuery1= fConn.prepareStatement(
                "select SCALAR.VALUE from SCALAR, DATAPOINT, DRAW, SAMPLE, SCENARIO, CONFIG, SESSION " +
            		"where SCALAR.DATAPOINT_ID = DATAPOINT.ID and " +
            		"SCALAR.DIM_ID = ? and " +
            		"DATAPOINT.DRAW_ID = DRAW.ID and " +
            		"DRAW.SAMPLE_ID = SAMPLE.ID and " +
            		"SAMPLE.SCENARIO_ID = SCENARIO.ID and " +
            		"SCENARIO.NAME = ? and " +
            		"SAMPLE.SESSION_ID = SESSION.ID and " +
            		"SESSION.VERSION = ? and " +
            		"SESSION.CONFIG_ID = CONFIG.ID and " +
            		"CONFIG.HOST = ? and " +
            		"CONFIG.PLATFORM = ?");
    }
    
    void initialize() throws SQLException {
        Statement stmt= fConn.createStatement();
        
        stmt.executeUpdate(
            "create table SESSION (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "VERSION varchar(40) not null," +
                "CONFIG_ID int not null" +
            ")"
        );
        stmt.executeUpdate(
            "create table CONFIG (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "HOST varchar(40)," +
                "PLATFORM varchar(20)" +
            ")"
        );
        stmt.executeUpdate(
            "create table TAG (" +
                "ID varchar(40) not null primary key," +
                "SESSION_ID int not null" +
            ")"
        );
        stmt.executeUpdate(
            "create table SAMPLE (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "SESSION_ID int not null," +
                "SCENARIO_ID int not null," +
                "VARIATION_ID int" +
            ")"
        );           
        stmt.executeUpdate(
            "create table SCENARIO (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "NAME varchar(255)" +
            ")"
        );     
        stmt.executeUpdate(
            "create table VARIATION (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "INFO varchar(255)" +
            ")"
        );        
        stmt.executeUpdate(
            "create table DRAW (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "SAMPLE_ID int not null," +
                "SEQ int not null" +
            ")"
        );
        stmt.executeUpdate(
            "create table DATAPOINT (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "DRAW_ID int not null," +
                "SEQ int not null" +
            ")"
        );
        stmt.executeUpdate(
            "create table SCALAR (" +
                "DATAPOINT_ID int not null," +
                "DIM_ID int not null," +
                "VALUE int not null" +
            ")"
        ); 
        stmt.executeUpdate(
            "create table DIMENSION (" +
                "ID int not null GENERATED ALWAYS AS IDENTITY," +
                "NAME varchar(20)" +
            ")"
        );
        stmt.close();
    }
    
    private static int create(PreparedStatement stmt) throws SQLException {
        stmt.executeUpdate();
        ResultSet rs= stmt.getGeneratedKeys();
        if (rs != null) {
	        try {
		        if (rs.next()) {
			        BigDecimal idColVar= rs.getBigDecimal(1);
			        return idColVar.intValue();
		        }
	        } finally {
	            rs.close();
	        }
        }
        return 0;
	}

    int addSession(String version, int config_id) throws SQLException {
        fInsertSession.setString(1, version);
        fInsertSession.setInt(2, config_id);
        return create(fInsertSession);
    }
    
    int getConfig(String host, String platform) throws SQLException {
        fQueryConfig.setString(1, host);
        fQueryConfig.setString(2, platform);
        ResultSet result= fQueryConfig.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertConfig.setString(1, host);
        fInsertConfig.setString(2, platform);
        return create(fInsertConfig);
    }
    
    int getScenario(String name) throws SQLException {
        fQueryScenario.setString(1, name);
        ResultSet result= fQueryScenario.executeQuery();
        while (result.next())
            return result.getInt(1);

        fInsertScenario.setString(1, name);
        return create(fInsertScenario);
    }
    
    int createSample(int session_id, int scenario_id, int variation_id) throws SQLException {
        fInsertSample.setInt(1, session_id);
        fInsertSample.setInt(2, scenario_id);
        fInsertSample.setInt(3, variation_id);
        return create(fInsertSample);
    }
    
    int createDraw(int sample_id) throws SQLException {
        fInsertDraw.setInt(1, sample_id);
        return create(fInsertDraw);
    }
    
    int createDataPoint(int draw_id) throws SQLException {
        fInsertDataPoint.setInt(1, draw_id);
        return create(fInsertDataPoint);
    }

    int getDimension(String dimname) throws SQLException {
        fQueryDimension.setString(1, dimname);
        ResultSet result= fQueryDimension.executeQuery();
        while (result.next())
            return result.getInt(1);
        
        fInsertDimension.setString(1, dimname);
        return create(fInsertDimension);        
    }
   
    void createScalar(int dataPoint_id, int dim_id, int value) throws SQLException {
        fInsertScalar.setInt(1, dataPoint_id);
        fInsertScalar.setInt(2, dim_id);
        fInsertScalar.setInt(3, value);
        create(fInsertScalar);
    }
    
    ResultSet query(String host, String platform, String version, String test, int dim_id) throws SQLException {
        fQuery1.setInt(1, dim_id);
        fQuery1.setString(2, test);
        fQuery1.setString(3, version);
        fQuery1.setString(4, host);
        fQuery1.setString(5, platform);
        return fQuery1.executeQuery();
    }
}
