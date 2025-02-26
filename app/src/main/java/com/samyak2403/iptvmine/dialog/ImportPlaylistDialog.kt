package com.samyak2403.iptvmine.dialog

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.screens.ActivityAddPlaylistFromDevice
import com.samyak2403.iptvmine.screens.ActivityImportPlaylistM3U
import com.samyak2403.iptvmine.screens.ActivityImportPlaylistUrl
import com.samyak2403.iptvmine.screens.HowToUseActivity

class ImportPlaylistDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.item_import_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.btnImportUrl).setOnClickListener {
            openActivity(ActivityImportPlaylistUrl::class.java)
        }

        view.findViewById<TextView>(R.id.btnImportM3U).setOnClickListener {
            openActivity(ActivityImportPlaylistM3U::class.java)
        }

        view.findViewById<TextView>(R.id.btnImportDevice).setOnClickListener {
            openActivity(ActivityAddPlaylistFromDevice::class.java)
        }
        view.findViewById<TextView>(R.id.btn_howtouse).setOnClickListener{
            openActivity(HowToUseActivity::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // Đặt dialog ở phía dưới màn hình, sát 3 góc dưới
            val params = window.attributes
            params.gravity = Gravity.BOTTOM
            params.width = WindowManager.LayoutParams.MATCH_PARENT // Chiếm toàn chiều ngang
            window.attributes = params

            // Đảm bảo nền trong suốt để không có khoảng cách thừa
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun openActivity(activityClass: Class<*>) {
        startActivity(Intent(requireContext(), activityClass))
        dismiss()
    }
}
