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

package org.verdictdb.connection;

import org.apache.spark.sql.types.*;

import static java.sql.Types.*;

public class SparkDataTypeConverter {

  public static int typeInt(DataType type) {
    if (type instanceof BinaryType) {
      return BIT;
    } else if (type instanceof BooleanType) {
      return BOOLEAN;
    } else if (type instanceof DateType) {
      return DATE;
    } else if (type instanceof StringType) {
      return VARCHAR;
    } else if (type instanceof TimestampType) {
      return TIMESTAMP;
    } else if (type instanceof DecimalType) {
      return DECIMAL;
    } else if (type instanceof DoubleType) {
      return DOUBLE;
    } else if (type instanceof FloatType) {
      return FLOAT;
    } else if (type instanceof ByteType) {
      return SMALLINT;
    } else if (type instanceof IntegerType) {
      return INTEGER;
    } else if (type instanceof LongType) {
      return BIGINT;
    } else if (type instanceof ShortType) {
      return SMALLINT;
    } else if (type instanceof ArrayType) {
      return ARRAY;
    } else if (type instanceof StructType) {
      return STRUCT;
    } else return OTHER;
  }

  public static String typeClassName(int type) {
    if (type == VARCHAR) {
      return "java.lang.String";
    } else if (type == BIT || type == BOOLEAN) {
      return "java.lang.Boolean";
    } else if (type == DATE) {
      return "java.sql.Date";
    } else if (type == TIMESTAMP) {
      return "java.sql.Timestamp";
    } else if (type == DECIMAL) {
      return "java.math.BigDecimal";
    } else if (type == DOUBLE) {
      return "java.lang.Double";
    } else if (type == FLOAT) {
      return "java.lang.Float";
    } else if (type == SMALLINT) {
      return "java.lang.Short";
    } else if (type == INTEGER) {
      return "java.lang.Integer";
    } else if (type == BIGINT) {
      return "java.lang.Long";
    } else {
      return "java.lang.Object";
    }
  }
}
