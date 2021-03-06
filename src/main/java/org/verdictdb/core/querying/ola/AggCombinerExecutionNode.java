/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.core.querying.ola;

import org.apache.commons.lang3.tuple.Pair;
import org.verdictdb.connection.DbmsQueryResult;
import org.verdictdb.core.execplan.ExecutionInfoToken;
import org.verdictdb.core.querying.*;
import org.verdictdb.core.sqlobject.*;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBValueException;

import java.util.ArrayList;
import java.util.List;

public class AggCombinerExecutionNode extends CreateTableAsSelectNode {

  private static final long serialVersionUID = -5083977853340736042L;

  AggMeta aggMeta = new AggMeta();

  static String unionTableAlias = "unionTable";

  private AggCombinerExecutionNode(IdCreator namer) {
    super(namer, null);
  }

  public static AggCombinerExecutionNode create(
      IdCreator namer,
      ExecutableNodeBase leftQueryExecutionNode,
      ExecutableNodeBase rightQueryExecutionNode)
      throws VerdictDBValueException {

    AggCombinerExecutionNode node = new AggCombinerExecutionNode(namer);

    SelectQuery rightQuery =
        ((QueryNodeBase) rightQueryExecutionNode)
            .getSelectQuery(); // the right one is the aggregate query
    String leftAliasName = namer.generateAliasName();
    String rightAliasName = namer.generateAliasName();

    // create placeholders to use
    Pair<BaseTable, SubscriptionTicket> leftBaseAndTicket =
        node.createPlaceHolderTable(leftAliasName);
    Pair<BaseTable, SubscriptionTicket> rightBaseAndTicket =
        node.createPlaceHolderTable(rightAliasName);

    // compose a join query
    SelectQuery joinQuery =
        composeUnionQuery(rightQuery, leftBaseAndTicket.getLeft(), rightBaseAndTicket.getLeft());

    leftQueryExecutionNode.registerSubscriber(leftBaseAndTicket.getRight());
    rightQueryExecutionNode.registerSubscriber(rightBaseAndTicket.getRight());

    node.setSelectQuery(joinQuery);
    return node;
  }

  /**
   * Composes a query that joins two tables. The select list is inferred from a given query.
   *
   * @param rightQuery The query from which to infer a select list
   * @param leftBase
   * @param rightBase
   * @return
   */
  static SelectQuery composeUnionQuery(
      SelectQuery rightQuery, BaseTable leftBase, BaseTable rightBase) {

    List<SelectItem> allItems = new ArrayList<>();
    // replace the select list
    List<String> groupAliasNames = new ArrayList<>();
    for (SelectItem item : rightQuery.getSelectList()) {
      if (item.isAggregateColumn()) {
        if (item instanceof AliasedColumn
            && ((AliasedColumn) item).getColumn() instanceof ColumnOp
            && (((ColumnOp) ((AliasedColumn) item).getColumn()).getOpType().equals("max")
                || ((ColumnOp) ((AliasedColumn) item).getColumn()).getOpType().equals("min"))) {
          allItems.add(
              new AliasedColumn(
                  new ColumnOp(
                      ((ColumnOp) ((AliasedColumn) item).getColumn()).getOpType(),
                      new BaseColumn(unionTableAlias, ((AliasedColumn) item).getAliasName())),
                  ((AliasedColumn) item).getAliasName()));
        } else
          allItems.add(
              new AliasedColumn(
                  ColumnOp.sum(
                      new BaseColumn(unionTableAlias, ((AliasedColumn) item).getAliasName())),
                  ((AliasedColumn) item).getAliasName()));
      } else {
        allItems.add(
            new AliasedColumn(
                new BaseColumn(unionTableAlias, ((AliasedColumn) item).getAliasName()),
                ((AliasedColumn) item).getAliasName()));
        groupAliasNames.add(((AliasedColumn) item).getAliasName());
      }
    }

    SelectQuery left = SelectQuery.create(new AsteriskColumn(), leftBase);
    SelectQuery right = SelectQuery.create(new AsteriskColumn(), rightBase);
    SetOperationRelation newBase =
        new SetOperationRelation(left, right, SetOperationRelation.SetOpType.unionAll);
    newBase.setAliasName(unionTableAlias);
    SelectQuery unionQuery = SelectQuery.create(allItems, newBase);
    for (String a : groupAliasNames) {
      unionQuery.addGroupby(new AliasReference(a));
    }
    /*
    // finally, creates a join query
    SelectQuery joinQuery = SelectQuery.create(
        allItems,
        Arrays.<AbstractRelation>asList(leftBase, rightBase));
    for (String a : groupAliasNames) {
      joinQuery.addFilterByAnd(
          ColumnOp.equal(new BaseColumn(leftAliasName, a), new BaseColumn(rightAliasName, a)));
    }
    */

    return unionQuery;
  }

  @Override
  public SqlConvertible createQuery(List<ExecutionInfoToken> tokens) throws VerdictDBException {
    for (ExecutionInfoToken token : tokens) {
      AggMeta aggMeta = (AggMeta) token.getValue("aggMeta");
      //      if (aggMeta == null) {
      //        throw new VerdictDBValueException("No aggregation metadata is passed from downstream
      // nodes.");
      //      }

      if (aggMeta != null) {
        this.aggMeta.getCubes().addAll(aggMeta.getCubes());
        this.aggMeta.setAggAlias(aggMeta.getAggAlias());
        this.aggMeta.setOriginalSelectList(aggMeta.getOriginalSelectList());
        this.aggMeta.setAggColumn(aggMeta.getAggColumn());
        this.aggMeta.setAggColumnAggAliasPair(aggMeta.getAggColumnAggAliasPair());
        this.aggMeta.setAggColumnAggAliasPairOfMaxMin(aggMeta.getAggColumnAggAliasPairOfMaxMin());
        this.aggMeta.setMaxminAggAlias(aggMeta.getMaxminAggAlias());
      }
    }
    return super.createQuery(tokens);
  }

  @Override
  public ExecutionInfoToken createToken(DbmsQueryResult result) {
    ExecutionInfoToken token = super.createToken(result);
    token.setKeyValue("aggMeta", aggMeta);
    token.setKeyValue("dependentQuery", this.selectQuery);
    return token;
  }
}
