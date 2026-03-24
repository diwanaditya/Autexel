package com.autexel.app.ui.country

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autexel.app.R
import com.autexel.app.databinding.ActivityCountryPickerBinding
import com.autexel.app.ui.home.HomeActivity
import com.autexel.app.utils.CurrencyManager

class CountryPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountryPickerBinding
    private lateinit var adapter: CountryAdapter
    private val allCountries = CurrencyManager.countries

    companion object {
        private const val PREF = "autexel_prefs"
        private const val KEY_COUNTRY_DONE = "country_selected"

        fun shouldShow(context: android.content.Context): Boolean {
            return !context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
                .getBoolean(KEY_COUNTRY_DONE, false)
        }

        fun markDone(context: android.content.Context) {
            context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COUNTRY_DONE, true).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-select device locale country
        val detectedCode = CurrencyManager.detectDefaultCountryCode()
        val detectedCountry = allCountries.find { it.code == detectedCode }

        // Show detected country chip
        if (detectedCountry != null) {
            binding.tvDetected.visibility = View.VISIBLE
            val currency = CurrencyManager.currencies.find { it.code == detectedCountry.currencyCode }
            binding.tvDetected.text = "Detected: ${detectedCountry.name} (${currency?.symbol ?: detectedCountry.currencyCode})"
            binding.tvDetected.setOnClickListener {
                selectCountry(detectedCode)
            }
        }

        adapter = CountryAdapter(allCountries.toMutableList()) { country ->
            selectCountry(country.code)
        }

        binding.rvCountries.layoutManager = LinearLayoutManager(this)
        binding.rvCountries.adapter = adapter

        // Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(e: Editable?) {
                val query = e.toString().trim().lowercase()
                val filtered = if (query.isEmpty()) allCountries
                else allCountries.filter {
                    it.name.lowercase().contains(query) || it.code.lowercase().contains(query)
                }
                adapter.update(filtered)
            }
        })

        binding.btnSkip.setOnClickListener {
            // Use auto-detected currency without saving country preference
            val detected = CurrencyManager.detectDefaultCurrency()
            CurrencyManager.setSelected(this, detected)
            markDone(this)
            goHome()
        }
    }

    private fun selectCountry(code: String) {
        CurrencyManager.setSelectedCountry(this, code)
        markDone(this)
        val country  = CurrencyManager.countries.find { it.code == code }
        val currency = CurrencyManager.currencies.find { it.code == country?.currencyCode }
        val msg = if (currency != null)
            "Currency set to ${currency.name} (${currency.symbol})"
        else "Country saved"
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        goHome()
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class CountryAdapter(
        private var items: List<CurrencyManager.Country>,
        private val onClick: (CurrencyManager.Country) -> Unit
    ) : RecyclerView.Adapter<CountryAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvFlag: TextView   = view.findViewById(R.id.tvCountryFlag)
            val tvName: TextView   = view.findViewById(R.id.tvCountryName)
            val tvCurrency: TextView = view.findViewById(R.id.tvCountryCurrency)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_country, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val country  = items[position]
            val currency = CurrencyManager.currencies.find { it.code == country.currencyCode }
            holder.tvFlag.text     = countryCodeToFlag(country.code)
            holder.tvName.text     = country.name
            holder.tvCurrency.text = "${currency?.symbol ?: country.currencyCode}  ${currency?.name ?: country.currencyCode}"
            holder.itemView.contentDescription = "${country.name}, currency ${currency?.name ?: country.currencyCode}"
            holder.itemView.setOnClickListener { onClick(country) }
        }

        override fun getItemCount() = items.size

        fun update(newList: List<CurrencyManager.Country>) {
            items = newList
            notifyDataSetChanged()
        }

        private fun countryCodeToFlag(code: String): String {
            if (code.length != 2) return ""
            val first  = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
    }
}
