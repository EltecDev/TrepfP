import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.eltec.oxxomonitor.R


data class RecyclerResultItem(val fecha: String, val traducido: String)

class RecyclerResultAdapter(private val resultList: List<RecyclerResultItem>) :
    RecyclerView.Adapter<RecyclerResultAdapter.RecyclerResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.listitem_device, parent, false)

       // val view = LayoutInflater.from(parent.context).inflate(R.layout.listitem_device, parent, false)
        return RecyclerResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerResultViewHolder, position: Int) {
        val item = resultList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    inner class RecyclerResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha: TextView = itemView.findViewById(R.id.tvdevice_name)
        private val tvTraducido: TextView = itemView.findViewById(R.id.tvdevice_address)

        fun bind(item: RecyclerResultItem) {
            tvFecha.text = item.fecha
            tvTraducido.text = item.traducido
        }
    }
}
