// package spinal.lib.uvm.sim

// import spinal.sim._
// import spinal.core._
// import spinal.core.sim._

// // trait Transaction

// // trait Analysis{
// //   def write(t: Transaction)
// // }

// // trait Blocking{
// //   val that
// //   def put(t: Transaction) = that.get()
// //   def get(): Transaction = {
// //   }
// //   def peek(): Transaction
// //   def transport(t: Transaction): Transaction
// //   def b_connect(that: Blocking){
// //     this.put(that.get())
// //     that.put(this.get())
// //   }
// // }

// // trait NonBlocking{
// //   def try_put(t: Transaction): Unit
// //   def can_put(): Boolean
// //   def try_get(t: Transaction): Unit
// //   def can_get(): Boolean
// //   def nb_transport(t: Transaction): Transaction
// //   def nb_connect(that: Blocking){
// //     this.try_put(that.try_get())
// //     that.try_put(this.try_get())
// //     this.can_put = that.can_get()
// //     this.can_get = that.can_put()
// //   }
// // }

// // trait Port extends Analysis with blocking with NonBlocking{
// //   def connect(export)
// // }

// // trait ExPort{}

// // trait tlm(){
// //   def get()
// //   def put()
// //   def peek()
// // }

// // trait uvm_tlm2(){

// // }

// // trait transport with blocking{}
// // trait transport with not_blocking{}

// // trait transport{}
// // ///////////////////////////////////////////
// // trait Blocking[T <: Transaction]{
// //   def put
// //   def get
// //   def peek
// //   def b_transport
// // } 

// // trait NonBlocking[T <: Transaction]{
// //   def try_put
// //   def try_get
// //   def try_peek
// //   def can_put
// //   def can_get
// //   def can_peek
// //   def nb_transport
// // } 
// // trait Blocking[T <: Transaction] {
// //   val that : +Blocking
// //   val transactions = immutable.Queue[T]()
// //   private def canPut : Boolean = this.transactions.size < queueSize
// //   private def canGet : Boolean = that.transactions.nonEmpty

// //   def put(t: T): Unit ={
// //     while(!canPut) //TODO:waintUntil?
// //     this.transactions.enqueue(t)
// //   }

// //   def get(): T = {
// //     while(!canGet) //TODO:waintUntil?
// //     that.transactions.dequeue()
// //   }

// //   def peek(): T = {
// //     while(!canGet) //TODO:waintUntil?
// //     that.transactions.last
// //   }

// //   def b_transport(t: T): T ={
// //     put(t)
// //     get()
// //   }
// // }

// // trait NonBlocking[T <: Transaction]{
// //   val that : +NonBlocking
// //   val transactions = immutable.Queue[T]()
// //   def try_put(t: T) : Boolean = {
// //     if(can_put){
// //       this.transactions.enqueue(t)
// //       True
// //     } else {
// //       False
// //     }
// //   }
// //   def try_get(): Option[T] = if(can_get) Some(that.transactions.dequeue()) else None
// //   def try_peek(): Option[T] = if(can_get) Some(that.transactions.last) else None
// //   def can_put = this.transactions.size < queueSize
// //   def can_get = that.transactions.nonEmpty
// //   def can_peek = can_get()
// //   def nb_transport(t: T): Option[T] = if(try_put(t)) try_get() else None
// // }

// // class TLM[T <: Transaction](val that: TLM, val fifoSize = 1) extends Blocking with NonBlocking = {
// //   val transactions = immutable.Queue[T]()
// //   ///Blocking
// //   def put(t: T): Unit ={
// //     while(!can_put) //TODO:waintUntil?
// //     this.transactions.enqueue(t)
// //   }

// //   def get(): T = {
// //     while(!can_get) //TODO:waintUntil?
// //     that.transactions.dequeue()
// //   }

// //   def peek(): T = {
// //     while(!can_get) //TODO:waintUntil?
// //     that.transactions.last
// //   }

// //   def b_transport(t: T): T ={
// //     put(t)
// //     get()
// //   }
// //   ///// NON Blocking
// //   def try_put(t: T) : Boolean = {
// //     if(can_put){
// //       this.transactions.enqueue(t)
// //       True
// //     } else {
// //       False
// //     }
// //   }
// //   def try_get(): Option[T] = if(can_get) Some(that.transactions.dequeue()) else None
// //   def try_peek(): Option[T] = if(can_get) Some(that.transactions.last) else None
// //   def can_put = this.transactions.size < fifoSize
// //   def can_get = that.transactions.nonEmpty
// //   def can_peek = can_get()
// //   def nb_transport(t: T): Option[T] = if(try_put(t)) try_get() else None
// // }

// // trait generic_payload{
// //   val address
// //   val command
// //   val response_status
// //   val byte_enable
// //   val streaming_widht
// // }
// // ///////////////////////////////////////////////////////////////////////////////
// // trait uvm_b_put{
// //   def put
// // }

// // trait uvm_nb_put{
// //   def try_put
// //   def can_put
// // }

// // trait b_get{
// //   def get
// // }

// // trait nb_get{
// //   def try_get
// //   def can_get
// // }

// // trait b_peek{
// //   def peek
// // }

// // trait nb_peek{
// //   def try_peek
// //   def can_peek
// // }

// // trait uvm_put extends uvm_b_put with uvm_nb_put
// // trait uvm_get extends uvm_b_get with uvm_nb_get
// // trait uvm_peek extends uvm_b_peek with uvm_nb_peek
// // trait uvm_b_transport extends uvm_b_put with uvm_b_get{
// //   def transport(){
// //     put()
// //     get()
// //   }
// // }
// //////////////////////////////////////////////////////////////////////////////
// trait Transaction
// class NQTLM[T <: Transaction](val that, val queueSize = 1){
//   val transactions = immutable.Queue[T]()
//   private def canPut : Boolean = this.transactions.size < queueSize
//   private def canGet : Boolean = that.transactions.nonEmpty

//   def put(t: T): Unit ={
//     while(!canPut) //TODO:waintUntil?
//     this.transactions.enqueue(t)
//   }

//   def get(): T = {
//     while(!canGet) //TODO:waintUntil?
//     that.transactions.dequeue()
//   }

//   def peek(): T = {
//     while(!canGet) //TODO:waintUntil?
//     that.transactions.last
//   }

//   def transport(t: T): T ={
//     put(t)
//     get()
//   }
// }