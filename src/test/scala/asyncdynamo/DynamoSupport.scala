/*
 * Copyright 2012 2ndlanguage Limited.
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

package asyncdynamo

import nonblocking.{CreateTable, TableExists}
import org.scalatest.{BeforeAndAfterAll, Suite}
import akka.util.Timeout
import akka.util.duration._
import com.amazonaws.services.dynamodb.model.AttributeValue

object DynamoTestDataObjects{
  case class DynamoTestObject(id:String, someValue:String)

  implicit object DynamoTestDO extends DynamoObject[DynamoTestObject]{
    def toDynamo(t: DynamoTestObject) = Map("id"->t.id, "someValue"->t.someValue)
    def fromDynamo(a: Map[String, AttributeValue]) = DynamoTestObject(a("id").getS, a("someValue").getS)
    protected val table = "%s_dynamotest" format Option(System.getenv("USER")).getOrElse("unknown")
  }

  case class DynamoTestWithRangeObject(id:String, rangeValue:String, otherValue: String)
  implicit object DynamoTestWithRangeDO extends DynamoObject[DynamoTestWithRangeObject]{
    def toDynamo(t: DynamoTestWithRangeObject) = Map("id"->t.id, "rangeValue"->t.rangeValue, "otherValue" -> t.otherValue)
    def fromDynamo(a: Map[String, AttributeValue]) = DynamoTestWithRangeObject(a("id").getS, a("rangeValue").getS, a("otherValue").getS)
    protected val table = "%s_dynamotest_withrange" format Option(System.getenv("USER")).getOrElse("unknown")
    override val range = Some(key("rangeValue", "S"))
  }

  case class Broken(id:String)
  implicit object BrokenDO extends DynamoObject[Broken]{
    def toDynamo(t: Broken) = Map()
    def fromDynamo(a: Map[String, AttributeValue]) = Broken("wiejfi")
    protected def table = "nonexistenttable"
  }
}

trait DynamoSupport extends BeforeAndAfterAll{ self : Suite =>
  implicit val dynamo = Dynamo(DynamoConfig(System.getProperty("amazon.accessKey"), System.getProperty("amazon.secret"), tablePrefix = "devng_", endpointUrl = System.getProperty("dynamo.url", "https://dynamodb.eu-west-1.amazonaws.com" )), connectionCount = 4)
  implicit val timeout = Timeout(10 seconds)

  override protected def afterAll() {
    dynamo ! 'stop
    super.afterAll()
  }
}

trait DynamoTestObjectSupport extends BeforeAndAfterAll with DynamoSupport{ self : Suite =>
  import DynamoTestDataObjects._

  override protected def beforeAll() {
    super.beforeAll()
    if (!TableExists[DynamoTestObject]().blockingExecute){
      CreateTable[DynamoTestObject]().blockingExecute(dynamo,1 minute)
    }
    if (!TableExists[DynamoTestWithRangeObject]().blockingExecute){
      CreateTable[DynamoTestWithRangeObject](100, 100).blockingExecute(dynamo,1 minute)
    }
  }
}


