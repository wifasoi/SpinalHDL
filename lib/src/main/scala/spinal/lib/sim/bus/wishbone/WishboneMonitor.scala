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
  def apply(bus : Wishbone, clockdomain: ClockDomain, asMaster: Boolean = true)(callback: (WishboneTransaction) => Unit@suspendable) = new WishboneMonitor(bus,clockdomain,asMaster).addCallback(callback)
}

class WishboneMonitor(val bus: Wishbone, val clockdomain: ClockDomain, asMaster: Boolean = true) extends Monitor[WishboneTransaction,Wishbone]{
  val busStatus = WishboneStatus(bus,clockdomain)

  def trigger : Boolean = if(asMaster)  busStatus.isAck
                          else          busStatus.isTransfer
  def sample(): WishboneTransaction = if(asMaster) WishboneTransaction.sampleAsMaster(bus)
                                      else         WishboneTransaction.sampleAsSlave(bus)

  def doSampling() = fork{
    while(true){
      //clockdomain.waitSamplingWhere(trigger)
      //waitUntil(trigger)
      clockdomain.waitSampling()
      val dummy = if(trigger){
        val transaction = sample()
        //println("sampled!: " + asMaster + transaction)
        onTriggerCallbacks.suspendable.foreach( _(transaction) )
      }

    }
  }
}