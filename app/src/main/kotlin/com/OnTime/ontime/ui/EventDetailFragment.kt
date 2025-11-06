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
    private lateinit var originText: TextView
    private lateinit var destinationText: TextView
    private lateinit var travelTimeText: TextView
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
        originText = view.findViewById(R.id.origin_text)
        destinationText = view.findViewById(R.id.destination_text)
        travelTimeText = view.findViewById(R.id.travel_time_text)
        departureTimeText = view.findViewById(R.id.departure_time)
        
        // TODO: Load event details and route calculation results from arguments or ViewModel
        
        // Set placeholder data for now
        val dummyOrigin = "현재 위치 (123.456, 78.910)"
        val dummyDestination = "강남역"
        val dummyTravelTime = "35분"
        val dummyDepartureTime = "오후 2:25"

        eventTitleText.text = "강남에서 친구 만나기"
        originText.text = "출발: $dummyOrigin"
        destinationText.text = "도착: $dummyDestination"
        travelTimeText.text = "예상 소요 시간: $dummyTravelTime"
        departureTimeText.text = "예상 출발 시간: $dummyDepartureTime"
    }
}
