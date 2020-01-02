package spinal.lib.memory.hyperram
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.com.hyperbus._


case class HyperRamCmd(csWidth: Int = 1) extends Bundle{
    val rw = Bool
    val adressSpace = Bool
    val burstType = Bool
    val address = Bits(32 bits)

    val data = Bits(8 bits)
    val CSn = Bits(csWidth bits)

    def cmdSlice: Vec[Bits] = {
        val pack = Vec(rw
    }
}

case class HyperRamRsp() extends Bundle{
    val data = Bits(8 bits)
    val error = Bool
}

case class HyperRamConfig(
    val initialLatency: Int,
    val csWidth: Int = 1

)

class HyperRamCtrl(device: HyperRamConfig) extends Component {
    val io = {
        val cmd = slave(Stream(HyperRamCmd(device.csWidth)))
        val rsp = master(Stream(HyperRamRsp()))
        val hyperbus = master(HyperBus(device.csWidth))
    }

    io.hyperbus.CSn.setAll() // reste CS
    var data2send = 

    val sendFSM = new StateMachine{
        val wait = new State with EntryPoint{
            whenIsActive{
                when(io.cmd.valid){
                    io.hyperbus.CSn := io.cmd.CSn
                    goto(sendCMD)
                }

            }
        }
        val sendCMD = new State{

        }
        val sendDATA = new State{

        }
        val receiveDATA = new State{

        }
    }
    val sendCD = new SlowArea(4){

        when(cmd.ready)

    }
}