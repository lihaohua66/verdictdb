package org.verdictdb.core.querying;

import static org.junit.Assert.assertEquals;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.connection.JdbcConnection;
import org.verdictdb.core.execplan.ExecutablePlanRunner;
import org.verdictdb.core.execplan.ExecutionInfoToken;
import org.verdictdb.core.resulthandler.ExecutionTokenReader;
import org.verdictdb.core.scrambling.SimpleTreePlan;
import org.verdictdb.core.sqlobject.AliasedColumn;
import org.verdictdb.core.sqlobject.BaseColumn;
import org.verdictdb.core.sqlobject.BaseTable;
import org.verdictdb.core.sqlobject.ColumnOp;
import org.verdictdb.core.sqlobject.SelectItem;
import org.verdictdb.core.sqlobject.SelectQuery;
import org.verdictdb.core.sqlobject.SubqueryColumn;
import org.verdictdb.exception.VerdictDBDbmsException;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.sqlsyntax.H2Syntax;

public class ProjectionExecutionNodeTest {

  static String originalSchema = "originalschema";

  static String originalTable = "originaltable";

  static String newSchema = "newschema";

  static String newTable  = "newtable";

  static int aggblockCount = 2;

  static DbmsConnection conn;

  @BeforeClass
  public static void setupDbConnAndScrambledTable() throws SQLException, VerdictDBException {
    final String DB_CONNECTION = "jdbc:h2:mem:projectiontest;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = new JdbcConnection(DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD), new H2Syntax());
    conn.execute(String.format("CREATE SCHEMA IF NOT EXISTS\"%s\"", originalSchema));
    conn.execute(String.format("CREATE SCHEMA IF NOT EXISTS\"%s\"", newSchema));
    populateData(conn, originalSchema, originalTable);
  }

  static void populateData(DbmsConnection conn, String schemaName, String tableName) throws VerdictDBDbmsException {
    conn.execute(String.format("CREATE TABLE \"%s\".\"%s\"(\"id\" int, \"value\" double)", schemaName, tableName));
    for (int i = 0; i < 2; i++) {
      conn.execute(String.format("INSERT INTO \"%s\".\"%s\"(\"id\", \"value\") VALUES(%s, %f)",
          schemaName, tableName, i, (double) i+1));
    }
  }

  @Test
  public void testGenerateDependency()  throws VerdictDBException {
    SelectQuery subquery = SelectQuery.create(
        Arrays.<SelectItem>asList(new AliasedColumn(new ColumnOp("avg", new BaseColumn("t1", "value")), "a")),
        new BaseTable(originalSchema, originalTable, "t1"));
    SelectQuery query = SelectQuery.create(
        Arrays.<SelectItem>asList(new AliasedColumn(new BaseColumn("t", "value"), "average")),
        new BaseTable(originalSchema, originalTable, "t"));
    query.addFilterByAnd(new ColumnOp("greater", Arrays.asList(
        new BaseColumn("t", "value"),
        new SubqueryColumn(subquery)
    )));
    QueryExecutionPlan plan = QueryExecutionPlanFactory.create("newschema");
    ProjectionNode node = ProjectionNode.create(plan, query);
    String aliasName = String.format("verdictdbalias_%d_0", plan.getSerialNumber());

    assertEquals(1, node.getExecutableNodeBaseDependents().size());
    SelectQuery rewritten = SelectQuery.create(
        Arrays.<SelectItem>asList(
            new AliasedColumn(new BaseColumn("placeholderSchemaName", aliasName, "a"), "a"))
        , new BaseTable("placeholderSchemaName", "placeholderTableName", aliasName));
    assertEquals(rewritten, ((SubqueryColumn)((ColumnOp) node.getSelectQuery().getFilter().get()).getOperand(1)).getSubquery());
  }

  //
  // select t.value as average
  // from originalSchema.originalTable t
  // where t.value > (select avg(t1.value) a from originalSchema.originalTable t1);
  //
  @Test
  public void testExecuteNode() throws VerdictDBException {
    SelectQuery subquery = SelectQuery.create(
        Arrays.<SelectItem>asList(new AliasedColumn(new ColumnOp("avg", new BaseColumn("t1", "value")), "a")),
        new BaseTable(originalSchema, originalTable, "t1"));
    SelectQuery query = SelectQuery.create(
        Arrays.<SelectItem>asList(new AliasedColumn(new BaseColumn("t", "value"), "average")),
        new BaseTable(originalSchema, originalTable, "t"));
    query.addFilterByAnd(new ColumnOp("greater", Arrays.asList(
        new BaseColumn("t", "value"),
        new SubqueryColumn(subquery)
    )));
    ProjectionNode node = ProjectionNode.create(QueryExecutionPlanFactory.create("newschema"), query);
    // node.print();

//    ExecutionInfoToken subqueryToken = new ExecutionInfoToken();
//    subqueryToken.setKeyValue("schemaName", ((AggExecutionNode)node.dependents.get(0)).newTableSchemaName);
//    subqueryToken.setKeyValue("tableName", ((AggExecutionNode)node.dependents.get(0)).newTableName);
//    ExecutionInfoToken downstreamResult = node.dependents.get(0).executeNode(conn, null);
//    ExecutionInfoToken newTableToken = node.executeNode(conn, Arrays.asList(downstreamResult));

//    ExecutionTokenQueue queue = new ExecutionTokenQueue();
//    node.addBroadcastingQueue(queue);
    ExecutionTokenReader reader = ExecutablePlanRunner.getTokenReader(conn, new SimpleTreePlan(node));

//    node.executeAndWaitForTermination(conn);

    ExecutionInfoToken token = reader.next();
    String newSchemaName = (String) token.getValue("schemaName");
    String newTableName = (String) token.getValue("tableName");
    conn.execute(String.format("DROP TABLE \"%s\".\"%s\"", newSchemaName, newTableName));
  }

  @AfterClass
  static public void clean() throws VerdictDBDbmsException {
    conn.execute(String.format("DROP TABLE \"%s\".\"%s\"", originalSchema, originalTable));
  }
}
