package au.gov.health.covidsafe.ui.restriction

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.databinding.FragmentRestrictionBinding
import au.gov.health.covidsafe.links.LinkBuilder
import au.gov.health.covidsafe.networking.response.Subheadings
import au.gov.health.covidsafe.preference.Preference
import au.gov.health.covidsafe.ui.base.BaseFragment
import au.gov.health.covidsafe.utils.AnimationUtils.slideAnimation
import au.gov.health.covidsafe.utils.SlideDirection
import au.gov.health.covidsafe.utils.SlideType
import kotlinx.android.synthetic.main.fragment_restriction.*
import kotlinx.android.synthetic.main.fragment_restriction.select_state

class RestrictionFragment: BaseFragment() {

    private val viewModelRestriction: RestrictionViewModel by viewModels()
    private lateinit var stateListAdapter: StateAdapter
    private lateinit var stateActivityListAdapter: StateActivityAdapter
    private lateinit var restrictionListAdapter: RestrictionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeObservers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentRestrictionBinding.inflate(layoutInflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewModelRestriction
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModelRestriction.setup()
        loadStateAndListener()
    }

    override fun onResume() {
        super.onResume()
        setupListener()
    }

    private fun initializeObservers() {
        viewModelRestriction.activityList.observe(this, Observer { list ->
            list?.let {
                if (it.size > 0) {
                    stateActivityListAdapter = StateActivityAdapter(this.requireContext())
                    select_state_activity.adapter = stateActivityListAdapter
                    val layoutManager = LinearLayoutManager(this.requireContext())
                    select_state_activity.layoutManager = layoutManager

                    stateActivityListAdapter.setRecords(it, 0)

                    stateActivityListAdapter.setOnStateListClickListener(object : StateActivityAdapter.OnStateListClickListener {
                        override fun onStateClick(subheading: List<Subheadings>, activity: String?, activityTitle: String?, time: String?, content: String?) {
                            select_state_activity_layout.slideAnimation(SlideDirection.DOWN, SlideType.HIDE, 300)
                            viewModelRestriction.setSelectedStateActivity(activity, activityTitle, time)
                            loadRestrictionSection(subheading,activity, activityTitle, time, content)
                        }
                    })
                }
            }
        })
    }

    private fun loadRestrictionSection(subheading: List<Subheadings>, activity: String? , activityTitle: String?, time: String?, content: String?) {
        restrictionListAdapter = RestrictionAdapter(this.requireContext())
        restriction_list.adapter = restrictionListAdapter
        val layoutManager = LinearLayoutManager(this.requireContext())
        restriction_list.layoutManager = layoutManager

        val subContent = ArrayList<Subheadings>()
        if (!content.isNullOrEmpty()) {
            subContent.add(Subheadings(requireContext().getString(R.string.main_restrictions), content))
        }
        subheading.forEach {
            subContent.add(it)
        }
        restrictionListAdapter.setRecords(subContent)
        restrictionListAdapter.setOnStateListClickListener(object : RestrictionAdapter.OnStateListClickListener{
            override fun onSectionClick(title: String?, content: String?) {

                val intent = Intent(requireContext(), RestrictionDescActivity::class.java)
                intent.putExtra("toolbarTitle", title)
                intent.putExtra("ActivityTitle", activityTitle)
                intent.putExtra("htmlDesc", content)
                requireContext().startActivity(intent)
            }
        })
    }

    private fun loadStateAndListener() {
        stateListAdapter = StateAdapter(this.requireContext())
        select_state.adapter = stateListAdapter
        val layoutManager = LinearLayoutManager(this.requireContext())
        select_state.layoutManager = layoutManager

        stateListAdapter.setRecords(createStateList(), 0)

        stateListAdapter.setOnStateListClickListener(object : StateAdapter.OnStateListClickListener{
            override fun onStateClick(state: String) {
                select_state_layout.slideAnimation(SlideDirection.DOWN, SlideType.HIDE, 300)
                viewModelRestriction.stateListVisible.value = false
                viewModelRestriction.setSelectedState(state, lifecycle)
            }
        })
        if (Preference.getSelectedRestrictionState(requireContext())!=null && Preference.getSelectedRestrictionState(requireContext())!="")  {
            viewModelRestriction.loadActivity(Preference.getSelectedRestrictionState(requireContext()).toString(), lifecycle)
        }

        btn_dissmiss.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    fun setupListener() {
        layout_select_activity.setOnClickListener {
            select_state_activity_layout.slideAnimation(SlideDirection.UP, SlideType.SHOW, 300)
            viewModelRestriction.stateActivityListVisible.value = true
        }

        layout_select_state.setOnClickListener {
            select_state_layout.slideAnimation(SlideDirection.UP, SlideType.SHOW, 300)
            viewModelRestriction.stateListVisible.value = true
        }
    }

    fun createStateList(): ArrayList<String> {
        val list = ArrayList<String>()
        list.add(requireContext().getString(R.string.australian_capital_territory))
        list.add(requireContext().getString(R.string.new_south_wales))
        list.add(requireContext().getString(R.string.northern_territory))
        list.add(requireContext().getString(R.string.queensland))
        list.add(requireContext().getString(R.string.south_australia))
        list.add(requireContext().getString(R.string.tasmania))
        list.add(requireContext().getString(R.string.victoria))
        list.add(requireContext().getString(R.string.western_australia))
        return list
    }
}