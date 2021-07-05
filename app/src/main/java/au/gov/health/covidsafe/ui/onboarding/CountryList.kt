package au.gov.health.covidsafe.ui.onboarding

import android.content.Context
import android.content.res.Configuration
import android.icu.text.AlphabeticIndex
import android.os.Build
import androidx.annotation.RequiresApi
import au.gov.health.covidsafe.R
import com.blongho.country_data.World
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
                CountryListItem(R.string.country_region_name_al, 355, World.getFlagOf("al")),
                CountryListItem(R.string.country_region_name_dz, 213, World.getFlagOf("dz")),
//            CountryListItem(R.string.country_region_name_ad, 376, R.drawable.ic_list_country_ad),
                CountryListItem(R.string.country_region_name_ao, 244, World.getFlagOf("ao")),
                CountryListItem(R.string.country_region_name_ai, 1,World.getFlagOf("ai")),
                CountryListItem(R.string.country_region_name_ag, 1, World.getFlagOf("ag")),
                CountryListItem(R.string.country_region_name_ar, 54, World.getFlagOf("ar")),
                CountryListItem(R.string.country_region_name_am, 374, World.getFlagOf("am")),
                CountryListItem(R.string.country_region_name_aw, 297, World.getFlagOf("aw")),
                CountryListItem(R.string.country_region_name_au, 61, World.getFlagOf("au")),
                CountryListItem(R.string.country_region_name_at, 43, World.getFlagOf("at")),
                CountryListItem(R.string.country_region_name_az, 994, World.getFlagOf("az")),
                CountryListItem(R.string.country_region_name_bs, 1, World.getFlagOf("bs")),
                CountryListItem(R.string.country_region_name_bh, 973, World.getFlagOf("bh")),
                CountryListItem(R.string.country_region_name_bd, 880, World.getFlagOf("bd")),
                CountryListItem(R.string.country_region_name_bb, 1, World.getFlagOf("bb")),
                CountryListItem(R.string.country_region_name_by, 375, World.getFlagOf("by")),
                CountryListItem(R.string.country_region_name_be, 32, World.getFlagOf("be")),
                CountryListItem(R.string.country_region_name_bz, 501, World.getFlagOf("bz")),
                CountryListItem(R.string.country_region_name_bj, 229, World.getFlagOf("bj")),
                CountryListItem(R.string.country_region_name_bm, 1, World.getFlagOf("bm")),
//            CountryListItem(R.string.country_region_name_bt, 975, R.drawable.ic_list_country_bt),
                CountryListItem(R.string.country_region_name_bo, 591, World.getFlagOf("bo")),
                CountryListItem(R.string.country_region_name_ba, 387, World.getFlagOf("ba")),
                CountryListItem(R.string.country_region_name_bw, 267, World.getFlagOf("bw")),
                CountryListItem(R.string.country_region_name_br, 55, World.getFlagOf("br")),
//            CountryListItem(R.string.country_region_name_bn, 673, R.drawable.ic_list_country_bn),
                CountryListItem(R.string.country_region_name_vg, 1, World.getFlagOf("vg")),
                CountryListItem(R.string.country_region_name_bg, 359, World.getFlagOf("bg")),
                CountryListItem(R.string.country_region_name_bf, 226, World.getFlagOf("bf")),
//            CountryListItem(R.string.country_region_name_bi, 257, R.drawable.ic_list_country_bi),
                CountryListItem(R.string.country_region_name_kh, 855, World.getFlagOf("kh")),
                CountryListItem(R.string.country_region_name_cm, 237, World.getFlagOf("cm")),
                CountryListItem(R.string.country_region_name_ca, 1, World.getFlagOf("ca")),
                CountryListItem(R.string.country_region_name_cv, 238, World.getFlagOf("cv")),
                CountryListItem(R.string.country_region_name_ky, 1, World.getFlagOf("ky")),
//            CountryListItem(R.string.country_region_name_cf, 236, R.drawable.ic_list_country_cf),
//            CountryListItem(R.string.country_region_name_td, 235, R.drawable.ic_list_country_td),
                CountryListItem(R.string.country_region_name_cl, 56, World.getFlagOf("cl")),
                CountryListItem(R.string.country_region_name_cn, 86, World.getFlagOf("cn")),
                CountryListItem(R.string.country_region_name_co, 57, World.getFlagOf("co")),
//            CountryListItem(R.string.country_region_name_km, 269, R.drawable.ic_list_country_km),
//            CountryListItem(R.string.country_region_name_ck, 682, R.drawable.ic_list_country_ck),
                CountryListItem(R.string.country_region_name_cr, 506, World.getFlagOf("cr")),
                CountryListItem(R.string.country_region_name_hr, 385, World.getFlagOf("hr")),
                CountryListItem(R.string.country_region_name_cu, 53, World.getFlagOf("cu")),
                CountryListItem(R.string.country_region_name_cw, 599, World.getFlagOf("cw")),
                CountryListItem(R.string.country_region_name_cy, 357, World.getFlagOf("cy")),
                CountryListItem(R.string.country_region_name_cz, 420, World.getFlagOf("cz")),
//            CountryListItem(R.string.country_region_name_cd, 243, R.drawable.ic_list_country_cd),
                CountryListItem(R.string.country_region_name_dk, 45, World.getFlagOf("dk")),
//            CountryListItem(R.string.country_region_name_dj, 253, R.drawable.ic_list_country_dj),
                CountryListItem(R.string.country_region_name_dm, 1, World.getFlagOf("dm")),
                CountryListItem(R.string.country_region_name_do, 1, World.getFlagOf("do")),
                CountryListItem(R.string.country_region_name_ec, 593, World.getFlagOf("ec")),
//            CountryListItem(R.string.country_region_name_eg, 20, R.drawable.ic_list_country_eg),
                CountryListItem(R.string.country_region_name_sv, 503, World.getFlagOf("sv")),
//            CountryListItem(R.string.country_region_name_gq, 240, R.drawable.ic_list_country_gq),
                CountryListItem(R.string.country_region_name_ee, 372, World.getFlagOf("ee")),
//            CountryListItem(R.string.country_region_name_et, 251, R.drawable.ic_list_country_et),
//            CountryListItem(R.string.country_region_name_fo, 298, R.drawable.ic_list_country_fo),
                CountryListItem(R.string.country_region_name_fj, 679, World.getFlagOf("fj")),
                CountryListItem(R.string.country_region_name_fi, 358, World.getFlagOf("fi")),
                CountryListItem(R.string.country_region_name_fr, 33, World.getFlagOf("fr")),
//            CountryListItem(R.string.country_region_name_gf, 995, R.drawable.ic_list_country_fr),
                CountryListItem(R.string.country_region_name_ga, 241, World.getFlagOf("ga")),
//            CountryListItem(R.string.country_region_name_gm, 220, R.drawable.ic_list_country_gm),
                CountryListItem(R.string.country_region_name_ge, 995, World.getFlagOf("ge")),
                CountryListItem(R.string.country_region_name_de, 49, World.getFlagOf("de")),
                CountryListItem(R.string.country_region_name_gh, 233, World.getFlagOf("gh")),
//            CountryListItem(R.string.country_region_name_gi, 350, R.drawable.ic_list_country_gi),
                CountryListItem(R.string.country_region_name_gr, 30, World.getFlagOf("gr")),
                CountryListItem(R.string.country_region_name_gl, 299, World.getFlagOf("gl")),
                CountryListItem(R.string.country_region_name_gd, 1, World.getFlagOf("gd")),
//            CountryListItem(R.string.country_region_name_gp, 224, R.drawable.ic_list_country_fr),
                CountryListItem(R.string.country_region_name_gu, 1, World.getFlagOf("gu")),
                CountryListItem(R.string.country_region_name_gt, 502, World.getFlagOf("gt")),
//            CountryListItem(R.string.country_region_name_gn, 224, R.drawable.ic_list_country_gn),
                CountryListItem(R.string.country_region_name_gw, 245, World.getFlagOf("gw")),
//            CountryListItem(R.string.country_region_name_gy, 592, R.drawable.ic_list_country_gy),
                CountryListItem(R.string.country_region_name_ht, 509, World.getFlagOf("ht")),
                CountryListItem(R.string.country_region_name_hn, 504, World.getFlagOf("hn")),
                CountryListItem(R.string.country_region_name_hk, 852, World.getFlagOf("hk")),
                CountryListItem(R.string.country_region_name_hu, 36, World.getFlagOf("hu")),
                CountryListItem(R.string.country_region_name_is, 354, World.getFlagOf("is")),
                CountryListItem(R.string.country_region_name_in, 91, World.getFlagOf("in")),
                CountryListItem(R.string.country_region_name_id, 62, World.getFlagOf("id")),
                CountryListItem(R.string.country_region_name_ir, 98, World.getFlagOf("ir")),
                CountryListItem(R.string.country_region_name_iq, 964, World.getFlagOf("iq")),
                CountryListItem(R.string.country_region_name_ie, 353, World.getFlagOf("ie")),
                CountryListItem(R.string.country_region_name_il, 972, World.getFlagOf("il")),
//            CountryListItem(R.string.country_region_name_it, 39, R.drawable.ic_list_country_it),
                CountryListItem(R.string.country_region_name_ci, 225, World.getFlagOf("ci")),
                CountryListItem(R.string.country_region_name_jm, 1, World.getFlagOf("jm")),
                CountryListItem(R.string.country_region_name_jp, 81, World.getFlagOf("jp")),
                CountryListItem(R.string.country_region_name_jo, 962, World.getFlagOf("jo")),
                CountryListItem(R.string.country_region_name_kz, 7, World.getFlagOf("kz")),
                CountryListItem(R.string.country_region_name_ke, 254, World.getFlagOf("ke")),
//            CountryListItem(R.string.country_region_name_ki, 686, R.drawable.ic_list_country_ki),
                CountryListItem(R.string.country_region_name_kw, 965, World.getFlagOf("kw")),
                CountryListItem(R.string.country_region_name_kg, 996, World.getFlagOf("kg")),
                CountryListItem(R.string.country_region_name_la, 856, World.getFlagOf("la")),
                CountryListItem(R.string.country_region_name_lv, 371, World.getFlagOf("lv")),
                CountryListItem(R.string.country_region_name_lb, 961, World.getFlagOf("lb")),
//            CountryListItem(R.string.country_region_name_ls, 266, R.drawable.ic_list_country_ls),
//            CountryListItem(R.string.country_region_name_lr, 231, R.drawable.ic_list_country_lr),
//            CountryListItem(R.string.country_region_name_ly, 218, R.drawable.ic_list_country_ly),
                CountryListItem(R.string.country_region_name_li, 423, World.getFlagOf("li")),
                CountryListItem(R.string.country_region_name_lt, 370, World.getFlagOf("lt")),
                CountryListItem(R.string.country_region_name_lu, 352, World.getFlagOf("lu")),
                CountryListItem(R.string.country_region_name_mo, 853, World.getFlagOf("mo")),
//            CountryListItem(R.string.country_region_name_mg, 261, R.drawable.ic_list_country_mg),
//            CountryListItem(R.string.country_region_name_mw, 265, R.drawable.ic_list_country_mw),
                CountryListItem(R.string.country_region_name_my, 60, World.getFlagOf("my")),
//            CountryListItem(R.string.country_region_name_mv, 960, R.drawable.ic_list_country_mv),
                CountryListItem(R.string.country_region_name_ml, 223, World.getFlagOf("ml")),
                CountryListItem(R.string.country_region_name_mt, 356, World.getFlagOf("mt")),
                CountryListItem(R.string.country_region_name_mq, 1, World.getFlagOf("mq")),
//            CountryListItem(R.string.country_region_name_mr, 222, R.drawable.ic_list_country_mr),
                CountryListItem(R.string.country_region_name_mu, 230, World.getFlagOf("mu")),
                CountryListItem(R.string.country_region_name_mx, 52, World.getFlagOf("mx")),
                CountryListItem(R.string.country_region_name_md, 373, World.getFlagOf("md")),
                CountryListItem(R.string.country_region_name_mc, 377, World.getFlagOf("mc")),
                CountryListItem(R.string.country_region_name_mn, 976, World.getFlagOf("mn")),
                CountryListItem(R.string.country_region_name_me, 382, World.getFlagOf("me")),
                CountryListItem(R.string.country_region_name_ms, 1, World.getFlagOf("ms")),
                CountryListItem(R.string.country_region_name_ma, 212, World.getFlagOf("ma")),
                CountryListItem(R.string.country_region_name_mz, 258, World.getFlagOf("mz")),
                CountryListItem(R.string.country_region_name_mm, 95, World.getFlagOf("mm")),
                CountryListItem(R.string.country_region_name_na, 264, World.getFlagOf("na")),
                CountryListItem(R.string.country_region_name_np, 977, World.getFlagOf("np")),
                CountryListItem(R.string.country_region_name_nl, 31, World.getFlagOf("nl")),
//            CountryListItem(R.string.country_region_name_an, 599, R.drawable.ic_list_country_an),
//            CountryListItem(R.string.country_region_name_nc, 687, R.drawable.ic_list_country_nc),
                CountryListItem(R.string.country_region_name_nz, 64, World.getFlagOf("nz")),
                CountryListItem(R.string.country_region_name_ni, 505, World.getFlagOf("ni")),
                CountryListItem(R.string.country_region_name_ne, 227, World.getFlagOf("ne")),
                CountryListItem(R.string.country_region_name_ng, 234, World.getFlagOf("ng")),
                CountryListItem(R.string.country_region_name_au2, 672, World.getFlagOf("au2")),
                CountryListItem(R.string.country_region_name_mk, 389, World.getFlagOf("mk")),
                CountryListItem(R.string.country_region_name_no, 47, World.getFlagOf("no")),
                CountryListItem(R.string.country_region_name_om, 968, World.getFlagOf("om")),
                CountryListItem(R.string.country_region_name_pk, 92, World.getFlagOf("pk")),
//            CountryListItem(R.string.country_region_name_pw, 680, R.drawable.ic_list_country_pw),
//            CountryListItem(R.string.country_region_name_ps, 970, R.drawable.ic_list_country_ps),
                CountryListItem(R.string.country_region_name_pa, 507, World.getFlagOf("pa")),
                CountryListItem(R.string.country_region_name_pg, 675, World.getFlagOf("pg")),
                CountryListItem(R.string.country_region_name_py, 595, World.getFlagOf("py")),
                CountryListItem(R.string.country_region_name_pe, 51, World.getFlagOf("pe")),
                CountryListItem(R.string.country_region_name_ph, 63, World.getFlagOf("ph")),
                CountryListItem(R.string.country_region_name_pl, 48, World.getFlagOf("pl")),
                CountryListItem(R.string.country_region_name_pt, 351, World.getFlagOf("pt")),
                CountryListItem(R.string.country_region_name_pr, 1, World.getFlagOf("pr")),
                CountryListItem(R.string.country_region_name_qa, 974, World.getFlagOf("qa")),
//            CountryListItem(R.string.country_region_name_cg, 242, R.drawable.ic_list_country_cg),
//            CountryListItem(R.string.country_region_name_re, 262, R.drawable.ic_list_country_fr),
//            CountryListItem(R.string.country_region_name_ro, 40, R.drawable.ic_list_country_ro),
                CountryListItem(R.string.country_region_name_ru, 7, World.getFlagOf("ru")),
                CountryListItem(R.string.country_region_name_rw, 250, World.getFlagOf("rw")),
                CountryListItem(R.string.country_region_name_kn, 1, World.getFlagOf("kn")),
                CountryListItem(R.string.country_region_name_lc, 1, World.getFlagOf("lc")),
                CountryListItem(R.string.country_region_name_vc, 1, World.getFlagOf("vc")),
//            CountryListItem(R.string.country_region_name_ws, 685, R.drawable.ic_list_country_ws),
//            CountryListItem(R.string.country_region_name_st, 239, R.drawable.ic_list_country_st),
//            CountryListItem(R.string.country_region_name_sa, 966, R.drawable.ic_list_country_sa),
                CountryListItem(R.string.country_region_name_sn, 221, World.getFlagOf("sn")),
                CountryListItem(R.string.country_region_name_rs, 381, World.getFlagOf("rs")),
//            CountryListItem(R.string.country_region_name_sc, 248, R.drawable.ic_list_country_sc),
//            CountryListItem(R.string.country_region_name_sl, 232, R.drawable.ic_list_country_sl),
                CountryListItem(R.string.country_region_name_sg, 65, World.getFlagOf("sq")),
                CountryListItem(R.string.country_region_name_sk, 421, World.getFlagOf("sk")),
                CountryListItem(R.string.country_region_name_si, 386, World.getFlagOf("si")),
                CountryListItem(R.string.country_region_name_sb, 677, World.getFlagOf("sb")),
//            CountryListItem(R.string.country_region_name_so, 252, R.drawable.ic_list_country_so),
                CountryListItem(R.string.country_region_name_za, 27, World.getFlagOf("za")),
                CountryListItem(R.string.country_region_name_kr, 82, World.getFlagOf("kr")),
//            CountryListItem(R.string.country_region_name_ss, 211, R.drawable.ic_list_country_ss),
                CountryListItem(R.string.country_region_name_es, 34, World.getFlagOf("es")),
                CountryListItem(R.string.country_region_name_lk, 94, World.getFlagOf("lk")),
                CountryListItem(R.string.country_region_name_sd, 249, World.getFlagOf("sd")),
//            CountryListItem(R.string.country_region_name_sr, 597, R.drawable.ic_list_country_sr),
//            CountryListItem(R.string.country_region_name_sz, 268, R.drawable.ic_list_country_sz),
                CountryListItem(R.string.country_region_name_se, 46, World.getFlagOf("se")),
                CountryListItem(R.string.country_region_name_ch, 41, World.getFlagOf("ch")),
                CountryListItem(R.string.country_region_name_tw, 886, World.getFlagOf("tw")),
                CountryListItem(R.string.country_region_name_tj, 992, World.getFlagOf("tj")),
                CountryListItem(R.string.country_region_name_tz, 255, World.getFlagOf("tz")),
                CountryListItem(R.string.country_region_name_th, 66, World.getFlagOf("th")),
//            CountryListItem(R.string.country_region_name_tl, 670, R.drawable.ic_list_country_tl),
                CountryListItem(R.string.country_region_name_tg, 228, World.getFlagOf("tg")),
//            CountryListItem(R.string.country_region_name_to, 676, R.drawable.ic_list_country_to),
                CountryListItem(R.string.country_region_name_tt, 1, World.getFlagOf("tt")),
                CountryListItem(R.string.country_region_name_tn, 216, World.getFlagOf("tn")),
                CountryListItem(R.string.country_region_name_tr, 90, World.getFlagOf("tr")),
                CountryListItem(R.string.country_region_name_tm, 993, World.getFlagOf("tm")),
                CountryListItem(R.string.country_region_name_tc, 1, World.getFlagOf("tc")),
                CountryListItem(R.string.country_region_name_ug, 256, World.getFlagOf("ug")),
                CountryListItem(R.string.country_region_name_ua, 380, World.getFlagOf("ua")),
                CountryListItem(R.string.country_region_name_ae, 971, World.getFlagOf("ae")),
                CountryListItem(R.string.country_region_name_gb, 44, World.getFlagOf("gb")),
                CountryListItem(R.string.country_region_name_us, 1, World.getFlagOf("us")),
                CountryListItem(R.string.country_region_name_uy, 598, World.getFlagOf("uy")),
                CountryListItem(R.string.country_region_name_uz, 998, World.getFlagOf("uz")),
//            CountryListItem(R.string.country_region_name_vu, 678, R.drawable.ic_list_country_vu),
                CountryListItem(R.string.country_region_name_ve, 58, World.getFlagOf("ve")),
                CountryListItem(R.string.country_region_name_vn, 84, World.getFlagOf("vn")),
                CountryListItem(R.string.country_region_name_vi, 1, World.getFlagOf("vi")),
                CountryListItem(R.string.country_region_name_ye, 967, World.getFlagOf("ye")),
                CountryListItem(R.string.country_region_name_zm, 260, World.getFlagOf("zm")),
                CountryListItem(R.string.country_region_name_zw, 263, World.getFlagOf("zw"))
        )
    }

    private fun getCountryListInitialisedWithOptionsForAustralia(): MutableList<CountryListItemInterface> {
        return mutableListOf(
                CountryGroupTitle(R.string.options_for_australia),
                CountryListItem(R.string.country_region_name_au, 61, World.getFlagOf("au")),
                CountryListItem(R.string.country_region_name_au2, 672, World.getFlagOf("nf"))
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