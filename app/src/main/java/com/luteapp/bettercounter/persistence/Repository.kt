package com.luteapp.bettercounter.persistence

import android.content.Context
import android.content.SharedPreferences
import com.luteapp.bettercounter.BuildConfig
import com.luteapp.bettercounter.R
import com.luteapp.bettercounter.boilerplate.Converters
import java.util.*
import kotlin.collections.HashMap

const val alwaysShowTutorialsInDebugBuilds = true

const val COUNTERS_PREFS_KEY = "counters"
const val COUNTERS_INTERVAL_PREFS_KEY = "interval.%s"
const val COUNTERS_COLOR_PREFS_KEY = "color.%s"
const val TUTORIALS_PREFS_KEY = "tutorials"

class Repository(
    private val context: Context,
    private val entryDao: EntryDao,
    private val sharedPref : SharedPreferences
) {

    private var tutorials: MutableSet<String>
    private var counters : List<String>
    private var counterCache = HashMap<String, CounterSummary>()

    init {
        val countersStr = sharedPref.getString(COUNTERS_PREFS_KEY, "[]")
        counters = Converters.stringToStringList(countersStr)
        tutorials = sharedPref.getStringSet(TUTORIALS_PREFS_KEY, setOf())!!.toMutableSet()
        if (BuildConfig.DEBUG && alwaysShowTutorialsInDebugBuilds) {
            tutorials = mutableSetOf()
        }
    }

    fun getCounterList() : List<String> {
        return counters
    }

    fun setCounterList(list : List<String>) {
        val jsonStr = Converters.stringListToString(list)
        sharedPref.edit().putString(COUNTERS_PREFS_KEY, jsonStr).apply()
        counters = list
    }

    fun setTutorialShown(id: Tutorial) {
        tutorials.add(id.name)
        sharedPref.edit().putStringSet(TUTORIALS_PREFS_KEY, tutorials).apply()
    }

    fun isTutorialShown(id: Tutorial) : Boolean {
        return tutorials.contains(id.name)
    }

    fun getCounterColor(name : String) : Int {
        val key = COUNTERS_COLOR_PREFS_KEY.format(name)
        return sharedPref.getInt(key, context.getColor(R.color.colorPrimary))
    }

    fun setCounterColor(name : String, color : Int) {
        val key = COUNTERS_COLOR_PREFS_KEY.format(name)
        sharedPref.edit().putInt(key, color).apply()
        counterCache.remove(name)
    }

    fun getCounterInterval(name : String) : Interval {
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        val str = sharedPref.getString(key, null)
        return if (str != null) {
            Interval.valueOf(str)
        } else {
            DEFAULT_INTERVAL
        }
    }

    fun removeCounterColor(name : String) {
        val key = COUNTERS_COLOR_PREFS_KEY.format(name)
        sharedPref.edit().remove(key).apply()
        counterCache.remove(name)
    }

    fun removeCounterInterval(name : String) {
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        sharedPref.edit().remove(key).apply()
        counterCache.remove(name)
    }

    fun setCounterInterval(name : String, interval : Interval) {
        val key = COUNTERS_INTERVAL_PREFS_KEY.format(name)
        sharedPref.edit().putString(key, interval.toString()).apply()
        counterCache.remove(name)
    }

    suspend fun getCounterSummary(name : String): CounterSummary {
        val interval = getCounterInterval(name)
        val color = getCounterColor(name)
        return counterCache.getOrPut(name, {
            CounterSummary(
                name = name,
                count = entryDao.getCountSince(name, interval.toDate()),
                color = color,
                mostRecent = entryDao.getMostRecent(name)?.date
            )
        })
    }

    suspend fun renameCounter(oldName : String, newName : String) {
        entryDao.renameCounter(oldName, newName)
        counterCache.remove(oldName)
    }

    suspend fun addEntry(name: String, date: Date = Calendar.getInstance().time) {
        entryDao.insert(Entry(name=name, date=date))
        counterCache.remove(name)
    }

    suspend fun removeEntry(name: String) {
        val entry = entryDao.getLastAdded(name)
        if (entry != null) {
            entryDao.delete(entry)
        }
        counterCache.remove(name)
    }

    suspend fun removeAllEntries(name: String) {
        entryDao.deleteAll(name)
        counterCache.remove(name)
    }

    suspend fun getCounterDetails(name : String): CounterDetails {
        val interval = getCounterInterval(name)
        val color = getCounterColor(name)
        val entries = entryDao.getAllEntriesInRange(name, interval.toDate(), Calendar.getInstance().time)
        return CounterDetails(
            name = name,
            interval = interval,
            color = color,
            intervalEntries = entries
        )
    }

    suspend fun getAllEntries(name : String): List<Entry> {
        return entryDao.getAllEntries(name)
    }

}
