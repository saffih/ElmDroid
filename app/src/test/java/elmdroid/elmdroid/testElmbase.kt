package elmdroid.elmdroid


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 26/04/17.
 */

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.FunSpec

//
//class MyTests : FunSpec() {
//    init {
//        test("String.length should return the length of the string") {
//            "sammy".length shouldBe 5
//            "".length shouldBe 0
//        }
//    }
//
//
//}



class QueTest : FunSpec() {
    val noneQue = Que(listOf<Int>())
    init {
        val p=1.que().split()
        val noneSplit= noneQue.split()
        test("Que split operations ") {
            p shouldEqual  Pair(1, noneQue)
            noneSplit shouldEqual Pair(null, noneQue)
        }

        test("Que join operations ") {
            1.que().lst.size shouldBe 1
            noneQue.lst.size shouldBe 0
            (noneQue + 1.que() + 1.que()).lst.size shouldBe 2
            (1.que() + 1.que()).lst.size shouldBe 2
            (1.que() + 1.que() + noneQue).lst.size shouldBe 2
            noneQue.lst.size shouldBe 0
        }

        test("Que order") {
            (1.que() + 2 + 3).lst.size shouldBe 3
            (1.que() + 2 + 3).lst shouldBe listOf(1,2,3)
            (3.que() + 2 + 1).lst shouldBe listOf(3,2,1)
        }
    }
}
