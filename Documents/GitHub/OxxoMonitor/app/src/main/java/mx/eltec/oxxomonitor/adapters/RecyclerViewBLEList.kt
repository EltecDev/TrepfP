package mx.eltec.oxxomonitor.adapters


import Utility.BLEDevices
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.eltec.oxxomonitor.R


class RecyclerViewBLEList(private var listaDevices: ArrayList<BLEDevices>) :
    RecyclerView.Adapter<RecyclerViewBLEList.BLEListViewHolder>(), View.OnClickListener {

    private var listener: View.OnClickListener? = null
    private val imageView: List<ImageView>? = null
    private val im: ImageView? = null

    //Pantalla de peticion inicial de permisos
    var sp: SharedPreferences? = null
    var esp: SharedPreferences.Editor? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEListViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.listitem_device, null, false)
        view.setOnClickListener(this)


        return BLEListViewHolder(view)
    }

    override fun onBindViewHolder(holder: BLEListViewHolder, position: Int) {
        holder.txtNameDevice.text = listaDevices[position].nombre
        holder.txtMac.text = listaDevices[position].mac
     //   holder.txtRssi.text = listaDevices[position].rssi.toString()
        // esp?.putString("rssi",listaDevices[position].rssi.toString())

    }

    override fun getItemCount(): Int {
        return listaDevices.size
    }

    fun setOnClickListener(listener: View.OnClickListener?) {
        this.listener = listener
    }
    fun clear() {
        listaDevices.clear()
        notifyDataSetChanged()
    }
    override fun onClick(v: View) {
        if (listener != null) {
            listener!!.onClick(v)
        }
    }

    inner class BLEListViewHolder(view: View?) : RecyclerView.ViewHolder(
        view!!
    ) {
        var txtNameDevice: TextView
        var txtMac: TextView
        var btnConnect: Button? = null
      //  var txtRssi: TextView

        init {
            txtNameDevice = itemView.findViewById<View>(R.id.tvdevice_name) as TextView
            txtMac = itemView.findViewById<View>(R.id.tvdevice_address) as TextView
            txtMac = itemView.findViewById<View>(R.id.tvdevice_address) as TextView
          //  txtRssi = itemView.findViewById<View>(R.id.tvdevice_rssi) as TextView
        }
    }
}