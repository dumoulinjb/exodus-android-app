package org.eu.exodus_privacy.exodusprivacy.fragments.trackerdetail

import android.content.res.Configuration
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import org.eu.exodus_privacy.exodusprivacy.R
import org.eu.exodus_privacy.exodusprivacy.databinding.FragmentTrackerDetailBinding
import org.eu.exodus_privacy.exodusprivacy.fragments.apps.model.AppsRVAdapter
import org.eu.exodus_privacy.exodusprivacy.utils.copyToClipboard
import org.eu.exodus_privacy.exodusprivacy.utils.openURL
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class TrackerDetailFragment : Fragment(R.layout.fragment_tracker_detail) {

    private var _binding: FragmentTrackerDetailBinding? = null
    private val binding get() = _binding!!

    private val args: TrackerDetailFragmentArgs by navArgs()
    private val viewModel: TrackerDetailViewModel by viewModels()

    private val httpPattern: Pattern = Pattern.compile("[a-z]+:\\/\\/[^ \\n]*")

    @Inject
    lateinit var customTabsIntent: CustomTabsIntent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTrackerDetailBinding.bind(view)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
        returnTransition = MaterialFadeThrough()

        viewModel.getTracker(args.trackerID)

        binding.toolbarTD.setOnClickListener {
            view.findNavController().navigateUp()
        }

        viewModel.tracker.observe(viewLifecycleOwner) { tracker ->
            if (tracker.exodusApplications.isNotEmpty()) {
                viewModel.getApps(tracker.exodusApplications)
            }

            binding.apply {
                openTrackerPage.apply {
                    setOnClickListener {
                        openURL(
                            customTabsIntent,
                            view.context,
                            tracker.website,
                        )
                    }
                    setOnLongClickListener {
                        copyToClipboard(
                            requireContext(),
                            tracker.website,
                        )
                    }
                }
                trackerTitleTV.text = tracker.name

                // Setup categories
                chipGroup.removeAllViews()
                tracker.categories.forEach {
                    val chip = Chip(context)
                    val chipStyle = ChipDrawable.createFromAttributes(
                        view.context,
                        null,
                        0,
                        R.style.Theme_Exodus_Chip,
                    )
                    chip.text = it
                    chip.setChipDrawable(chipStyle)
                    chipGroup.addView(chip)
                }

                // Show presence if tracker is present on device
                if (args.percentage != 0) {
                    trackerPresenceTV.visibility = View.VISIBLE
                    trackerPresenceTV.text = resources.getQuantityString(
                        R.plurals.trackers_presence,
                        tracker.exodusApplications.size,
                        args.percentage,
                        tracker.exodusApplications.size,
                    )
                }

                // Tracker description and webURL
                if (tracker.description.isNotEmpty()) {
                    trackerDescTV.apply {
                        val markwon = Markwon.create(view.context)
                        markwon.setMarkdown(this, tracker.description)
                        movementMethod = LinkMovementMethod.getInstance()
                        isClickable = true
                    }
                    Linkify.addLinks(trackerDescTV, httpPattern, "")
                } else {
                    trackerDescTV.visibility = View.GONE
                }

                // Tracker code and network signatures
                codeSignTV.text = tracker.code_signature
                if (tracker.network_signature.isNotEmpty()) {
                    networkSignTV.text = tracker.network_signature
                    networkSignTV.setOnLongClickListener {
                        copyToClipboard(
                            requireContext(),
                            networkSignTV.text.toString(),
                        )
                    }
                    networkDetectTV.visibility = View.VISIBLE
                    networkSignTV.visibility = View.VISIBLE
                } else {
                    networkDetectTV.visibility = View.GONE
                    networkSignTV.visibility = View.GONE
                }
            }
        }

        viewModel.appsList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                val appsRVAdapter = AppsRVAdapter(findNavController().currentDestination!!.id)
                binding.appsListTV.visibility = View.VISIBLE
                binding.appsListRV.apply {
                    visibility = View.VISIBLE
                    adapter = appsRVAdapter
                    val column: Int =
                        if (resources.configuration.smallestScreenWidthDp >= 600 && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            2
                        } else {
                            1
                        }
                    layoutManager = object : StaggeredGridLayoutManager(column, 1) {
                        override fun canScrollVertically(): Boolean {
                            return false
                        }
                    }
                }
                appsRVAdapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.toolbarTD.setOnMenuItemClickListener(null)
        _binding = null
    }
}
