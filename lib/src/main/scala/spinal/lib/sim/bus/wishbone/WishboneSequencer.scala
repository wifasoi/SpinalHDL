package spinal.lib.wishbone.sim
import spinal.lib.uvm.sim._


import spinal.sim._
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.wishbone._
import scala.collection.immutable._
import scala.util.Random

case class AddressRange(base : BigInt, size: Int){
  def inRange(address: BigInt): Boolean = (address >= base) && (address <= base + size)
  //def == (range: AddressRange): Boolean = (base == range.base) && (size == range.size)
  def mask(address: BigInt): BigInt = address - base
  def randomAddressInRange: BigInt = BigInt(Random.nextInt(size)) + base
}

object WishboneTransaction{
  // implicit def singleToCycle(transaction : WishboneTransaction): WishboneCycle = new WishboneCycle(List(transaction))
  implicit def singleToList(transaction : WishboneTransaction): Seq[WishboneTransaction] = Seq(transaction)
  implicit def singleToCycle(transaction : WishboneTransaction): WishboneCycle = WishboneCycle(transaction)

  //implicit def singleToCycle(transaction : WishboneTransaction): WishboneCycle = WishboneCycle(List(transaction))
  // implicit def singleToCycle(transaction : WishboneTransaction): WishboneCycle = List(transaction).asInstanceOf[WishboneCycle]
  //implicit def singleToCycle(transaction : WishboneTransaction): WishboneCycle = WishboneCycle(List(transaction))
  //implicit def listToCycle(transactions: Seq[WishboneTransaction]) : WishboneCycle = new WishboneCycle(transactions)
  //implicit def cycleToList(cycle: WishboneCycle) : Seq[WishboneTransaction] = cycle.cycle

  def sampleAsMaster(bus: Wishbone): WishboneTransaction = {
    val transaction = WishboneTransaction(bus.ADR.toBigInt, bus.DAT_MISO.toBigInt, bus.WE.toBoolean)
    if(bus.config.useTGA) transaction.copy(tga = bus.TGA.toBigInt)
    if(bus.config.useTGC) transaction.copy(tga = bus.TGC.toBigInt)
    if(bus.config.useTGD) transaction.copy(tga = bus.TGD_MISO.toBigInt)
    transaction
  }

  def sampleAsSlave(bus: Wishbone): WishboneTransaction = {
    val transaction = WishboneTransaction(bus.ADR.toBigInt, bus.DAT_MOSI.toBigInt, bus.WE.toBoolean)
    if(bus.config.useTGA) transaction.copy(tga = bus.TGA.toBigInt)
    if(bus.config.useTGC) transaction.copy(tgc = bus.TGC.toBigInt)
    if(bus.config.useTGD) transaction.copy(tgd = bus.TGD_MOSI.toBigInt)
    transaction
  }
}

case class WishboneTransaction( address : BigInt = 0,
                                data : BigInt = 0,
                                we : Boolean = false,
                                tga : BigInt  = 0,
                                tgc : BigInt  = 0,
                                tgd : BigInt  = 0) extends Transaction{
  //override def toString : String = "Address\tData\tTGA\tTGC\tTGD\n%d\t%d\t%d\t%d\t%d".format(address,data,tga,tgc,tgd)
  def masked(mask : BigInt) : WishboneTransaction = this.copy(address = this.address & mask)

  def driveAsMaster(bus: Wishbone): Unit = {
    bus.ADR       #= address
    bus.DAT_MOSI  #= data
    bus.WE #= we
    if(bus.config.useTGA) bus.TGA       #= tga
    if(bus.config.useTGC) bus.TGC       #= tgc
    if(bus.config.useTGD) bus.TGD_MOSI  #= tgd
  }

  def driveAsSlave(bus: Wishbone): Unit = {
    bus.DAT_MISO  #= data
    if(bus.config.useTGD) bus.TGD_MISO  #= tgd
  }

  def randomizeAddress(max : Int, min : Int = 0) : WishboneTransaction = this.copy(address = Random.nextInt(max - min) + min)
  def randomizeData(max : Int, min : Int = 0) : WishboneTransaction = this.copy(data = Random.nextInt(max - min) + min)
  def randomizeTGA(max : Int, min : Int = 0) : WishboneTransaction = this.copy(tga = Random.nextInt(max - min) + min)
  def randomizeTGC(max : Int, min : Int = 0) : WishboneTransaction = this.copy(tgc = Random.nextInt(max - min) + min)
  def randomizeTGD(max : Int, min : Int = 0) : WishboneTransaction = this.copy(tgd = Random.nextInt(max - min) + min)

  override def toString: String = List(address,data,we,tga,tgc,tgd).mkString("\t")
}

// object WishboneCycle{
//   implicit def listToCycle(list: Seq[WishboneTransaction]) : WishboneCycle = new WishboneCycle(list)
//   implicit def cycleToList(cycle: WishboneCycle) : Seq[WishboneTransaction] = cycle.cycle
// }
case class WishboneCycle(val transactions: Seq[WishboneTransaction]) extends Cycle[WishboneTransaction]{
  //override def equals(that: Any) : Boolean = true
  override def toString: String = {
    List("address","data","we","tga","tgc","tgd").mkString("\t") + "\n" +
    transactions.mkString("\n")
  }
}

//object WishboneCycle{
//    implicit def listToCycle(list: Seq[WishboneTransaction]) : WishboneCycle = new WishboneCycle(list)
//  implicit def cycleToList(cycle: WishboneCycle) : Seq[WishboneTransaction] = cycle.cycle
//}


// object WishboneCycle{
//   //implicit def listToCycle(list: immutable.Seq[WishboneTransaction]) : Cycle[WishboneTransaction] = classOf[Cycle[WishboneTransaction]].getConstructor(classOf[Cycle[WishboneTransaction]]).newInstance(list)
//   implicit def listToCycle(list: immutable.Seq[WishboneTransaction]) : WishboneCycle = new WishboneCycle(list)
//   implicit def cycleToList(cycle: WishboneCycle) : immutable.Seq[WishboneTransaction] = cycle.transactions
// }
// class WishboneCycle(val transactions : immutable.Seq[WishboneTransaction]) extends Cycle[WishboneTransaction]


// object WishboneSequencer{
//   //def apply(builder: () => Seq[WishboneTransaction]) = new WishboneSequencer(builder)
//   def apply(builder: => Seq[WishboneTransaction]) = new WishboneSequencer(builder)
//   //implicit def singleTransactionToList(transaction : WishboneTransaction): Seq[WishboneTransaction] = Seq(transaction)

// }

// class WishboneSequencer(builder: => Seq[WishboneTransaction]){
//   val transactions = new scala.collection.mutable.Queue[Seq[WishboneTransaction]]()
//   //val builder:() => WishboneTransaction


//   def nextTransaction: Seq[WishboneTransaction] = transactions.dequeue()
//   def addTransaction(transaction: Seq[WishboneTransaction]): Unit = transactions.enqueue(transaction)

//   //def transactionBuilder(builder: () => WishboneTransaction): Unit = this.builder = builder
//   def generateTransactions(number: Int = 1): Unit = for(i <- 0 to number) addTransaction(builder)

//   def isEmpty: Boolean = transactions.isEmpty
// }


// object WishboneSequencer{
//   def apply(callback: (WishboneCycle) => Unit@suspendable): WishboneSequencer ={
//     val sequencer = new WishboneSequencer()
//     sequencer.addCallback(callback)
//     sequencer
//   }
// }

object WishboneSequencer{
  def apply(callback: (WishboneCycle) => Unit@suspendable): WishboneSequencer ={
    val sequencer = new WishboneSequencer()
    sequencer.addCallback(callback)
    sequencer
  }
}

class WishboneSequencer extends Sequencer[WishboneCycle]