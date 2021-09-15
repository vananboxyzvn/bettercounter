package com.luteapp.bettercounter.ui

import android.content.Context
import android.view.Gravity
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import com.luteapp.bettercounter.R
import com.luteapp.bettercounter.ViewModel
import com.luteapp.bettercounter.databinding.FragmentEntryBinding
import com.luteapp.bettercounter.persistence.CounterSummary
import com.luteapp.bettercounter.persistence.Tutorial
import java.util.*


class EntryViewHolder(
    private val context : Context,
    val binding : FragmentEntryBinding,
    private var viewModel: ViewModel
) : RecyclerView.ViewHolder(binding.root) {

    var counter : CounterSummary? = null

    fun onBind(counter: CounterSummary) {
        this.counter = counter
        binding.root.setBackgroundColor(counter.color)
        binding.increaseButton.setOnClickListener {
            viewModel.incrementCounter(counter.name)
            if (!viewModel.isTutorialShown(Tutorial.PICKDATE)) {
                viewModel.setTutorialShown(Tutorial.PICKDATE)
                SimpleTooltip.Builder(context)
                    .anchorView(binding.increaseButton)
                    .text(R.string.tutorial_pickdate)
                    .gravity(Gravity.BOTTOM)
                    .animated(true)
                    .modal(true)
                    .build()
                    .show()
            }
        }
        binding.increaseButton.setOnLongClickListener {
            showDateTimePicker(context, Calendar.getInstance()) { pickedDateTime ->
                viewModel.incrementCounter(counter.name, pickedDateTime.time)
            }
            true
        }
        binding.undoButton.setOnClickListener { viewModel.decrementCounter(counter.name) }
        binding.nameText.text = counter.name
        binding.countText.text = counter.count.toString()
        val mostRecentDate = counter.mostRecent
        if (mostRecentDate != null) {
            binding.timestampText.referenceTime = mostRecentDate.time
            binding.undoButton.isEnabled = true
        } else {
            binding.timestampText.referenceTime = -1L
            binding.undoButton.isEnabled = false
        }
    }
}
