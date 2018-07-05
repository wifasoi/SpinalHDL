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
  def apply(bus: Wishbone, clockdomain: ClockDomain) = new WishboneStatus(bus,clockdomain)
}

class WishboneStatus(bus: Wishbone, clockdomain: ClockDomain){
  var ackCount : Int = 0
  var stbCount : Int = 0

  def isCycle   : Boolean = bus.CYC.toBoolean
  def isStrobe  : Boolean = isCycle && bus.STB.toBoolean
  def isStall   : Boolean = if(bus.config.isPipelined)  isCycle && bus.STALL.toBoolean //TODO
                            else                        false
  def isTransfer: Boolean = if(bus.config.isPipelined)  isCycle && (ackCount < stbCount)
                            else                        isCycle && bus.STB.toBoolean

  def isAck     : Boolean = if(bus.config.isPipelined)  isCycle &&  bus.ACK.toBoolean //TODO
                            else                        isCycle &&  bus.ACK.toBoolean

  def isWrite   : Boolean =                             isTransfer &&  bus.WE.toBoolean
  def isRead    : Boolean =                             isTransfer && !bus.WE.toBoolean

  fork{
    while(true){
      clockdomain.waitSampling()
      if(isCycle){
        if(isAck && !isStall){
          ackCount = ackCount + 1
        }
        if(isStrobe && !isStall){
          stbCount = stbCount + 1
        }
      } else{
        ackCount = 0
        stbCount = 0
      }
    }
  }
}

class WishboneDriver(val bus: Wishbone, val clockdomain: ClockDomain,asMaster: Boolean = true) extends Driver[WishboneCycle,Wishbone]{
  val busStatus = WishboneStatus(bus,clockdomain)

  def clearBus(): Unit = {
    /////////////////////
    // MINIMAl SIGLALS //
    /////////////////////
    if(!bus.isMasterInterface) bus.CYC      #= false
    if(!bus.isMasterInterface) bus.ADR      #= 0
    if(!bus.isMasterInterface) bus.DAT_MOSI #= 0
    if( bus.isMasterInterface) bus.DAT_MISO #= 0
    if(!bus.isMasterInterface) bus.STB      #= false
    if(!bus.isMasterInterface) bus.WE       #= false
    if( bus.isMasterInterface) bus.ACK      #= false

    ///////////////////////////
    // OPTIONAL FLOW CONTROS //
    ///////////////////////////
    if(bus.config.useSTALL &&  bus.isMasterInterface) bus.STALL  #= false
    if(bus.config.useERR   &&  bus.isMasterInterface) bus.ERR    #= false
    if(bus.config.useLOCK  && !bus.isMasterInterface) bus.LOCK   #= false
    if(bus.config.useRTY   &&  bus.isMasterInterface) bus.RTY    #= false
    if(bus.config.useSEL   && !bus.isMasterInterface) bus.SEL    #= 0
    if(bus.config.useCTI   && !bus.isMasterInterface) bus.CTI    #= 0

    //////////
    // TAGS //
    //////////
    if(bus.config.useTGA && !bus.isMasterInterface) bus.TGA      #= 0
    if(bus.config.useTGC && !bus.isMasterInterface) bus.TGC      #= 0
    if(bus.config.useBTE && !bus.isMasterInterface) bus.BTE      #= 0
    if(bus.config.useTGD &&  bus.isMasterInterface) bus.TGD_MISO #= 0
    if(bus.config.useTGD && !bus.isMasterInterface) bus.TGD_MOSI #= 0
  }

  def sendAsSlave(transaction : WishboneTransaction): Unit@suspendable = {
    waitUntil(busStatus.isTransfer)
    transaction.driveAsSlave(bus)
  }

  // def sendAsMaster(transaction : WishboneTransaction): Unit@suspendable = {
  //   transaction.driveAsMaster(bus)
  //   if(!bus.config.isPipelined) clockdomain.waitSamplingWhere(busStatus.isAck)
  //   else                        clockdomain.waitSamplingWhere(busStatus.isTransfer)
  // }

  def sendAsMaster(transaction : WishboneTransaction): Unit@suspendable = {
    transaction.driveAsMaster(bus)
    if(!bus.config.isPipelined) clockdomain.waitSamplingWhere(busStatus.isAck)
    else                        waitUntil(busStatus.isTransfer)
  }

  def sendBlockAsSlave(cycle: WishboneCycle): Unit@suspendable = {
    def sendTransaction(transaction : WishboneTransaction) = {
      sendAsSlave(transaction)
      bus.ACK #= true
      waitUntil(!busStatus.isTransfer)
      bus.ACK #= false
    }
    cycle.transactions.suspendable.foreach{ transaction =>
      sendTransaction(transaction)
    }
  }

  def sendPipelinedBlockAsSlave(cycle: WishboneCycle): Unit@suspendable = {
    cycle.transactions.suspendable.foreach{ transaction =>
      sendAsSlave(transaction)
      bus.ACK #= true
      clockdomain.waitSampling()
    }
    bus.ACK #= false
  }

  def sendBlockAsMaster(cycle: WishboneCycle): Unit@suspendable = {
    def sendTransaction(transaction: WishboneTransaction): Unit@suspendable = {
      bus.STB #= true
      sendAsMaster(transaction)
      bus.STB #= false
    }
    bus.CYC #= true
    cycle.transactions.dropRight(1).suspendable.foreach{ transaction =>
      sendTransaction(transaction)
      clockdomain.waitSampling()
    }
    sendTransaction(cycle.transactions.last)
    bus.CYC #= false
  }

  // def sendPipelinedBlockAsMaster(cycle: WishboneCycle): Unit@suspendable = {
  //   bus.CYC #= true
  //   bus.STB #= true
  //   cycle.transactions.suspendable.foreach{ transaction =>
  //     sendAsMaster(transaction)
  //   }
  //   bus.STB #= false
  //   waitUntil(!busStatus.isTransfer)
  //   bus.CYC #= false
  // }

  def sendPipelinedBlockAsMaster(cycle: WishboneCycle): Unit@suspendable = {
    bus.CYC #= true
    bus.STB #= true
    cycle.transactions.dropRight(1).suspendable.foreach{ transaction =>
      sendAsMaster(transaction)
    }
    sendAsMaster(cycle.transactions.last)
    bus.STB #= false
    waitUntil(!busStatus.isTransfer)
    bus.CYC #= false
  }

  // def drive(cycle: WishboneCycle): Unit@suspendable = {
  //   val dummy = (bus.isMasterInterface,bus.config.isPipelined) match {
  //     case (false, true ) => sendPipelinedBlockAsMaster(cycle)
  //     case (false, false) => sendBlockAsMaster(cycle)
  //     case (true , true ) => sendPipelinedBlockAsSlave(cycle)
  //     case (true , false) => sendBlockAsSlave(cycle)
  //   }
  //   clockdomain.waitSampling()
  // }

  def drive(cycle: WishboneCycle): Unit@suspendable = {
    val dummy = (bus.isMasterInterface,bus.config.isPipelined) match {
      case (false, true ) => sendPipelinedBlockAsMaster(cycle)
      case (false, false) => sendBlockAsMaster(cycle)
      case (true , true ) => sendPipelinedBlockAsSlave(cycle)
      case (true , false) => sendBlockAsSlave(cycle)
    }
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
    waitUntil(busStatus.isTransfer)
    fork{
      waitUntil(!busStatus.isTransfer)
      bus.ACK #= false
      bus.STALL #= false
    }
    while(busStatus.isTransfer){
      val ack = Random.nextBoolean
      bus.ACK #= ack
      bus.STALL #= Random.nextBoolean && !ack
      clockdomain.waitSampling()
    }
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
