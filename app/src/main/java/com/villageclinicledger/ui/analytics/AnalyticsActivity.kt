package com.villageclinicledger.ui.analytics

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.databinding.ActivityAnalyticsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/** Displays financial analytics: total medicine charges and payments for
 * today, this week, and this month, plus a ranked list of the top 10
 * patients by outstanding balance. */
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val repository by lazy { PatientRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        loadAnalytics()
    }

    /** Fetches aggregate financial data on IO and binds it to the UI.
     * Calculates date boundaries for today, current week (starting Monday),
     * and current month to pass to the repository's sum queries. */
    private fun loadAnalytics() {
        val today = getStartOfDay(0)
        val weekStart = getWeekStart()
        val monthStart = getStartOfMonth()

        binding.topPatientsRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todayMedicine = repository.getTotalMedicineSince(today)
                val todayPayments = repository.getTotalPaymentsSince(today)
                val weekMedicine = repository.getTotalMedicineSince(weekStart)
                val weekPayments = repository.getTotalPaymentsSince(weekStart)
                val monthMedicine = repository.getTotalMedicineSince(monthStart)
                val monthPayments = repository.getTotalPaymentsSince(monthStart)

                withContext(Dispatchers.Main) {
                    binding.todayMedicine.text = "₹${"%.2f".format(todayMedicine)}"
                    binding.todayPayment.text = "₹${"%.2f".format(todayPayments)}"
                    binding.weekMedicine.text = "₹${"%.2f".format(weekMedicine)}"
                    binding.weekPayment.text = "₹${"%.2f".format(weekPayments)}"
                    binding.monthMedicine.text = "₹${"%.2f".format(monthMedicine)}"
                    binding.monthPayment.text = "₹${"%.2f".format(monthPayments)}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.todayMedicine.text = "Error"
                    binding.todayPayment.text = "Error"
                    binding.weekMedicine.text = "Error"
                    binding.weekPayment.text = "Error"
                    binding.monthMedicine.text = "Error"
                    binding.monthPayment.text = "Error"
                }
            }
        }

        repository.getTopPatientsByBalance(10).observe(this) { patients ->
            binding.topPatientsRecyclerView.adapter = TopPatientAdapter(patients)
        }
    }

    /** Returns midnight on the Monday of the current week.
     * Uses modulo arithmetic to handle the Calendar week wrapping correctly
     * for all days including Sundays. */
    private fun getWeekStart(): java.util.Date {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /** Returns the start of the day (midnight) for a given number of days
     * before today. daysAgo=0 returns midnight of the current day. */
    private fun getStartOfDay(daysAgo: Int): java.util.Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /** Returns midnight on the 1st day of the current month. */
    private fun getStartOfMonth(): java.util.Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}

/** Simple RecyclerView adapter for the top-10 patients list.
 * Uses Android's built-in simple_list_item_2 layout to show the patient
 * name (with rank) on line 1 and the formatted balance (color-coded) on line 2. */
class TopPatientAdapter(private val patients: List<Patient>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<TopPatientAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = patients[position]
        holder.nameView.text = "${position + 1}. ${patient.name}"
        holder.balanceView.text = "₹${patient.formattedBalance}"
        holder.balanceView.setTextColor(
            if (patient.currentBalance > 0) android.graphics.Color.RED
            else android.graphics.Color.GREEN
        )
    }

    override fun getItemCount() = patients.size

    class ViewHolder(view: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(android.R.id.text1)
        val balanceView: TextView = view.findViewById(android.R.id.text2)
    }
}