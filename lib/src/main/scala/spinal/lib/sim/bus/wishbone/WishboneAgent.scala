package spinal.lib.wishbone.sim
import spinal.lib.uvm.sim._

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.wishbone._
import scala.collection.immutable._
import scala.util.Random
import scala.language.implicitConversions

object WishboneAgent{
  def apply(bus: Wishbone,clockdomain: ClockDomain,asMaster : Boolean)(callback: (WishboneTransaction) => Unit@suspendable) = {
    val agent = new WishboneAgent(bus,clockdomain,asMaster)
    agent.addCallback(callback)
    agent.build()
    agent
  }
}
class WishboneAgent(bus: Wishbone,clockdomain: ClockDomain,asMaster : Boolean) extends Agent[WishboneTransaction,WishboneCycle,Wishbone]{
  val driver = new WishboneDriver(bus,clockdomain)
  val sequencer = new WishboneSequencer
  val monitor = new WishboneMonitor(bus,clockdomain,asMaster)
}