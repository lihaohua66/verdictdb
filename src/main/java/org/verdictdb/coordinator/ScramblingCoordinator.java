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

package org.verdictdb.coordinator;

import com.google.common.base.Optional;
import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.execplan.ExecutablePlanRunner;
import org.verdictdb.core.scrambling.*;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBValueException;

import java.util.*;
import java.util.Map.Entry;

public class ScramblingCoordinator {

  private final Set<String> scramblingMethods =
      new HashSet<>(Arrays.asList("uniform", "fastconverge"));

  // default options
  private final Map<String, String> options =
      new HashMap<String, String>() {
        private static final long serialVersionUID = -4491518418086939738L;

        {
          put("tierColumnName", "verdictdbtier");
          put("blockColumnName", "verdictdbblock");
          put("scrambleTableSuffix", "_scrambled");
          put("scrambleTableBlockSize", "1e6");
        }
      };

  Optional<String> scrambleSchema;

  DbmsConnection conn;

  Optional<String> scratchpadSchema;

  public ScramblingCoordinator(DbmsConnection conn) {
    this(conn, null);
  }

  public ScramblingCoordinator(DbmsConnection conn, String scrambleSchema) {
    this(conn, scrambleSchema, scrambleSchema); // uses the same schema
  }

  public ScramblingCoordinator(
      DbmsConnection conn, String scrambleSchema, String scratchpadSchema) {
    this(conn, scrambleSchema, scratchpadSchema, null);
  }

  public ScramblingCoordinator(
      DbmsConnection conn, String scrambleSchema, String scratchpadSchema, Long blockSize) {
    this.conn = conn;
    this.scratchpadSchema = Optional.fromNullable(scratchpadSchema);
    this.scrambleSchema = Optional.fromNullable(scrambleSchema);
    if (blockSize != null) {
      options.put("scrambleTableBlockSize", String.valueOf(blockSize));
    }
  }

  public ScrambleMeta scramble(String originalSchema, String originalTable)
      throws VerdictDBException {
    String newSchema;
    if (scrambleSchema.isPresent()) {
      newSchema = scrambleSchema.get();
    } else {
      newSchema = originalSchema;
    }
    String newTable = originalTable + options.get("scrambleTableSuffix");
    ScrambleMeta meta = scramble(originalSchema, originalTable, newSchema, newTable);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema, String originalTable, String newSchema, String newTable)
      throws VerdictDBException {

    String methodName = "uniform";
    String primaryColumn = null;
    ScrambleMeta meta =
        scramble(originalSchema, originalTable, newSchema, newTable, methodName, primaryColumn);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema,
      String originalTable,
      String newSchema,
      String newTable,
      String methodName)
      throws VerdictDBException {

    String primaryColumn = null;
    ScrambleMeta meta =
        scramble(originalSchema, originalTable, newSchema, newTable, methodName, primaryColumn);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema,
      String originalTable,
      String newSchema,
      String newTable,
      String methodName,
      String primaryColumn)
      throws VerdictDBException {

    // copied options
    Map<String, String> customOptions = new HashMap<>(options);

    ScrambleMeta meta =
        scramble(
            originalSchema,
            originalTable,
            newSchema,
            newTable,
            methodName,
            primaryColumn,
            customOptions);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema,
      String originalTable,
      String newSchema,
      String newTable,
      String methodName,
      String primaryColumn,
      Map<String, String> customOptions)
      throws VerdictDBException {

    // sanity check
    if (!scramblingMethods.contains(methodName.toLowerCase())) {
      throw new VerdictDBValueException("Not supported scrambling method: " + methodName);
    }

    // overwrite options with custom options.
    Map<String, String> effectiveOptions = new HashMap<String, String>();
    for (Entry<String, String> o : options.entrySet()) {
      effectiveOptions.put(o.getKey(), o.getValue());
    }
    for (Entry<String, String> o : customOptions.entrySet()) {
      effectiveOptions.put(o.getKey(), o.getValue());
    }

    // determine scrambling method
    long blockSize = Double.valueOf(effectiveOptions.get("scrambleTableBlockSize")).longValue();
    ScramblingMethod scramblingMethod;
    if (methodName.equalsIgnoreCase("uniform")) {
      scramblingMethod = new UniformScramblingMethod(blockSize);
    } else if (methodName.equalsIgnoreCase("FastConverge") && primaryColumn == null) {
      scramblingMethod = new FastConvergeScramblingMethod(blockSize, scratchpadSchema.get());
    } else if (methodName.equalsIgnoreCase("FastConverge") && primaryColumn != null) {
      scramblingMethod =
          new FastConvergeScramblingMethod(blockSize, scratchpadSchema.get(), primaryColumn);
    } else {
      throw new VerdictDBValueException("Invalid scrambling method: " + methodName);
    }

    // perform scrambling
    ScramblingPlan plan =
        ScramblingPlan.create(
            newSchema, newTable, originalSchema, originalTable, scramblingMethod, effectiveOptions);
    ExecutablePlanRunner.runTillEnd(conn, plan);

    // compose scramble meta
    String blockColumn = effectiveOptions.get("blockColumnName");
    int blockCount = scramblingMethod.getBlockCount();
    String tierColumn = effectiveOptions.get("tierColumnName");
    int tierCount = scramblingMethod.getTierCount();

    Map<Integer, List<Double>> cumulativeDistribution = new HashMap<>();
    for (int i = 0; i < tierCount; i++) {
      List<Double> dist = scramblingMethod.getStoredCumulativeProbabilityDistributionForTier(i);
      cumulativeDistribution.put(i, dist);
    }

    ScrambleMeta meta =
        new ScrambleMeta(
            newSchema,
            newTable,
            originalSchema,
            originalTable,
            blockColumn,
            blockCount,
            tierColumn,
            tierCount,
            cumulativeDistribution);

    return meta;
  }
}
