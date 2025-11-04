package com.OnTime.ontime.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.OnTime.ontime.R
import com.OnTime.ontime.ui.viewmodel.CalendarViewModel

class CalendarListFragment : Fragment() {
    
    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.calendar_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            // TODO: Update RecyclerView adapter
        }
    }
}
