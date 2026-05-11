package com.evgramacharge.app.ui.hosts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.databinding.ItemMyHostBinding
import java.util.Locale

class MyHostsAdapter : ListAdapter<ChargingHost, MyHostsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMyHostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemMyHostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(host: ChargingHost) {
            binding.hostName.text = host.name
            binding.hostAddress.text = host.address
            binding.hostMeta.text = String.format(
                Locale.getDefault(),
                "%s · %.2f / kWh",
                host.connectorType,
                host.pricePerKwh,
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChargingHost>() {
            override fun areItemsTheSame(oldItem: ChargingHost, newItem: ChargingHost): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ChargingHost, newItem: ChargingHost): Boolean =
                oldItem == newItem
        }
    }
}
