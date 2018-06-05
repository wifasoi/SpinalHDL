package spinal.lib.uvm.sim

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import scala.collection._

trait Transaction

trait Cycle[T <: Transaction]{
  val transactions : immutable.Seq[T]
}

trait Driver[T <: Transaction, B <: Bundle]{
  val bus : B
  val clockdomain : ClockDomain
  def drive(transactions: T) : Unit@suspendable

  // def event(bus: B): Boolean
  // val onBusEventCallbacks : mutable.ListBuffer[(B) => Unit@suspendable] = mutable.ListBuffer[(B) => Unit@suspendable]()
  // def addCallback(callback: (B) => Unit@suspendable) : Unit = onBusEventCallbacks += callback
  // val mapEvent = Map[(B) => Boolean, () => Unit@suspendable]()
  // def onEventDo
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

  val onTriggerCallbacks : mutable.ListBuffer[(T) => Unit] = mutable.ListBuffer[(T) => Unit]()
  def addCallback(callback: (T) => Unit): Unit = onTriggerCallbacks += callback

  doSampling()
}

trait Agent[T <: Transaction, B <: Bundle]{
  val driver : Driver[T,B]
  val sequencer : Sequencer[T]
  val monitor : Monitor[T,B]

//monitor
  def addCallback(callback: (T)=> Unit): Unit = monitor.addCallback(callback)
//sequencer
  def addBuilder(callback: => T) = sequencer.addBuilder(callback)
  //def start() = sequencer.start()
  //def next() : Unit@suspendable = sequencer.next()
  def create(number: Int = 1) = sequencer.create(number)

  def build(): Unit = {
    sequencer.addCallback{ cycle =>
      driver.drive(cycle)
    }
  }
}

