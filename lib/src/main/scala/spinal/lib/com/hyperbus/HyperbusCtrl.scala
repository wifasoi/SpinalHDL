package spinal.lib.com.hyperbus

import spinal.core._
import spinal.lib.IMasterSlave

case class HyperbusConfig(
    csWidth: Int = 1
)


case class HyperBus(csWidth: Int = 1) extends Bundle with IMasterSlave {
    val CSn = Bits(csWidth bits) // chip select, active low
    val CK = Bool                       // clock
    val DQ = Analog(Bits(8 bits))       // bidirectional data
    val RWDS = Analog(Bool)             //ReadWriteDatastrobe

    val RESETn = Bool.allowPruning()    // Reset
    val RSTOn = Bool.allowPruning()     // POR signal
    val INTn = Bool.allowPruning()      // interrupt

    override def asMaster(): Unit = {
        out(CSn,CK,RESETn)
        inout(DQ, RWDS)
    } 

}