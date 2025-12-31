package org.schabi.newpipe.fragments.list.kiosk

import android.os.Bundle
import android.view.View
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.util.TournesolHelper

class TournesolKioskFragment : KioskFragment() {
    private var tournesolFilterController: TournesolFilterController? = null

    override fun initViews(rootView: View?, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)
        if (rootView == null) {
            return
        }
        if (tournesolFilterController == null) {
            tournesolFilterController = TournesolFilterController(
                this,
                object : TournesolFilterController.Listener {
                    override fun onFiltersChanged(languages: List<String>, dateKey: String) {
                        onTournesolFiltersChanged(languages, dateKey)
                    }
                }
            )
        }
        tournesolFilterController?.init(rootView)
        val controller = tournesolFilterController ?: return
        applyTournesolFilters(controller.getCurrentLanguages(), controller.getCurrentDateKey(), false)
    }

    override fun onResume() {
        super.onResume()
        tournesolFilterController?.onResume()
    }

    override fun onDestroyView() {
        tournesolFilterController?.onDestroyView()
        tournesolFilterController = null
        super.onDestroyView()
    }

    private fun onTournesolFiltersChanged(languages: List<String>, dateKey: String) {
        applyTournesolFilters(languages, dateKey, true)
    }

    private fun applyTournesolFilters(
        languages: List<String>,
        dateKey: String,
        reload: Boolean
    ) {
        url = TournesolHelper.buildTournesolUrl(languages, dateKey)
        if (!reload) {
            return
        }
        currentInfo = null
        currentNextPage = null
        currentWorker?.dispose()
        reloadContent()
    }

    companion object {
        @JvmStatic
        @Throws(ExtractionException::class)
        fun getInstance(serviceId: Int, kioskId: String): TournesolKioskFragment {
            val instance = TournesolKioskFragment()
            val service: StreamingService = NewPipe.getService(serviceId)
            val kioskLinkHandlerFactory: ListLinkHandlerFactory =
                service.kioskList.getListLinkHandlerFactoryByType(kioskId)
            instance.setInitialData(
                serviceId,
                kioskLinkHandlerFactory.fromId(kioskId).url,
                kioskId
            )
            instance.kioskId = kioskId
            return instance
        }
    }
}
