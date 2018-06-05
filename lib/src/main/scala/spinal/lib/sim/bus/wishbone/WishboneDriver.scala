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

object WishboneStatus{
  def apply(bus: Wishbone) = new WishboneStatus(bus)
}

class WishboneStatus(bus: Wishbone){
  def isCycle   : Boolean = bus.CYC.toBoolean
  def isStall   : Boolean = if(bus.config.isPipelined)  isCycle && bus.STALL.toBoolean //TODO
                            else                        false
  def isTransfer: Boolean = if(bus.config.isPipelined)  isCycle && bus.STB.toBoolean //&& !bus.STALL.toBoolean //TODO
                            else                        isCycle && bus.STB.toBoolean

  def isAck     : Boolean = if(bus.config.isPipelined)  isCycle &&  bus.ACK.toBoolean //TODO
                            else                        isTransfer &&  bus.ACK.toBoolean

  def isWrite   : Boolean =                             isTransfer &&  bus.WE.toBoolean
  def isRead    : Boolean =                             isTransfer && !bus.WE.toBoolean
}

class WishboneDriver(val bus: Wishbone, val clockdomain: ClockDomain) extends Driver[WishboneCycle,Wishbone]{
  val busStatus = WishboneStatus(bus)

  def send(transaction : WishboneTransaction): Unit@suspendable = {
    transaction.driveAsMaster(bus)
    if(!bus.config.isPipelined) clockdomain.waitSamplingWhere(busStatus.isAck)
    else clockdomain.waitSamplingWhere(!busStatus.isStall)
  }

  def sendBlock(cycle: WishboneCycle): Unit@suspendable = {
    bus.CYC #= true
    cycle.transactions.dropRight(1).suspendable.foreach{ tran =>
      bus.STB #= true
      send(tran)
      if(!bus.config.isPipelined){
        bus.STB #= false
        clockdomain.waitSampling()
      }
    }
    bus.STB #= true
    send(cycle.transactions.last)
    bus.STB #= false
    bus.CYC #= false
  }


  def sendPipelinedBlock(cycle: WishboneCycle): Unit@suspendable = {
    bus.CYC #= true
    bus.STB #= true
    val ackCounter = fork{
      var counter = 0
      while(counter < cycle.transactions.size){
        clockdomain.waitSamplingWhere(busStatus.isAck)
        counter = counter + 1
      }
    }
    cycle.transactions.toList.suspendable.foreach(send(_))
    bus.STB #= false
    ackCounter.join()
    bus.CYC #= false
  }

  def drive(cycle: WishboneCycle): Unit@suspendable = {
    if(bus.config.isPipelined)  sendPipelinedBlock(cycle)
    else                        sendBlock(cycle)
    clockdomain.waitSampling()
  }
//////////////////////////////////////////////////////////////////////////
  def slaveAckResponse(): Unit@suspendable = {
    clockdomain.waitSamplingWhere(busStatus.isTransfer)
    bus.ACK #= true
    waitUntil(!busStatus.isTransfer)
    bus.ACK #= false
  }

  def slaveAckPipelinedResponse(): Unit@suspendable = {
    clockdomain.waitSamplingWhere(busStatus.isCycle)
    val cycle = fork{
      fork{
        waitUntil(!busStatus.isCycle)
        bus.ACK #= false
        bus.STALL #= false
      }
      while(busStatus.isCycle){
        val ack = Random.nextBoolean
        bus.ACK #= ack
        bus.STALL #= Random.nextBoolean && !ack
        clockdomain.waitSampling()
      }
    }
    cycle.join()
  }

  def slaveSink(): Unit@suspendable = {
    val dummy = fork{
      while(true){
        if(bus.config.isPipelined)  slaveAckPipelinedResponse()
        else                        slaveAckResponse()
      }
    }
  }
}
