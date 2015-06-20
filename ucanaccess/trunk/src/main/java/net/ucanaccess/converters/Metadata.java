/*
Copyright (c) 2012 Marco Amadei.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA

You can contact Marco Amadei at amadei.mar@gmail.com.

 */
package net.ucanaccess.converters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Metadata {
	
	private Connection conn;
	private final static String SCHEMA="CREATE SCHEMA UCA_METADATA AUTHORIZATION DBA";
	
	private final static String TABLES="CREATE MEMORY TABLE  IF NOT EXISTS  UCA_METADATA.TABLE(TABLE_ID INTEGER IDENTITY, TABLE_NAME LONGVARCHAR,ESCAPED_TABLE_NAME LONGVARCHAR, TYPE VARCHAR(5),UNIQUE(TABLE_NAME)) ";
	private final static String COLUMNS="CREATE MEMORY TABLE  IF NOT EXISTS  " +
			"				UCA_METADATA.COLUMN(COLUMN_ID INTEGER IDENTITY, COLUMN_NAME LONGVARCHAR,ESCAPED_COLUMN_NAME LONGVARCHAR, " +
			"				ORIGINAL_TYPE VARCHAR(20),COLUMN_DEF  LONGVARCHAR,TABLE_ID INTEGER, UNIQUE(TABLE_ID,COLUMN_NAME) )";
	
	private final static String COLUMNS_VIEW="CREATE VIEW   UCA_METADATA.COLUMNS_VIEW as " +
			"SELECT t.TABLE_NAME, c.COLUMN_NAME,t.ESCAPED_TABLE_NAME, c.ESCAPED_COLUMN_NAME,c.COLUMN_DEF," +
			"CASE WHEN(c.ORIGINAL_TYPE='COUNTER') THEN 'YES' ELSE 'NO' END as IS_AUTOINCREMENT " +
			"FROM UCA_METADATA.COLUMN c INNER JOIN UCA_METADATA.TABLE t ON (t.TABLE_ID=c.TABLE_ID)";
	
	private final static String FK="ALTER TABLE UCA_METADATA.COLUMN   " +
			"ADD CONSTRAINT UCA_METADATA_FK FOREIGN KEY (TABLE_ID) REFERENCES UCA_METADATA.TABLE (TABLE_ID) ON DELETE CASCADE";
	
	private final static String TABLE_RECORD="INSERT INTO UCA_METADATA.TABLE( TABLE_NAME,ESCAPED_TABLE_NAME, TYPE) VALUES(?,?,?)";
	private final static String COLUMN_RECORD="INSERT INTO UCA_METADATA.COLUMN(COLUMN_NAME,ESCAPED_COLUMN_NAME,ORIGINAL_TYPE, TABLE_ID) " +
			"VALUES(?,?,?,?)";
	
	private final static String SELECT_COLUMN="SELECT c.COLUMN_NAME,c.ORIGINAL_TYPE='COUNTER' as IS_AUTOINCREMENT, c.ORIGINAL_TYPE='MONEY' as IS_CURRENCY  " +
			"				FROM UCA_METADATA.COLUMN  c INNER JOIN UCA_METADATA.TABLE  t " +
			"				ON(t.TABLE_ID=c.TABLE_ID ) WHERE t.ESCAPED_TABLE_NAME=? AND c.ESCAPED_COLUMN_NAME=? ";
	
	private final static String SELECT_TABLE_METADATA="SELECT TABLE_ID, TABLE_NAME FROM UCA_METADATA.TABLE WHERE ESCAPED_TABLE_NAME=? ";
	private final static String DROP_TABLE="DELETE FROM UCA_METADATA.TABLE WHERE TABLE_NAME=?";
	private final static String UPDATE_COLUMN_DEF="UPDATE UCA_METADATA.COLUMN c SET c.COLUMN_DEF=? WHERE COLUMN_NAME=? " +
			" AND EXISTS(SELECT * FROM UCA_METADATA.TABLE t WHERE t.TABLE_NAME=? AND t.TABLE_ID=c.TABLE_ID) ";
	
	public static enum Types{VIEW,TABLE}
	
	public Metadata(Connection conn) throws SQLException {
		super();
		this.conn = conn;
		
	}
	
	public void createMetadata() throws SQLException{
		Statement st=null;
		try{
			st=conn.createStatement();
			st.execute(SCHEMA);
			st.execute(TABLES);
			st.execute(COLUMNS);
			st.execute(FK);
			st.execute(COLUMNS_VIEW);
		}finally{
			if(st!=null)st.close();
		}
	}
	
	public Integer newTable(String name,String escaped,Types type) throws SQLException{
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(TABLE_RECORD,PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setString(2, escaped);
			ps.setString(3, type.name());
			ps.executeUpdate();
			ResultSet rs= ps.getGeneratedKeys();
			rs.next();
			
			return rs.getInt(1);  
		}
		catch(Exception e)   {
			return getTableId(escaped);
		}
		finally{
			if(ps!=null)ps.close();
		}
	}
	
	public void newColumn(String name,String escaped,String originalType, Integer idTable) throws SQLException{
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(COLUMN_RECORD);
			ps.setString(1, name);
			ps.setString(2, escaped);
			ps.setString(3, originalType);
			ps.setInt(4, idTable);
			ps.executeUpdate();
		}catch(Exception e)   {
			
		}
		
		finally{
			if(ps!=null)ps.close();
		}
	}
	
	
	public String getColumn(String tableName,String columnName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(SELECT_COLUMN);
			ps.setString(1, tableName);
			ps.setString(2, columnName);
			ResultSet rs= ps.executeQuery();
			if(rs.next()){
				return rs.getString("COLUMN_NAME");
			}
			else return null;
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	public boolean isAutoIncrement(String tableName,String columnName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(SELECT_COLUMN);
			ps.setString(1, tableName);
			ps.setString(2, columnName);
			ResultSet rs= ps.executeQuery();
			if(rs.next()){
				return rs.getBoolean("IS_AUTOINCREMENT");
			}
			else return false;
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	
	public boolean isCurrency(String tableName,String columnName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(SELECT_COLUMN);
			ps.setString(1, tableName);
			ps.setString(2, columnName);
			ResultSet rs= ps.executeQuery();
			if(rs.next()){
				return rs.getBoolean("IS_CURRENCY");
			}
			else return false;
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	public Integer getTableId(String escapedName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(SELECT_TABLE_METADATA);
			ps.setString(1, escapedName);
			ResultSet rs= ps.executeQuery();
			if(rs.next()){
				return rs.getInt("TABLE_ID");
			}
			else return null;
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	
	public String getTableName(String escapedName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(SELECT_TABLE_METADATA);
			ps.setString(1, escapedName);
			
			ResultSet rs= ps.executeQuery();
			if(rs.next()){
				return rs.getString("NAME");
			}
			else return null;
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	public void dropTable(String tableName) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(DROP_TABLE);
			ps.setString(1, tableName);
			ps.execute();
			
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
	
	public void columnDef(String tableName,String columnName,String def) throws SQLException {
		PreparedStatement ps=null;
		try{
			ps=conn.prepareStatement(UPDATE_COLUMN_DEF);
			ps.setString(1, def);
			ps.setString(2, columnName);
			ps.setString(3, tableName);
			ps.execute();
			
			
		}finally{
			if(ps!=null)ps.close();
		}
	}
}
