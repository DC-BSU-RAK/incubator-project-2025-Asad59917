package com.example.car_service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingBar: ProgressBar
    private lateinit var countBadge: TextView
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseHelper = FirebaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerNotifications)
        emptyState = view.findViewById(R.id.emptyState)
        loadingBar = view.findViewById(R.id.loadingBar)
        countBadge = view.findViewById(R.id.countBadge)

        adapter = NotificationAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        loadingBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            val isPremium = firebaseHelper.isPremium()
            val all = firebaseHelper.getAllNotifications()

            // Filter notifications based on audience: 'all', 'pro', or 'free'
            val visible = all.filter { notif ->
                when (notif.audience) {
                    "pro" -> isPremium
                    "free" -> !isPremium
                    else -> true  // "all" or anything else = show to everyone
                }
            }

            loadingBar.visibility = View.GONE

            if (visible.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                countBadge.text = "0"
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateNotifications(visible)
                countBadge.text = visible.size.toString()
            }
        }
    }
}