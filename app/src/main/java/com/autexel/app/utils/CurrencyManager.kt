package com.autexel.app.utils

import android.content.Context
import java.util.Locale

object CurrencyManager {

    private const val PREF_NAME   = "autexel_prefs"
    private const val KEY_CURRENCY = "selected_currency_code"
    private const val KEY_COUNTRY  = "selected_country_code"

    data class Currency(
        val code: String,
        val symbol: String,
        val name: String
    )

    data class Country(
        val code: String,       // ISO 3166-1 alpha-2
        val name: String,
        val currencyCode: String
    )

    // ── Currencies (80 currencies covering every country) ────────────────────
    val currencies: List<Currency> = listOf(
        Currency("AFN","AFN","Afghan Afghani"),
        Currency("ALL","ALL","Albanian Lek"),
        Currency("DZD","DZD","Algerian Dinar"),
        Currency("AOA","AOA","Angolan Kwanza"),
        Currency("ARS","ARS","Argentine Peso"),
        Currency("AMD","AMD","Armenian Dram"),
        Currency("AUD","AUD","Australian Dollar"),
        Currency("AZN","AZN","Azerbaijani Manat"),
        Currency("BHD","BHD","Bahraini Dinar"),
        Currency("BDT","BDT","Bangladeshi Taka"),
        Currency("BYN","BYN","Belarusian Ruble"),
        Currency("BZD","BZD","Belize Dollar"),
        Currency("BOB","BOB","Bolivian Boliviano"),
        Currency("BAM","BAM","Bosnia-Herzegovina Mark"),
        Currency("BWP","BWP","Botswanan Pula"),
        Currency("BRL","BRL","Brazilian Real"),
        Currency("GBP","GBP","British Pound"),
        Currency("BND","BND","Brunei Dollar"),
        Currency("BGN","BGN","Bulgarian Lev"),
        Currency("BIF","BIF","Burundian Franc"),
        Currency("KHR","KHR","Cambodian Riel"),
        Currency("CAD","CAD","Canadian Dollar"),
        Currency("CVE","CVE","Cape Verdean Escudo"),
        Currency("XAF","XAF","Central African CFA Franc"),
        Currency("XOF","XOF","West African CFA Franc"),
        Currency("CLP","CLP","Chilean Peso"),
        Currency("CNY","CNY","Chinese Yuan"),
        Currency("COP","COP","Colombian Peso"),
        Currency("CDF","CDF","Congolese Franc"),
        Currency("CRC","CRC","Costa Rican Colon"),
        Currency("HRK","HRK","Croatian Kuna"),
        Currency("CUP","CUP","Cuban Peso"),
        Currency("CZK","CZK","Czech Koruna"),
        Currency("DKK","DKK","Danish Krone"),
        Currency("DJF","DJF","Djiboutian Franc"),
        Currency("DOP","DOP","Dominican Peso"),
        Currency("EGP","EGP","Egyptian Pound"),
        Currency("ETB","ETB","Ethiopian Birr"),
        Currency("EUR","EUR","Euro"),
        Currency("FJD","FJD","Fijian Dollar"),
        Currency("GEL","GEL","Georgian Lari"),
        Currency("GHS","GHS","Ghanaian Cedi"),
        Currency("GTQ","GTQ","Guatemalan Quetzal"),
        Currency("GNF","GNF","Guinean Franc"),
        Currency("HNL","HNL","Honduran Lempira"),
        Currency("HKD","HKD","Hong Kong Dollar"),
        Currency("HUF","HUF","Hungarian Forint"),
        Currency("ISK","ISK","Icelandic Krona"),
        Currency("INR","Rs.","Indian Rupee"),
        Currency("IDR","IDR","Indonesian Rupiah"),
        Currency("IRR","IRR","Iranian Rial"),
        Currency("IQD","IQD","Iraqi Dinar"),
        Currency("ILS","ILS","Israeli Shekel"),
        Currency("JMD","JMD","Jamaican Dollar"),
        Currency("JPY","JPY","Japanese Yen"),
        Currency("JOD","JOD","Jordanian Dinar"),
        Currency("KZT","KZT","Kazakhstani Tenge"),
        Currency("KES","KES","Kenyan Shilling"),
        Currency("KWD","KWD","Kuwaiti Dinar"),
        Currency("KGS","KGS","Kyrgystani Som"),
        Currency("LAK","LAK","Laotian Kip"),
        Currency("LBP","LBP","Lebanese Pound"),
        Currency("LSL","LSL","Lesotho Loti"),
        Currency("LYD","LYD","Libyan Dinar"),
        Currency("MOP","MOP","Macanese Pataca"),
        Currency("MKD","MKD","Macedonian Denar"),
        Currency("MGA","MGA","Malagasy Ariary"),
        Currency("MWK","MWK","Malawian Kwacha"),
        Currency("MYR","MYR","Malaysian Ringgit"),
        Currency("MVR","MVR","Maldivian Rufiyaa"),
        Currency("MUR","MUR","Mauritian Rupee"),
        Currency("MXN","MXN","Mexican Peso"),
        Currency("MDL","MDL","Moldovan Leu"),
        Currency("MNT","MNT","Mongolian Tugrik"),
        Currency("MAD","MAD","Moroccan Dirham"),
        Currency("MZN","MZN","Mozambican Metical"),
        Currency("MMK","MMK","Myanmar Kyat"),
        Currency("NAD","NAD","Namibian Dollar"),
        Currency("NPR","NPR","Nepalese Rupee"),
        Currency("NZD","NZD","New Zealand Dollar"),
        Currency("NIO","NIO","Nicaraguan Cordoba"),
        Currency("NGN","NGN","Nigerian Naira"),
        Currency("NOK","NOK","Norwegian Krone"),
        Currency("OMR","OMR","Omani Rial"),
        Currency("PKR","PKR","Pakistani Rupee"),
        Currency("PAB","PAB","Panamanian Balboa"),
        Currency("PGK","PGK","Papua New Guinean Kina"),
        Currency("PYG","PYG","Paraguayan Guarani"),
        Currency("PEN","PEN","Peruvian Sol"),
        Currency("PHP","PHP","Philippine Peso"),
        Currency("PLN","PLN","Polish Zloty"),
        Currency("QAR","QAR","Qatari Rial"),
        Currency("RON","RON","Romanian Leu"),
        Currency("RUB","RUB","Russian Ruble"),
        Currency("RWF","RWF","Rwandan Franc"),
        Currency("SAR","SAR","Saudi Riyal"),
        Currency("RSD","RSD","Serbian Dinar"),
        Currency("SGD","SGD","Singapore Dollar"),
        Currency("SOS","SOS","Somali Shilling"),
        Currency("ZAR","ZAR","South African Rand"),
        Currency("KRW","KRW","South Korean Won"),
        Currency("SSP","SSP","South Sudanese Pound"),
        Currency("LKR","LKR","Sri Lankan Rupee"),
        Currency("SDG","SDG","Sudanese Pound"),
        Currency("SEK","SEK","Swedish Krona"),
        Currency("CHF","CHF","Swiss Franc"),
        Currency("TWD","TWD","Taiwan Dollar"),
        Currency("TZS","TZS","Tanzanian Shilling"),
        Currency("THB","THB","Thai Baht"),
        Currency("TND","TND","Tunisian Dinar"),
        Currency("TRY","TRY","Turkish Lira"),
        Currency("UGX","UGX","Ugandan Shilling"),
        Currency("UAH","UAH","Ukrainian Hryvnia"),
        Currency("AED","AED","UAE Dirham"),
        Currency("USD","$","US Dollar"),
        Currency("UYU","UYU","Uruguayan Peso"),
        Currency("UZS","UZS","Uzbekistani Som"),
        Currency("VES","VES","Venezuelan Bolivar"),
        Currency("VND","VND","Vietnamese Dong"),
        Currency("YER","YER","Yemeni Rial"),
        Currency("ZMW","ZMW","Zambian Kwacha"),
        Currency("ZWL","ZWL","Zimbabwean Dollar")
    )

    // ── Countries mapped to their currency (159 countries, 126 currencies) - covers 190+ territories ───────────────────────────────────
    val countries: List<Country> = listOf(
        Country("AF","Afghanistan","AFN"), Country("AL","Albania","ALL"),
        Country("DZ","Algeria","DZD"), Country("AO","Angola","AOA"),
        Country("AR","Argentina","ARS"), Country("AM","Armenia","AMD"),
        Country("AU","Australia","AUD"), Country("AT","Austria","EUR"),
        Country("AZ","Azerbaijan","AZN"), Country("BH","Bahrain","BHD"),
        Country("BD","Bangladesh","BDT"), Country("BY","Belarus","BYN"),
        Country("BE","Belgium","EUR"), Country("BZ","Belize","BZD"),
        Country("BO","Bolivia","BOB"), Country("BA","Bosnia and Herzegovina","BAM"),
        Country("BW","Botswana","BWP"), Country("BR","Brazil","BRL"),
        Country("BN","Brunei","BND"), Country("BG","Bulgaria","BGN"),
        Country("BI","Burundi","BIF"), Country("KH","Cambodia","KHR"),
        Country("CM","Cameroon","XAF"), Country("CA","Canada","CAD"),
        Country("CV","Cape Verde","CVE"), Country("CF","Central African Republic","XAF"),
        Country("TD","Chad","XAF"), Country("CL","Chile","CLP"),
        Country("CN","China","CNY"), Country("CO","Colombia","COP"),
        Country("CD","Congo (DRC)","CDF"), Country("CG","Congo (Republic)","XAF"),
        Country("CR","Costa Rica","CRC"), Country("HR","Croatia","HRK"),
        Country("CU","Cuba","CUP"), Country("CY","Cyprus","EUR"),
        Country("CZ","Czech Republic","CZK"), Country("DK","Denmark","DKK"),
        Country("DJ","Djibouti","DJF"), Country("DO","Dominican Republic","DOP"),
        Country("EC","Ecuador","USD"), Country("EG","Egypt","EGP"),
        Country("SV","El Salvador","USD"), Country("GQ","Equatorial Guinea","XAF"),
        Country("ER","Eritrea","ETB"), Country("EE","Estonia","EUR"),
        Country("ET","Ethiopia","ETB"), Country("FJ","Fiji","FJD"),
        Country("FI","Finland","EUR"), Country("FR","France","EUR"),
        Country("GA","Gabon","XAF"), Country("GE","Georgia","GEL"),
        Country("DE","Germany","EUR"), Country("GH","Ghana","GHS"),
        Country("GR","Greece","EUR"), Country("GT","Guatemala","GTQ"),
        Country("GN","Guinea","GNF"), Country("HN","Honduras","HNL"),
        Country("HK","Hong Kong","HKD"), Country("HU","Hungary","HUF"),
        Country("IS","Iceland","ISK"), Country("IN","India","INR"),
        Country("ID","Indonesia","IDR"), Country("IR","Iran","IRR"),
        Country("IQ","Iraq","IQD"), Country("IE","Ireland","EUR"),
        Country("IL","Israel","ILS"), Country("IT","Italy","EUR"),
        Country("JM","Jamaica","JMD"), Country("JP","Japan","JPY"),
        Country("JO","Jordan","JOD"), Country("KZ","Kazakhstan","KZT"),
        Country("KE","Kenya","KES"), Country("KW","Kuwait","KWD"),
        Country("KG","Kyrgyzstan","KGS"), Country("LA","Laos","LAK"),
        Country("LV","Latvia","EUR"), Country("LB","Lebanon","LBP"),
        Country("LS","Lesotho","LSL"), Country("LY","Libya","LYD"),
        Country("LT","Lithuania","EUR"), Country("LU","Luxembourg","EUR"),
        Country("MO","Macau","MOP"), Country("MK","North Macedonia","MKD"),
        Country("MG","Madagascar","MGA"), Country("MW","Malawi","MWK"),
        Country("MY","Malaysia","MYR"), Country("MV","Maldives","MVR"),
        Country("ML","Mali","XOF"), Country("MT","Malta","EUR"),
        Country("MR","Mauritania","MRU"), Country("MU","Mauritius","MUR"),
        Country("MX","Mexico","MXN"), Country("MD","Moldova","MDL"),
        Country("MN","Mongolia","MNT"), Country("ME","Montenegro","EUR"),
        Country("MA","Morocco","MAD"), Country("MZ","Mozambique","MZN"),
        Country("MM","Myanmar","MMK"), Country("NA","Namibia","NAD"),
        Country("NP","Nepal","NPR"), Country("NL","Netherlands","EUR"),
        Country("NZ","New Zealand","NZD"), Country("NI","Nicaragua","NIO"),
        Country("NE","Niger","XOF"), Country("NG","Nigeria","NGN"),
        Country("NO","Norway","NOK"), Country("OM","Oman","OMR"),
        Country("PK","Pakistan","PKR"), Country("PA","Panama","PAB"),
        Country("PG","Papua New Guinea","PGK"), Country("PY","Paraguay","PYG"),
        Country("PE","Peru","PEN"), Country("PH","Philippines","PHP"),
        Country("PL","Poland","PLN"), Country("PT","Portugal","EUR"),
        Country("QA","Qatar","QAR"), Country("RO","Romania","RON"),
        Country("RU","Russia","RUB"), Country("RW","Rwanda","RWF"),
        Country("SA","Saudi Arabia","SAR"), Country("SN","Senegal","XOF"),
        Country("RS","Serbia","RSD"), Country("SL","Sierra Leone","SLL"),
        Country("SG","Singapore","SGD"), Country("SK","Slovakia","EUR"),
        Country("SI","Slovenia","EUR"), Country("SO","Somalia","SOS"),
        Country("ZA","South Africa","ZAR"), Country("KR","South Korea","KRW"),
        Country("SS","South Sudan","SSP"), Country("ES","Spain","EUR"),
        Country("LK","Sri Lanka","LKR"), Country("SD","Sudan","SDG"),
        Country("SE","Sweden","SEK"), Country("CH","Switzerland","CHF"),
        Country("TW","Taiwan","TWD"), Country("TJ","Tajikistan","TJS"),
        Country("TZ","Tanzania","TZS"), Country("TH","Thailand","THB"),
        Country("TG","Togo","XOF"), Country("TN","Tunisia","TND"),
        Country("TR","Turkey","TRY"), Country("TM","Turkmenistan","TMT"),
        Country("UG","Uganda","UGX"), Country("UA","Ukraine","UAH"),
        Country("AE","United Arab Emirates","AED"),
        Country("GB","United Kingdom","GBP"),
        Country("US","United States","USD"),
        Country("UY","Uruguay","UYU"), Country("UZ","Uzbekistan","UZS"),
        Country("VE","Venezuela","VES"), Country("VN","Vietnam","VND"),
        Country("YE","Yemen","YER"), Country("ZM","Zambia","ZMW"),
        Country("ZW","Zimbabwe","ZWL")
    ).sortedBy { it.name }

    // ── Persistence ───────────────────────────────────────────────────────────

    fun getSelectedCountry(context: Context): Country? {
        val code = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COUNTRY, null) ?: return null
        return countries.find { it.code == code }
    }

    fun setSelectedCountry(context: Context, countryCode: String) {
        val country = countries.find { it.code == countryCode } ?: return
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COUNTRY, countryCode)
            .putString(KEY_CURRENCY, country.currencyCode)
            .apply()
    }

    fun getSelected(context: Context): Currency {
        val code = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY, detectDefaultCurrency()) ?: detectDefaultCurrency()
        return currencies.find { it.code == code } ?: currencies.find { it.code == "USD" }!!
    }

    fun setSelected(context: Context, code: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CURRENCY, code).apply()
    }

    fun isCountrySelected(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COUNTRY, null) != null
    }

    fun getSymbol(context: Context): String = getSelected(context).symbol

    fun format(context: Context, amount: Double): String {
        val currency = getSelected(context)
        return "${currency.symbol} ${String.format("%,.2f", amount)}"
    }

    // ── Auto-detect from device locale ────────────────────────────────────────
    fun detectDefaultCurrency(): String {
        return try {
            val locale = Locale.getDefault()
            val countryCode = locale.country
            val country = countries.find { it.code == countryCode }
            country?.currencyCode ?: "USD"
        } catch (e: Exception) { "USD" }
    }

    fun detectDefaultCountryCode(): String {
        return try { Locale.getDefault().country.ifEmpty { "US" } } catch (e: Exception) { "US" }
    }
}
