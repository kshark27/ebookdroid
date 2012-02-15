package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

public class VScrollController extends AbstractScrollController {

    public VScrollController(final IActivityController base) {
        super(base, DocumentViewMode.VERTICALL_SCROLL);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState) {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final int viewY = Math.round(viewState.viewRect.centerY());

        if (viewState.firstVisible != -1) {
            for (final Page page : viewState.model.getPages(viewState.firstVisible, viewState.lastVisible + 1)) {
                final RectF bounds = viewState.getBounds(page);
                final int pageY = Math.round(bounds.centerY());
                final long dist = Math.abs(pageY - viewY);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    result = page.index.viewIndex;
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        final int scrollheight = SettingsManager.getAppSettings().getScrollHeight();
        final int dy = (int) (direction * getHeight() * (scrollheight / 100.0));

        view.startPageScroll(0, dy);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getScrollLimits()
     */
    @Override
    public final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();
        final Page lpo = getBase().getDocumentModel().getLastPageObject();
        final float zoom = getBase().getZoomModel().getZoom();

        final int bottom = lpo != null ? (int) lpo.getBounds(zoom).bottom - height : 0;
        final int right = (int) (width * zoom) - width;

        return new Rect(0, 0, right, bottom);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#invalidatePageSizes(org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public synchronized final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized) {
            return;
        }

        if (reason == InvalidateSizeReason.PAGE_ALIGN) {
            return;
        }

        DocumentModel model = getBase().getDocumentModel();
        if (model == null) {
            return;
        }

        final int width = getWidth();

        if (changedPage == null) {
            float heightAccum = 0;
            for (final Page page : model.getPages()) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight + 1;
            }
        } else {
            float heightAccum = changedPage.getBounds(1.0f).top;
            for (final Page page : model.getPages(changedPage.index.viewIndex)) {
                final float pageHeight = width / page.getAspectRatio();
                page.setBounds(new RectF(0, heightAccum, width, heightAccum + pageHeight));
                heightAccum += pageHeight + 1;
            }
        }
    }
}