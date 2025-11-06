package com.OnTime.ontime.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.OnTime.ontime.ui.compose.CalendarListScreen
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.OnTime.ontime.ui.viewmodel.CalendarViewModel
import com.OnTime.ontime.ui.viewmodel.ViewModelFactory

class CalendarListFragment : Fragment() {
    
    private lateinit var viewModel: CalendarViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OnTimeTheme {
                    // ViewModel must be initialized before setContent is called if used here.
                    // We will initialize it in onViewCreated and observe it there.
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel using the factory
        val factory = ViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(CalendarViewModel::class.java)

        // Set the content after viewModel is initialized
        (view as ComposeView).setContent {
            OnTimeTheme {
                val events by viewModel.events.observeAsState(initial = emptyList())
                CalendarListScreen(events = events)
            }
        }

        // Trigger the loading of events
        viewModel.loadEvents()
    }
}
