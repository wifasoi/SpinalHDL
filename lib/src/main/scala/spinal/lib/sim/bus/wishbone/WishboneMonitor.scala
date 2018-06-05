package spinal.lib.wishbone.sim
import spinal.lib.uvm.sim._

import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.wishbone._
import scala.collection.mutable._
import scala.util.Random



object WishboneMonitor{
  def apply(bus : Wishbone, clockdomain: ClockDomain)(callback: (WishboneCycle) => Unit) = new WishboneMonitor(bus,clockdomain).addCallback(callback)
}

class WishboneMonitor(val bus: Wishbone, val clockdomain: ClockDomain, sampleAsMaster: Boolean = true) extends Monitor[WishboneCycle,Wishbone]{
  val busStatus = WishboneStatus(bus)

  def trigger = busStatus.isAck
  def sample() =  if(sampleAsMaster) WishboneTransaction.sampleAsMaster(bus)
                  else               WishboneTransaction.sampleAsSlave(bus)

  def doSampling() = fork{
    while(true){
      clockdomain.waitSamplingWhere(busStatus.isCycle)
      val cycleBuffer = ListBuffer[WishboneTransaction]()
      while(busStatus.isCycle){
        clockdomain.waitSampling()
        if(busStatus.isAck)
          cycleBuffer += sample()
      }
      val cycle = WishboneCycle(cycleBuffer.toList)
      onTriggerCallbacks.suspendable.foreach( _(cycle) )
    }
  }
}