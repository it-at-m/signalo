package de.muenchen.appcenter.signalo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs

class SnapshotDisplayFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val selectedSnapshotID = navArgs<SnapshotDisplayFragmentArgs>().value.creationDate
        return inflater.inflate(R.layout.fragment_snapshot_display, container, false)
    }

}