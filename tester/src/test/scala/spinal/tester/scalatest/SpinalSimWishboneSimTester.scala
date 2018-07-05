package spinal.tester.scalatest

import org.scalatest.FunSuite
import spinal.tester
import spinal.core._
import spinal.core.sim._
import spinal.sim._
import spinal.lib._
import spinal.lib.bus.misc._
import spinal.lib.bus.wishbone._
import spinal.lib.wishbone.sim._
import spinal.lib.sim._
import scala.util.Random

class wishbonesimplebus(config : WishboneConfig) extends Component{
  val io = new Bundle{
    val busIN = slave(Wishbone(config))
    val busOUT = master(Wishbone(config))
  }
  val ff = Reg(Bool)
  io.busIN <> io.busOUT
}
class SpinalSimWishboneSimTester extends FunSuite{
  def simpleBus(conf: WishboneConfig,description : String = "") = {
    val compiled = SimConfig.allOptimisation.withWave.compile(rtl = new wishbonesimplebus(conf))
    compiled.doSim(description){ dut =>
    dut.clockDomain.forkStimulus(period=10)

    val sco = ScoreboardInOrder[WishboneTransaction]()

    val age1 = WishboneAgent(dut.io.busIN,dut.clockDomain,true){ transaction =>
      sco.pushRef(transaction)
      //sco.pushRef(WishboneTransaction(1,1))
    }
      age1.addBuilder{
        // val transaction = for( x <- 0 to 10) yield WishboneTransaction().randomizeAddress(200).randomizeData(200)
        val transaction = for( x <- 0 to 10) yield WishboneTransaction().randomizeAddress(200).randomizeData(200)
        //val transaction = WishboneTransaction().randomizeAddress(200).randomizeData(200)
        WishboneCycle(transaction)
      }

      val age2 = WishboneAgent(dut.io.busOUT,dut.clockDomain,false){ transaction =>
        //sco.pushDut(transaction)
        //sco.pushDut(WishboneTransaction(1,1))
      }

      val mon = WishboneMonitor(dut.io.busOUT,dut.clockDomain,true){ transaction =>
        sco.pushDut(transaction)
      }

      age2.monitor.addCallback{ transaction =>
        println("called")
        age2.sequencer.next()
      }

      age2.addBuilder{
        //val transaction = for( x <- 0 to 1) yield WishboneTransaction().randomizeAddress(200).randomizeData(200)
        //val transaction = WishboneTransaction().randomizeAddress(200).randomizeData(200)
        //WishboneCycle(transaction)
        val transaction = for( x <- 0 to 10) yield WishboneTransaction().randomizeAddress(200).randomizeData(200)
        WishboneCycle(transaction)
      }

      age1.driver.clearBus()
      age2.driver.clearBus()
      dut.clockDomain.waitSampling(10)
      SimTimeout(100000)

      //age2.driver.slaveSink()
      age1.create(10)
      age2.create(100)
      age1.sequencer.start()

      dut.clockDomain.waitSampling(10)
    }
  }

  test("classic"){
    val simple = WishboneConfig(8,8)
    simpleBus(simple,"classic")
  }

  test("pipelined"){
    val simple = WishboneConfig(8,8).pipelined
    simpleBus(simple,"pipelined")
  }
      //val compPipe = SimConfig.allOptimisation.withWave.compile(rtl = new wishbonesimplebus(WishboneConfig(8,8).pipelined))
}


