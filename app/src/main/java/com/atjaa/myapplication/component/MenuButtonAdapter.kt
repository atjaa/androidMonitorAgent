import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atjaa.myapplication.databinding.ItemMenuButtonBinding

data class MenuBtn(val title: String, val iconRes: Int)

class MenuButtonAdapter(private val items: List<MenuBtn>, val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<MenuButtonAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMenuButtonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.ivIcon.setImageResource(item.iconRes)
        holder.binding.root.setOnClickListener { onClick(position) }
    }

    override fun getItemCount() = items.size
}