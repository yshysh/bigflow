/***************************************************************************
 *
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **************************************************************************/

package com.baidu.flume.runtime.spark.impl.util

import java.io.{File, FileInputStream, FileNotFoundException, IOException}

import org.scalatest.FunSuite
import org.mockito.Mockito._
/**
  * Test Suite for Utils
  *
  * @author Ye, Xianjin(bigflow-opensource@baidu.com)
  */
class UtilsTest extends FunSuite {

  test("testMergeCacheFileLists") {
    assert(Utils.mergeCacheFileLists("") === null)
    assert(Utils.mergeCacheFileLists("1", "2#12") === "1,2#12")
    assert(Utils.mergeCacheFileLists("1", "", "1,2,3") === "1,1,2,3")
    assert(Utils.mergeCacheFileLists("", "", "") === null)
  }

  test("testDirCommonItems") {
    // nothing here...
  }

  test("autoClose function") {
    // normal case
    val fakeISOne = mock(classOf[FileInputStream])
    when(fakeISOne.read()).thenReturn(1)
    assertResult(1) {
      Utils.autoClose(fakeISOne) { is => is.read() }
    }
    verify(fakeISOne).close()

    // read throws exception
    val fakeISTwo = mock(classOf[FileInputStream])
    doThrow(new IOException).when(fakeISTwo).read()
    intercept[IOException] {
      Utils.autoClose(fakeISTwo) { is => is.read() }
    }
    verify(fakeISTwo).close()
    verify(fakeISTwo).read()

    // close throws exception
    val fakeISThree = mock(classOf[FileInputStream])
    doThrow(new RuntimeException).when(fakeISThree).close()
    Utils.autoClose(fakeISThree) { _ => }
    verify(fakeISThree).close()

    // class creation throws exception
    val fakeISFour = mock(classOf[FileInputStream])
    def sourceFromNotExistedPath = {
      scala.io.Source.fromFile("/path/not/exist")
      fakeISFour
    }
    intercept[FileNotFoundException] {
      Utils.autoClose(sourceFromNotExistedPath) { it =>
        it.read()
      }
    }
    verifyZeroInteractions(fakeISFour)
  }

}
