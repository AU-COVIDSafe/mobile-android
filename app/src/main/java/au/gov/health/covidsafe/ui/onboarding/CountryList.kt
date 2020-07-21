package au.gov.health.covidsafe.ui.onboarding

import android.content.Context
import android.content.res.Configuration
import android.icu.text.AlphabeticIndex
import android.os.Build
import androidx.annotation.RequiresApi
import au.gov.health.covidsafe.R
import java.util.*

object CountryList {

    fun getCountryList(context: Context): List<CountryListItemInterface> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getSortedAndGroupedCountryListBasedOnLocaleLanguage(context)
        } else {
            getSortedAndGroupedCountryListBasedOnEnglish(context)
        }
    }

    private fun getCountries(): List<CountryListItem> {
        return listOf(
                CountryListItem(R.string.country_region_name_al, 355, R.drawable.ic_list_country_al),
                CountryListItem(R.string.country_region_name_dz, 213, R.drawable.ic_list_country_dz),
//            CountryListItem(R.string.country_region_name_ad, 376, R.drawable.ic_list_country_ad),
                CountryListItem(R.string.country_region_name_ao, 244, R.drawable.ic_list_country_ao),
                CountryListItem(R.string.country_region_name_ai, 1, R.drawable.ic_list_country_ai),
                CountryListItem(R.string.country_region_name_ag, 1, R.drawable.ic_list_country_ag),
                CountryListItem(R.string.country_region_name_ar, 54, R.drawable.ic_list_country_ar),
                CountryListItem(R.string.country_region_name_am, 374, R.drawable.ic_list_country_am),
                CountryListItem(R.string.country_region_name_aw, 297, R.drawable.ic_list_country_aw),
                CountryListItem(R.string.country_region_name_au, 61, R.drawable.ic_list_country_au),
                CountryListItem(R.string.country_region_name_at, 43, R.drawable.ic_list_country_at),
                CountryListItem(R.string.country_region_name_az, 994, R.drawable.ic_list_country_az),
                CountryListItem(R.string.country_region_name_bs, 1, R.drawable.ic_list_country_bs),
                CountryListItem(R.string.country_region_name_bh, 973, R.drawable.ic_list_country_bh),
                CountryListItem(R.string.country_region_name_bd, 880, R.drawable.ic_list_country_bd),
                CountryListItem(R.string.country_region_name_bb, 1, R.drawable.ic_list_country_bb),
                CountryListItem(R.string.country_region_name_by, 375, R.drawable.ic_list_country_by),
                CountryListItem(R.string.country_region_name_be, 32, R.drawable.ic_list_country_be),
                CountryListItem(R.string.country_region_name_bz, 501, R.drawable.ic_list_country_bz),
                CountryListItem(R.string.country_region_name_bj, 229, R.drawable.ic_list_country_bj),
                CountryListItem(R.string.country_region_name_bm, 1, R.drawable.ic_list_country_bm),
//            CountryListItem(R.string.country_region_name_bt, 975, R.drawable.ic_list_country_bt),
                CountryListItem(R.string.country_region_name_bo, 591, R.drawable.ic_list_country_bo),
                CountryListItem(R.string.country_region_name_ba, 387, R.drawable.ic_list_country_ba),
                CountryListItem(R.string.country_region_name_bw, 267, R.drawable.ic_list_country_bw),
                CountryListItem(R.string.country_region_name_br, 55, R.drawable.ic_list_country_br),
//            CountryListItem(R.string.country_region_name_bn, 673, R.drawable.ic_list_country_bn),
                CountryListItem(R.string.country_region_name_vg, 1, R.drawable.ic_list_country_vg),
                CountryListItem(R.string.country_region_name_bg, 359, R.drawable.ic_list_country_bg),
                CountryListItem(R.string.country_region_name_bf, 226, R.drawable.ic_list_country_bf),
//            CountryListItem(R.string.country_region_name_bi, 257, R.drawable.ic_list_country_bi),
                CountryListItem(R.string.country_region_name_kh, 855, R.drawable.ic_list_country_kh),
                CountryListItem(R.string.country_region_name_cm, 237, R.drawable.ic_list_country_cm),
                CountryListItem(R.string.country_region_name_ca, 1, R.drawable.ic_list_country_ca),
                CountryListItem(R.string.country_region_name_cv, 238, R.drawable.ic_list_country_cv),
                CountryListItem(R.string.country_region_name_ky, 1, R.drawable.ic_list_country_ky),
//            CountryListItem(R.string.country_region_name_cf, 236, R.drawable.ic_list_country_cf),
//            CountryListItem(R.string.country_region_name_td, 235, R.drawable.ic_list_country_td),
                CountryListItem(R.string.country_region_name_cl, 56, R.drawable.ic_list_country_cl),
                CountryListItem(R.string.country_region_name_cn, 86, R.drawable.ic_list_country_cn),
                CountryListItem(R.string.country_region_name_co, 57, R.drawable.ic_list_country_co),
//            CountryListItem(R.string.country_region_name_km, 269, R.drawable.ic_list_country_km),
//            CountryListItem(R.string.country_region_name_ck, 682, R.drawable.ic_list_country_ck),
                CountryListItem(R.string.country_region_name_cr, 506, R.drawable.ic_list_country_cr),
                CountryListItem(R.string.country_region_name_hr, 385, R.drawable.ic_list_country_hr),
                CountryListItem(R.string.country_region_name_cu, 53, R.drawable.ic_list_country_cu),
                CountryListItem(R.string.country_region_name_cw, 599, R.drawable.ic_list_country_cw),
                CountryListItem(R.string.country_region_name_cy, 357, R.drawable.ic_list_country_cy),
                CountryListItem(R.string.country_region_name_cz, 420, R.drawable.ic_list_country_cz),
//            CountryListItem(R.string.country_region_name_cd, 243, R.drawable.ic_list_country_cd),
                CountryListItem(R.string.country_region_name_dk, 45, R.drawable.ic_list_country_dk),
//            CountryListItem(R.string.country_region_name_dj, 253, R.drawable.ic_list_country_dj),
                CountryListItem(R.string.country_region_name_dm, 1, R.drawable.ic_list_country_dm),
                CountryListItem(R.string.country_region_name_do, 1, R.drawable.ic_list_country_do),
                CountryListItem(R.string.country_region_name_ec, 593, R.drawable.ic_list_country_ec),
//            CountryListItem(R.string.country_region_name_eg, 20, R.drawable.ic_list_country_eg),
                CountryListItem(R.string.country_region_name_sv, 503, R.drawable.ic_list_country_sv),
//            CountryListItem(R.string.country_region_name_gq, 240, R.drawable.ic_list_country_gq),
                CountryListItem(R.string.country_region_name_ee, 372, R.drawable.ic_list_country_ee),
//            CountryListItem(R.string.country_region_name_et, 251, R.drawable.ic_list_country_et),
//            CountryListItem(R.string.country_region_name_fo, 298, R.drawable.ic_list_country_fo),
                CountryListItem(R.string.country_region_name_fj, 679, R.drawable.ic_list_country_fj),
                CountryListItem(R.string.country_region_name_fi, 358, R.drawable.ic_list_country_fi),
                CountryListItem(R.string.country_region_name_fr, 33, R.drawable.ic_list_country_fr),
//            CountryListItem(R.string.country_region_name_gf, 995, R.drawable.ic_list_country_fr),
                CountryListItem(R.string.country_region_name_ga, 241, R.drawable.ic_list_country_ga),
//            CountryListItem(R.string.country_region_name_gm, 220, R.drawable.ic_list_country_gm),
                CountryListItem(R.string.country_region_name_ge, 995, R.drawable.ic_list_country_ge),
                CountryListItem(R.string.country_region_name_de, 49, R.drawable.ic_list_country_de),
                CountryListItem(R.string.country_region_name_gh, 233, R.drawable.ic_list_country_gh),
//            CountryListItem(R.string.country_region_name_gi, 350, R.drawable.ic_list_country_gi),
                CountryListItem(R.string.country_region_name_gr, 30, R.drawable.ic_list_country_gr),
//            CountryListItem(R.string.country_region_name_gl, 299, R.drawable.ic_list_country_gl),
                CountryListItem(R.string.country_region_name_gd, 1, R.drawable.ic_list_country_gd),
//            CountryListItem(R.string.country_region_name_gp, 224, R.drawable.ic_list_country_fr),
                CountryListItem(R.string.country_region_name_gu, 1, R.drawable.ic_list_country_gu),
                CountryListItem(R.string.country_region_name_gt, 502, R.drawable.ic_list_country_gt),
//            CountryListItem(R.string.country_region_name_gn, 224, R.drawable.ic_list_country_gn),
                CountryListItem(R.string.country_region_name_gw, 245, R.drawable.ic_list_country_gw),
//            CountryListItem(R.string.country_region_name_gy, 592, R.drawable.ic_list_country_gy),
                CountryListItem(R.string.country_region_name_ht, 509, R.drawable.ic_list_country_ht),
                CountryListItem(R.string.country_region_name_hn, 504, R.drawable.ic_list_country_hn),
                CountryListItem(R.string.country_region_name_hk, 852, R.drawable.ic_list_country_hk),
                CountryListItem(R.string.country_region_name_hu, 36, R.drawable.ic_list_country_hu),
                CountryListItem(R.string.country_region_name_is, 354, R.drawable.ic_list_country_is),
                CountryListItem(R.string.country_region_name_in, 91, R.drawable.ic_list_country_in),
                CountryListItem(R.string.country_region_name_id, 62, R.drawable.ic_list_country_id),
                CountryListItem(R.string.country_region_name_ir, 98, R.drawable.ic_list_country_ir),
                CountryListItem(R.string.country_region_name_iq, 964, R.drawable.ic_list_country_iq),
                CountryListItem(R.string.country_region_name_ie, 353, R.drawable.ic_list_country_ie),
                CountryListItem(R.string.country_region_name_il, 972, R.drawable.ic_list_country_il),
//            CountryListItem(R.string.country_region_name_it, 39, R.drawable.ic_list_country_it),
                CountryListItem(R.string.country_region_name_ci, 225, R.drawable.ic_list_country_ci),
                CountryListItem(R.string.country_region_name_jm, 1, R.drawable.ic_list_country_jm),
                CountryListItem(R.string.country_region_name_jp, 81, R.drawable.ic_list_country_jp),
                CountryListItem(R.string.country_region_name_jo, 962, R.drawable.ic_list_country_jo),
                CountryListItem(R.string.country_region_name_kz, 7, R.drawable.ic_list_country_kz),
                CountryListItem(R.string.country_region_name_ke, 254, R.drawable.ic_list_country_ke),
//            CountryListItem(R.string.country_region_name_ki, 686, R.drawable.ic_list_country_ki),
                CountryListItem(R.string.country_region_name_kw, 965, R.drawable.ic_list_country_kw),
                CountryListItem(R.string.country_region_name_kg, 996, R.drawable.ic_list_country_kg),
                CountryListItem(R.string.country_region_name_la, 856, R.drawable.ic_list_country_la),
                CountryListItem(R.string.country_region_name_lv, 371, R.drawable.ic_list_country_lv),
                CountryListItem(R.string.country_region_name_lb, 961, R.drawable.ic_list_country_lb),
//            CountryListItem(R.string.country_region_name_ls, 266, R.drawable.ic_list_country_ls),
//            CountryListItem(R.string.country_region_name_lr, 231, R.drawable.ic_list_country_lr),
//            CountryListItem(R.string.country_region_name_ly, 218, R.drawable.ic_list_country_ly),
                CountryListItem(R.string.country_region_name_li, 423, R.drawable.ic_list_country_li),
                CountryListItem(R.string.country_region_name_lt, 370, R.drawable.ic_list_country_lt),
                CountryListItem(R.string.country_region_name_lu, 352, R.drawable.ic_list_country_lu),
                CountryListItem(R.string.country_region_name_mo, 853, R.drawable.ic_list_country_mo),
//            CountryListItem(R.string.country_region_name_mg, 261, R.drawable.ic_list_country_mg),
//            CountryListItem(R.string.country_region_name_mw, 265, R.drawable.ic_list_country_mw),
                CountryListItem(R.string.country_region_name_my, 60, R.drawable.ic_list_country_my),
//            CountryListItem(R.string.country_region_name_mv, 960, R.drawable.ic_list_country_mv),
                CountryListItem(R.string.country_region_name_ml, 223, R.drawable.ic_list_country_ml),
                CountryListItem(R.string.country_region_name_mt, 356, R.drawable.ic_list_country_mt),
                CountryListItem(R.string.country_region_name_mq, 1, R.drawable.ic_list_country_mq),
//            CountryListItem(R.string.country_region_name_mr, 222, R.drawable.ic_list_country_mr),
                CountryListItem(R.string.country_region_name_mu, 230, R.drawable.ic_list_country_mu),
                CountryListItem(R.string.country_region_name_mx, 52, R.drawable.ic_list_country_mx),
                CountryListItem(R.string.country_region_name_md, 373, R.drawable.ic_list_country_md),
//            CountryListItem(R.string.country_region_name_mc, 377, R.drawable.ic_list_country_mc),
//            CountryListItem(R.string.country_region_name_mn, 976, R.drawable.ic_list_country_mn),
//            CountryListItem(R.string.country_region_name_me, 382, R.drawable.ic_list_country_me),
                CountryListItem(R.string.country_region_name_ms, 1, R.drawable.ic_list_country_ms),
                CountryListItem(R.string.country_region_name_ma, 212, R.drawable.ic_list_country_ma),
                CountryListItem(R.string.country_region_name_mz, 258, R.drawable.ic_list_country_mz),
                CountryListItem(R.string.country_region_name_mm, 95, R.drawable.ic_list_country_mm),
                CountryListItem(R.string.country_region_name_na, 264, R.drawable.ic_list_country_na),
                CountryListItem(R.string.country_region_name_np, 977, R.drawable.ic_list_country_np),
                CountryListItem(R.string.country_region_name_nl, 31, R.drawable.ic_list_country_nl),
//            CountryListItem(R.string.country_region_name_an, 599, R.drawable.ic_list_country_an),
//            CountryListItem(R.string.country_region_name_nc, 687, R.drawable.ic_list_country_nc),
                CountryListItem(R.string.country_region_name_nz, 64, R.drawable.ic_list_country_nz),
                CountryListItem(R.string.country_region_name_ni, 505, R.drawable.ic_list_country_ni),
                CountryListItem(R.string.country_region_name_ne, 227, R.drawable.ic_list_country_ne),
                CountryListItem(R.string.country_region_name_ng, 234, R.drawable.ic_list_country_ng),
                CountryListItem(R.string.country_region_name_au2, 672, R.drawable.ic_list_country_nf),
                CountryListItem(R.string.country_region_name_mk, 389, R.drawable.ic_list_country_mk),
                CountryListItem(R.string.country_region_name_no, 47, R.drawable.ic_list_country_no),
                CountryListItem(R.string.country_region_name_om, 968, R.drawable.ic_list_country_om),
                CountryListItem(R.string.country_region_name_pk, 92, R.drawable.ic_list_country_pk),
//            CountryListItem(R.string.country_region_name_pw, 680, R.drawable.ic_list_country_pw),
//            CountryListItem(R.string.country_region_name_ps, 970, R.drawable.ic_list_country_ps),
                CountryListItem(R.string.country_region_name_pa, 507, R.drawable.ic_list_country_pa),
                CountryListItem(R.string.country_region_name_pg, 675, R.drawable.ic_list_country_pg),
                CountryListItem(R.string.country_region_name_py, 595, R.drawable.ic_list_country_py),
                CountryListItem(R.string.country_region_name_pe, 51, R.drawable.ic_list_country_pe),
                CountryListItem(R.string.country_region_name_ph, 63, R.drawable.ic_list_country_ph),
                CountryListItem(R.string.country_region_name_pl, 48, R.drawable.ic_list_country_pl),
                CountryListItem(R.string.country_region_name_pt, 351, R.drawable.ic_list_country_pt),
                CountryListItem(R.string.country_region_name_pr, 1, R.drawable.ic_list_country_pr),
                CountryListItem(R.string.country_region_name_qa, 974, R.drawable.ic_list_country_qa),
//            CountryListItem(R.string.country_region_name_cg, 242, R.drawable.ic_list_country_cg),
//            CountryListItem(R.string.country_region_name_re, 262, R.drawable.ic_list_country_fr),
//            CountryListItem(R.string.country_region_name_ro, 40, R.drawable.ic_list_country_ro),
                CountryListItem(R.string.country_region_name_ru, 7, R.drawable.ic_list_country_ru),
                CountryListItem(R.string.country_region_name_rw, 250, R.drawable.ic_list_country_rw),
                CountryListItem(R.string.country_region_name_kn, 1, R.drawable.ic_list_country_kn),
                CountryListItem(R.string.country_region_name_lc, 1, R.drawable.ic_list_country_lc),
                CountryListItem(R.string.country_region_name_vc, 1, R.drawable.ic_list_country_vc),
//            CountryListItem(R.string.country_region_name_ws, 685, R.drawable.ic_list_country_ws),
//            CountryListItem(R.string.country_region_name_st, 239, R.drawable.ic_list_country_st),
//            CountryListItem(R.string.country_region_name_sa, 966, R.drawable.ic_list_country_sa),
                CountryListItem(R.string.country_region_name_sn, 221, R.drawable.ic_list_country_sn),
                CountryListItem(R.string.country_region_name_rs, 381, R.drawable.ic_list_country_rs),
//            CountryListItem(R.string.country_region_name_sc, 248, R.drawable.ic_list_country_sc),
//            CountryListItem(R.string.country_region_name_sl, 232, R.drawable.ic_list_country_sl),
                CountryListItem(R.string.country_region_name_sg, 65, R.drawable.ic_list_country_sg),
                CountryListItem(R.string.country_region_name_sk, 421, R.drawable.ic_list_country_sk),
                CountryListItem(R.string.country_region_name_si, 386, R.drawable.ic_list_country_si),
                CountryListItem(R.string.country_region_name_sb, 677, R.drawable.ic_list_country_sb),
//            CountryListItem(R.string.country_region_name_so, 252, R.drawable.ic_list_country_so),
                CountryListItem(R.string.country_region_name_za, 27, R.drawable.ic_list_country_za),
                CountryListItem(R.string.country_region_name_kr, 82, R.drawable.ic_list_country_kr),
//            CountryListItem(R.string.country_region_name_ss, 211, R.drawable.ic_list_country_ss),
                CountryListItem(R.string.country_region_name_es, 34, R.drawable.ic_list_country_es),
                CountryListItem(R.string.country_region_name_lk, 94, R.drawable.ic_list_country_lk),
                CountryListItem(R.string.country_region_name_sd, 249, R.drawable.ic_list_country_sd),
//            CountryListItem(R.string.country_region_name_sr, 597, R.drawable.ic_list_country_sr),
//            CountryListItem(R.string.country_region_name_sz, 268, R.drawable.ic_list_country_sz),
                CountryListItem(R.string.country_region_name_se, 46, R.drawable.ic_list_country_se),
                CountryListItem(R.string.country_region_name_ch, 41, R.drawable.ic_list_country_ch),
                CountryListItem(R.string.country_region_name_tw, 886, R.drawable.ic_list_country_tw),
                CountryListItem(R.string.country_region_name_tj, 992, R.drawable.ic_list_country_tj),
                CountryListItem(R.string.country_region_name_tz, 255, R.drawable.ic_list_country_tz),
                CountryListItem(R.string.country_region_name_th, 66, R.drawable.ic_list_country_th),
//            CountryListItem(R.string.country_region_name_tl, 670, R.drawable.ic_list_country_tl),
                CountryListItem(R.string.country_region_name_tg, 228, R.drawable.ic_list_country_tg),
//            CountryListItem(R.string.country_region_name_to, 676, R.drawable.ic_list_country_to),
                CountryListItem(R.string.country_region_name_tt, 1, R.drawable.ic_list_country_tt),
                CountryListItem(R.string.country_region_name_tn, 216, R.drawable.ic_list_country_tn),
                CountryListItem(R.string.country_region_name_tr, 90, R.drawable.ic_list_country_tr),
                CountryListItem(R.string.country_region_name_tm, 993, R.drawable.ic_list_country_tm),
                CountryListItem(R.string.country_region_name_tc, 1, R.drawable.ic_list_country_tc),
                CountryListItem(R.string.country_region_name_ug, 256, R.drawable.ic_list_country_ug),
                CountryListItem(R.string.country_region_name_ua, 380, R.drawable.ic_list_country_ua),
                CountryListItem(R.string.country_region_name_ae, 971, R.drawable.ic_list_country_ae),
                CountryListItem(R.string.country_region_name_gb, 44, R.drawable.ic_list_country_gb),
                CountryListItem(R.string.country_region_name_us, 1, R.drawable.ic_list_country_us),
                CountryListItem(R.string.country_region_name_uy, 598, R.drawable.ic_list_country_uy),
                CountryListItem(R.string.country_region_name_uz, 998, R.drawable.ic_list_country_uz),
//            CountryListItem(R.string.country_region_name_vu, 678, R.drawable.ic_list_country_vu),
                CountryListItem(R.string.country_region_name_ve, 58, R.drawable.ic_list_country_ve),
                CountryListItem(R.string.country_region_name_vn, 84, R.drawable.ic_list_country_vn),
                CountryListItem(R.string.country_region_name_vi, 1, R.drawable.ic_list_country_vi),
                CountryListItem(R.string.country_region_name_ye, 967, R.drawable.ic_list_country_ye),
                CountryListItem(R.string.country_region_name_zm, 260, R.drawable.ic_list_country_zm),
                CountryListItem(R.string.country_region_name_zw, 263, R.drawable.ic_list_country_zw)
        )
    }

    private fun getCountryListInitialisedWithOptionsForAustralia(): MutableList<CountryListItemInterface> {
        return mutableListOf(
                CountryGroupTitle(R.string.options_for_australia),
                CountryListItem(R.string.country_region_name_au, 61, R.drawable.ic_list_country_au),
                CountryListItem(R.string.country_region_name_au2, 672, R.drawable.ic_list_country_nf)
        )
    }

    private fun getSortedAndGroupedCountryListBasedOnEnglish(context: Context): List<CountryListItemInterface> {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        val configurationContext = context.createConfigurationContext(config)

        var groupTitle = ""

        val retVal = getCountryListInitialisedWithOptionsForAustralia()

        getCountries().sortedBy {
            configurationContext.getText(it.countryNameResId).toString()
        }.forEach {
            val firstLetter = configurationContext.getText(it.countryNameResId).toString().first().toString()

            if (groupTitle != firstLetter) {
                groupTitle = firstLetter

                retVal.add(CountryGroupTitle(null, groupTitle))
            }

            retVal.add(it)
        }

        return retVal
    }

    private fun getSortedCountryListBasedOnLocaleLanguage(context: Context): List<CountryListItemInterface> {
        val retVal = getCountryListInitialisedWithOptionsForAustralia()

        getCountries().sortedBy {
            context.getString(it.countryNameResId)
        }.forEach {
            retVal.add(it)
        }

        return retVal
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getSortedAndGroupedCountryListBasedOnLocaleLanguage(context: Context): List<CountryListItemInterface> {
        val alphabeticIndex: AlphabeticIndex<CountryListItem> =
                AlphabeticIndex(Locale.getDefault())

        getCountries().forEach {
            val countryNameInLocaleLanguage = context.getString(it.countryNameResId)
            alphabeticIndex.addRecord(countryNameInLocaleLanguage, it)
        }

        val retVal = getCountryListInitialisedWithOptionsForAustralia()

        alphabeticIndex.forEach { bucket ->
            if (bucket.size() > 0) {
                retVal.add(CountryGroupTitle(null, bucket.label))

                bucket.forEach { record ->
                    retVal.add(record.data)
                }
            }
        }

        return retVal
    }
}