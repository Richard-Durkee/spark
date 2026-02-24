/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql

import org.apache.spark.sql.test.SharedSparkSession

class TwoArgIfSuite extends QueryTest with SharedSparkSession {

  test("two-argument if with SQL and unresolved columns") {
    // Test with actual SQL query using table columns
    val df = spark.sql("SELECT if(id > 0, name) FROM values(1, 'test') as t(id, name)")
    checkAnswer(df, Row("test") :: Nil)

    val df2 = spark.sql("SELECT if(id < 0, name) FROM values(1, 'test') as t(id, name)")
    checkAnswer(df2, Row(null) :: Nil)
  }

  test("two-argument if type coercion") {
    // Verify type is inferred correctly and result is nullable
    val df = spark.sql("SELECT if(id > 0, id) as result FROM values(1), (2) as t(id)")
    assert(df.schema("result").dataType == org.apache.spark.sql.types.IntegerType)
    assert(df.schema("result").nullable == true)
    checkAnswer(df, Row(1) :: Row(2) :: Nil)

    val df2 = spark.sql("SELECT if(id < 0, id) as result FROM values(1) as t(id)")
    checkAnswer(df2, Row(null) :: Nil)

    // Edge case: null literal as trueValue
    val df3 = spark.sql("SELECT if(true, null) as result")
    assert(df3.schema("result").nullable == true)
    checkAnswer(df3, Row(null) :: Nil)

    val df4 = spark.sql("SELECT if(true, cast(null as int)) as result")
    assert(df4.schema("result").dataType == org.apache.spark.sql.types.IntegerType)
    assert(df4.schema("result").nullable == true)
    checkAnswer(df4, Row(null) :: Nil)
  }

  test("two-argument if equivalent to three-argument with null") {
    // Verify 2-arg form behaves identically to 3-arg form with null
    val twoArg = spark.sql("SELECT if(id > 0, id) as result FROM values(1), (-1) as t(id)")
    val threeArg = spark.sql("SELECT if(id > 0, id, null) as result FROM values(1), (-1) as t(id)")

    // Schema should be identical
    assert(twoArg.schema("result").dataType == threeArg.schema("result").dataType)
    assert(twoArg.schema("result").nullable == threeArg.schema("result").nullable)

    // Results should be identical
    checkAnswer(twoArg, threeArg.collect())
  }
}
