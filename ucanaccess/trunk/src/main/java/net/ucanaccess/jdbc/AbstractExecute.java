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
package net.ucanaccess.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.ucanaccess.commands.DDLCommandEnlist;
import net.ucanaccess.converters.SQLConverter;
import net.ucanaccess.converters.SQLConverter.DDLType;
import net.ucanaccess.jdbc.FeatureNotSupportedException.NotSupportedMessage;



public abstract class AbstractExecute {
	protected enum CommandType {
		BATCH, NO_ARGUMENTS, PREPARED_STATEMENT, UPDATABLE_RESULTSET, WITH_AUTO_GENERATED_KEYS, WITH_COLUMNS_NAME, WITH_INDEXES
	}
	protected int autoGeneratedKeys;
	protected String[] columnNames;
	protected CommandType commandType;
	protected int[] indexes;
	private UcanaccessResultSet resultSet;
	protected String sql;

	private UcanaccessStatement statement;

	protected AbstractExecute(UcanaccessPreparedStatement statement) {
		this.statement = statement;
		this.commandType = CommandType.PREPARED_STATEMENT;
	}

	protected AbstractExecute(UcanaccessResultSet resultSet) {
		this.resultSet = resultSet;
		this.statement = resultSet.getWrappedStatement();
		this.commandType = CommandType.UPDATABLE_RESULTSET;
	}

	public AbstractExecute(UcanaccessStatement statement) {
		this.statement = statement;
	}

	protected AbstractExecute(UcanaccessStatement statement, String sql) {
		this(statement);
		this.commandType = CommandType.NO_ARGUMENTS;
		this.sql = sql;
	}

	protected AbstractExecute(UcanaccessStatement statement, String sql,
			int autoGeneratedKeys) {
		this(statement, sql);
		this.autoGeneratedKeys = autoGeneratedKeys;
		this.commandType = CommandType.WITH_AUTO_GENERATED_KEYS;
	}

	protected AbstractExecute(UcanaccessStatement statement, String sql,
			int[] indexes) {
		this(statement, sql);
		this.indexes = indexes;
		this.commandType = CommandType.WITH_INDEXES;
	}

	protected AbstractExecute(UcanaccessStatement statement, String sql,
			String[] columnNames) {
		this(statement, sql);
		this.columnNames = columnNames;
		this.commandType = CommandType.WITH_COLUMNS_NAME;
	}

	
	
	
	
	private Object addDDLCommand() throws SQLException {
		Object ret;
		try {
			DDLType ddlType = SQLConverter.getDDLType(sql);
			if (ddlType == null)
				throw new FeatureNotSupportedException(
						NotSupportedMessage.NOT_SUPPORTED_YET);
			
			
			String sql0=SQLConverter.convertSQL(sql);
			String ddlExpr = ddlType.in(DDLType.CREATE_TABLE,
					DDLType.CREATE_TABLE_AS_SELECT) ? SQLConverter
					.convertCreateTable(sql0) : sql0;
				ret=	(this instanceof Execute) ?statement.getWrapped().execute(ddlExpr):statement.getWrapped().executeUpdate(ddlExpr);
		
					DDLCommandEnlist ddle = new DDLCommandEnlist();
			ddle.enlistDDLCommand(SQLConverter.restoreWorkAroundFunctions(sql), ddlType);
		} catch (Exception e) {
			throw new SQLException(e.getMessage());
		}
		return ret;
	}

	private boolean checkDDL() {
		return SQLConverter.checkDDL(this.sql);
	}

	public Object executeBase() throws SQLException {
		UcanaccessConnection conn = (UcanaccessConnection) statement
				.getConnection();
		UcanaccessConnection.setCtxConnection(conn);
		
		if(this.commandType.equals(CommandType.BATCH)){
			UcanaccessConnection.setCtxExecId(UcanaccessConnection. BATCH_ID);
		}
		else{
			UcanaccessConnection.setCtxExecId(Math.random() + "");
		}
		Object retv;
		
			if (checkDDL()) {
				retv = addDDLCommand();
			} else {
				try{
					retv = executeWrapped();
				}catch(SQLException e){
					if (conn.getAutoCommit()) {
						conn.rollback();
					}
					throw e;
				}
			}
			if (conn.getAutoCommit()) {
				conn.commit();
			}
		
		
		return retv;
	}

	public abstract Object executeWrapped() throws SQLException;

	ResultSet getWrappedResultSet() {
		return resultSet.getWrapped();
	}

	Statement getWrappedStatement() {
		return statement.getWrapped();
	}

	void setStatement(UcanaccessStatement statement) {
		this.statement = statement;
	}
}
