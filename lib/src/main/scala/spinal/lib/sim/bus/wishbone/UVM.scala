package spinal.lib.uvm.sim

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection._

trait Transaction

trait Cycle[T <: Transaction] extends Transaction{
  val transactions : immutable.Seq[T]
}

trait Driver[T <: Transaction, B <: Bundle]{
  val bus : B
  val clockdomain : ClockDomain
  def drive(transactions: T) : Unit@suspendable
}

trait Sequencer[T <: Transaction]{
  val queue : mutable.Queue[T] = mutable.Queue[T]()

  val onNextCallbacks : mutable.ListBuffer[ (T) => Unit@suspendable ] = mutable.ListBuffer[(T) => Unit@suspendable]()
  val builders : mutable.ListBuffer[() => T] = mutable.ListBuffer[() => T]()
  def addBuilder(callback: => T): Unit = builders += (() => callback)

  def addCallback(callback: (T) => Unit@suspendable): Unit = onNextCallbacks += callback
  def start() : Unit@suspendable = next(queue.size)
  def next(number: Int = 1) : Unit@suspendable = {
    (1 to number).suspendable.foreach{ x =>
      val transaction = queue.dequeue()
      onNextCallbacks.suspendable.foreach( _(transaction) )
    }
  }

  def addToQueue(transaction: T) = queue.enqueue(transaction)

  def create(num: Int = 1): Unit = builders.foreach{ builder =>
    for( i <- 1 to num) addToQueue(builder())
  }
}

trait Monitor[T <: Transaction, B <: Bundle]{
  val bus : B
  val clockdomain : ClockDomain

  def doSampling()

  val onTriggerCallbacks : mutable.ListBuffer[(T) => Unit@suspendable] = mutable.ListBuffer[(T) => Unit@suspendable]()
  def addCallback(callback: (T) => Unit@suspendable): Unit = onTriggerCallbacks += callback

  doSampling()
}

trait Agent[T <: Transaction, C <: Cycle[T], B <: Bundle] {
  val driver : Driver[C, B]
  val sequencer : Sequencer[C]
  val monitor : Monitor[T,B]

//monitor
  def addCallback(callback: (T)=> Unit@suspendable): Unit@suspendable = monitor.addCallback(callback)
//sequencer
  def addBuilder(callback: => C) = sequencer.addBuilder(callback)
  //def start() = sequencer.start()
  //def next() : Unit@suspendable = sequencer.next()
  def create(number: Int = 1) = sequencer.create(number)

  def build(): Unit = {
    sequencer.addCallback{ cycle =>
      driver.drive(cycle)
    }
  }
}

/////////////////////////
class BasicPC[T]{
  val callbacks = mutable.ListBuffer[(T) => Unit@suspendable]()
  def send(packet: T): Unit@suspendable = {
    callbacks.suspendable.foreach{_(packet)}
  }
  def receive(): T =
  def connect(that: (T) => Unit@suspendable) = callbacks += that
}

class bidirectional[T]{

}