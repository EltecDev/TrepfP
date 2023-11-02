import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trepfp.MainActivity
import com.example.trepfp.R
data class RecyclerResultItem(val fecha: String, val traducido: String)
/*
class RecyclerResultAdapter(private val itemList: List<RecyclerResultItem>) :
    RecyclerView.Adapter<RecyclerResultAdapter.RecyclerResultViewHolder>() {
  inner  class RecyclerResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Aquí declaras las vistas que se utilizarán en cada elemento del RecyclerView
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvTraducido: TextView = view.findViewById(R.id.tv_traducido)
      fun bind(fecha: String, traducido: String) {
          tvFecha.text = fecha
          tvTraducido.text = traducido
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.loggerlist, parent, false)
        return  RecyclerResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerResultViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item.fecha, item.traducido)
    }

    override fun getItemCount(): Int {
        return 1
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        private val tvTraducido: TextView = view.findViewById(R.id.tv_traducido)

        fun bind(fecha: String, traducido: String) {
            tvFecha.text = fecha
            tvTraducido.text = traducido
        }
    }
}
*/
class RecyclerResultAdapter(private val resultList: List<RecyclerResultItem>) :
    RecyclerView.Adapter<RecyclerResultAdapter.RecyclerResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.loggerlist, parent, false)

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
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)
        private val tvTraducido: TextView = itemView.findViewById(R.id.tv_traducido)

        fun bind(item: RecyclerResultItem) {
            tvFecha.text = item.fecha
            tvTraducido.text = item.traducido
        }
    }
}
