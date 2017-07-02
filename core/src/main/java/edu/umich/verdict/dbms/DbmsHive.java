package edu.umich.verdict.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.datatypes.SampleParam;
import edu.umich.verdict.datatypes.TableUniqueName;
import edu.umich.verdict.datatypes.VerdictResultSet;
import edu.umich.verdict.exceptions.VerdictException;
import edu.umich.verdict.util.VerdictLogger;

public class DbmsHive extends DbmsImpala {

	public DbmsHive(VerdictContext vc, String dbName, String host, String port, String schema, String user,
			String password, String jdbcClassName) throws VerdictException {
		super(vc, dbName, host, port, schema, user, password, jdbcClassName);
	}
	
	@Override
	public String getQuoteString() {
		return "`";
	}
	
	@Override
	public ResultSet describeTable(TableUniqueName tableUniqueName)  throws VerdictException {
		return executeQuery(String.format("describe %s", tableUniqueName));
	}
	
	@Override
	public ResultSet getDatabaseNames() throws VerdictException {
		return executeQuery("show databases");
	}
	
	protected TableUniqueName justCreateUniverseSampleTableOf(SampleParam param) throws VerdictException {
		TableUniqueName sampleTableName = param.sampleTableName();
		String sql = String.format("CREATE TABLE %s AS SELECT * FROM %s "
								 + "WHERE pmod(conv(substr(md5(%s),17,16),16,10),10000) <= %.4f",
								 sampleTableName, param.originalTable, param.columnNames.get(0), param.samplingRatio*10000);
		VerdictLogger.debug(this, String.format("Creates a table: %s", sql));
		this.executeUpdate(sql);
		VerdictLogger.debug(this, "Done.");
		return sampleTableName;
	}
	
}