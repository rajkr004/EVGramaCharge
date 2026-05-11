package com.evgramacharge.app.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evgramacharge.app.data.model.Booking
import com.evgramacharge.app.databinding.ItemBookingBinding
import java.text.DateFormat

class BookingsAdapter : ListAdapter<Booking, BookingsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBookingBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        private val fmt: DateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

        fun bind(item: Booking) {
            binding.bookingHost.text = item.hostName.ifBlank { item.hostId }
            binding.bookingStatus.text = item.status
            val start = fmt.format(item.startEpochMs)
            val end = fmt.format(item.endEpochMs)
            binding.bookingWindow.text = "$start — $end"
            binding.bookingEnergy.text =
                binding.root.context.getString(
                    com.evgramacharge.app.R.string.booking_energy_fmt,
                    item.estimatedEnergyKwh,
                )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean =
                oldItem == newItem
        }
    }
}
