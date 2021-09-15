package com.luteapp.bettercounter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import com.luteapp.bettercounter.boilerplate.DragAndSwipeTouchHelper
import com.luteapp.bettercounter.databinding.FragmentEntryBinding
import com.luteapp.bettercounter.persistence.CounterSummary
import com.luteapp.bettercounter.persistence.Interval
import com.luteapp.bettercounter.persistence.Tutorial
import com.luteapp.bettercounter.ui.CounterSettingsDialogBuilder
import com.luteapp.bettercounter.ui.EntryViewHolder
import java.util.*

class EntryListViewAdapter(
    private var activity: AppCompatActivity,
    private var viewModel: ViewModel
) : RecyclerView.Adapter<EntryViewHolder>(), DragAndSwipeTouchHelper.ListGesturesCallback
{
    var onItemClickListener: ((Int, CounterSummary) -> Unit)? = null
    var onItemAdded: ((Int) -> Unit)? = null

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var counters: MutableList<String> = mutableListOf()

    override fun getItemCount(): Int = counters.size

    init {
        viewModel.observeNewCounter(activity, { counterName, isUserAdded ->
            counters.add(counterName)
            activity.runOnUiThread {
                val position = counters.size - 1
                notifyItemInserted(position)
                viewModel.getCounterSummary(counterName).observe(activity) {
                    notifyItemChanged(counters.indexOf(it.name), Unit) // passing a second parameter disables the disappear+appear animation
                }
                if (isUserAdded) {
                    onItemAdded?.invoke(position)
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = FragmentEntryBinding.inflate(inflater, parent, false)
        val holder = EntryViewHolder(activity, binding, viewModel)
        binding.root.setOnClickListener {
            val counter = holder.counter
            if (counter != null) {
                onItemClickListener?.invoke(counters.indexOf(counter.name), counter)
            }
        }
        return holder
    }

    override fun onViewAttachedToWindow(holder: EntryViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (!viewModel.isTutorialShown(Tutorial.SWIPE)) {
            viewModel.setTutorialShown(Tutorial.SWIPE)
            SimpleTooltip.Builder(activity)
                .anchorView(holder.itemView)
                .text(R.string.tutorial_swipe)
                .gravity(Gravity.BOTTOM)
                .animated(true)
                .modal(true)
                .build()
                .show()
        } else if (counters.size > 1 && !viewModel.isTutorialShown(Tutorial.DRAG)) {
            viewModel.setTutorialShown(Tutorial.DRAG)
            SimpleTooltip.Builder(activity)
                .anchorView(holder.binding.countText)
                .text(R.string.tutorial_drag)
                .gravity(Gravity.BOTTOM)
                .animated(true)
                .modal(true)
                .build()
                .show()
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val counter = viewModel.getCounterSummary(counters[position]).value
        if (counter != null) {
            holder.onBind(counter)
        }
    }

    fun removeItem(position: Int) {
        val name = counters.removeAt(position)
        notifyItemRemoved(position)
        viewModel.deleteCounter(name)
        viewModel.saveCounterOrder(counters)
    }

    fun editCounter(position: Int, newName: String, newInterval : Interval, newColor : Int) {
        val oldName = counters[position]
        if (oldName != newName) {
            counters[position] = newName
            viewModel.saveCounterOrder(counters)
            viewModel.editCounter(oldName, newName, newInterval, newColor)
        } else {
            viewModel.editCounterSameName(newName, newInterval, newColor)
        }
        // The counter updates async, when the update is done we will get notified through the counter's livedata
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(counters, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(counters, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        // Do not store individual movements, store the final result in `onDragEnd`
    }

    override fun onDragStart(viewHolder: RecyclerView.ViewHolder?) {
        //TODO: haptic feedback?
    }

    override fun onDragEnd(viewHolder: RecyclerView.ViewHolder?) {
        viewModel.saveCounterOrder(counters)
    }

    override fun onSwipe(position: Int) {
        val name = counters[position]
        val interval = viewModel.getCounterInterval(name)
        val color = viewModel.getCounterColor(name)
        CounterSettingsDialogBuilder(activity, viewModel)
            .forExistingCounter(name, interval, color)
            .setOnSaveListener { newName, newInterval, newColor ->
                editCounter(position, newName, newInterval, newColor)
            }
            .setOnDismissListener {
                notifyItemChanged(position) // moves the swiped item back to its place.
            }
            .setOnDeleteListener { _, _ ->
                removeItem(position)
            }
            .show()
    }

}
