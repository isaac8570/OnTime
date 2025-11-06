package com.OnTime.ontime.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.OnTime.ontime.R

class EventDetailFragment : Fragment() {
    
    private lateinit var eventTitleText: TextView
    private lateinit var routeInfoText: TextView
    private lateinit var departureTimeText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_event_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        eventTitleText = view.findViewById(R.id.event_title)
        routeInfoText = view.findViewById(R.id.route_info)
        departureTimeText = view.findViewById(R.id.departure_time)
        
        // TODO: Load event details and route calculation
    }
}
