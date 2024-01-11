package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.DailyForecastItemBinding

class DailyForecastItemAdapter(
    private val context: Context,
    private var dailyForecastItemList: List<DailyForecastModel>
) : RecyclerView.Adapter<DailyForecastItemAdapter.DailyForecastItemViewHolder>() {
    class DailyForecastItemViewHolder(private val binding: DailyForecastItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: DailyForecastModel) = with(binding) {
            dayTextView.text = "${item.date.takeLast(2)}.${item.date.takeLast(5).dropLast(3)}"
            conditionImageView.setImageResource(item.currentConditionIcon)
            tempTextView.text = "${item.minTemp}/${item.maxTemp}"
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastItemViewHolder =
        DailyForecastItemViewHolder(
            DailyForecastItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = dailyForecastItemList.size

    override fun onBindViewHolder(holder: DailyForecastItemViewHolder, position: Int) =
        holder.bind(dailyForecastItemList[position])

    @SuppressLint("NotifyDataSetChanged")
    fun updateDailyForecastList(newDailyForecastModel: List<DailyForecastModel>) {
        dailyForecastItemList = newDailyForecastModel
        notifyDataSetChanged()
    }
}