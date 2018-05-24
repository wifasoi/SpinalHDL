package spinal.lib.sim

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection._

trait Transaction

trait Driver[T <: Transaction, B <: Bundle]{
  val bus : B
  val clockdomain : ClockDomain
  def drive : (T) => Unit@suspendable
}

trait Sequencer[T <: Transaction]{
  val queue : mutable.Queue[T] = mutable.Queue[T]()

  val onNextCallbacks : mutable.ArrayBuffer[ (T) => Unit@suspendable ] = mutable.ArrayBuffer[(T) => Unit@suspendable]()
  val builder : () => T

  def addCallback(callback: (T) => Unit@suspendable): Unit = onNextCallbacks += callback
  def next : Unit@suspendable = {
    val transaction = queue.dequeue()
    onNextCallbacks.suspendable.foreach( _(transaction) )
  }

  def addToQueue(transaction: T) = queue.enqueue(transaction)
  def create(num: Int = 1): Unit = for( i <- 1 to num) addToQueue(builder())
}

trait Monitor[T <: Transaction, B <: Bundle]{
  val bus : B
  val clockdomain : ClockDomain
  def trigger : (B) => Boolean
  def sample : (B) => T

  val onTriggerCallbacks : mutable.ArrayBuffer[(T) => Unit@suspendable] = mutable.ArrayBuffer[(T) => Unit@suspendable]()//TODO: bus type
  def addCallback(callback: (T) => Unit@suspendable): Unit = onTriggerCallbacks += callback

  fork{
    while(true){
      clockdomain.waitSamplingWhere(trigger(bus))  //TODO: on sampling?
      val transaction : T = sample(bus)
      onTriggerCallbacks.suspendable.foreach( _(transaction) )
    }
  }
}


// case class Agent[T <: Transaction, B <: Bundle](bus: B, clockdomain: ClockDomain){
//   val driver : Driver[T,B] = new Driver[T](bus, clockdomain)
//   val sequencer : Sequencer[T] = new Sequencer[T]
//   val monitor : Monitor[T,B] = new Monitor[T](bus, clockdomain)

//   def onTransactionCallbacks  : mutable.ArrayBuffer[(T) => Unit] = mutable.ArrayBuffer[(T) => Unit]()
//   def addCallback(callback: => T): Unit = onTransactionCallbacks += callback

//   def build(): Unit ={
//     sequencer.addCallback{ transaction =>
//       driver.drive(transaction)
//     }

//     onTransactionCallbacks.map( monitor.addCallback(_) )
//   }
// }

trait Agent[T <: Transaction, B <: Bundle]{
  val driver : Driver[T,B]
  val sequencer : Sequencer[T]
  val monitor : Monitor[T,B]

  def onTransactionCallbacks : mutable.ArrayBuffer[(T) => Unit@suspendable] = mutable.ArrayBuffer[(T) => Unit@suspendable]()
  def addCallback(callback: (T)=> Unit@suspendable): Unit = onTransactionCallbacks += callback

  def build(): Unit ={
    sequencer.addCallback{ transaction =>
      driver.drive(transaction)
    }

    onTransactionCallbacks.foreach( monitor.addCallback(_) )
  }
}

