package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.HourlyForecastItemBinding

class HourlyForecastItemAdapter(
    private val context: Context,
    private var hourlyForecastItemList: List<HourlyForecastModel>
) : RecyclerView.Adapter<HourlyForecastItemAdapter.HourlyForecastItemViewHolder>() {

    class HourlyForecastItemViewHolder(private val binding: HourlyForecastItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HourlyForecastModel) = with(binding) {
            hourTextView.text = item.time
            conditionImageView.setImageResource(item.currentConditionIcon)
            tempTextView.text = item.temp
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HourlyForecastItemViewHolder = HourlyForecastItemViewHolder(
        HourlyForecastItemBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
    )


    override fun getItemCount(): Int = hourlyForecastItemList.size

    override fun onBindViewHolder(holder: HourlyForecastItemViewHolder, position: Int) =
        holder.bind(hourlyForecastItemList[position])

    @SuppressLint("NotifyDataSetChanged")
    fun updateHourlyForecastList(newHourlyForecastList: List<HourlyForecastModel>) {
        hourlyForecastItemList = newHourlyForecastList
        notifyDataSetChanged()
    }
}