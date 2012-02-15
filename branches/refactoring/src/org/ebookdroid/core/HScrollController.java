package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

public class HScrollController extends AbstractScrollController {

    public HScrollController(final IActivityController base) {
        super(base, DocumentViewMode.HORIZONTAL_SCROLL);
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

        final int viewX = Math.round(viewState.viewRect.centerX());

        if (viewState.firstVisible != -1) {
            for (final Page page : getBase().getDocumentModel().getPages(viewState.firstVisible,
                    viewState.lastVisible + 1)) {
                final RectF bounds = viewState.getBounds(page);
                final int pageX = Math.round(bounds.centerX());
                final long dist = Math.abs(pageX - viewX);
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
        final int dx = (int) (direction * getWidth() * (scrollheight / 100.0));

        view.startPageScroll(dx, 0);
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

        final int right = lpo != null ? (int) lpo.getBounds(zoom).right - width : 0;
        final int bottom = (int) (height * zoom) - height;

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

        final int height = getHeight();

        if (changedPage == null) {
            float widthAccum = 0;
            for (final Page page : model.getPages()) {
                final float pageWidth = height * page.getAspectRatio();
                final float pageHeight = pageWidth / page.getAspectRatio();
                page.setBounds(new RectF(widthAccum, 0, widthAccum + pageWidth, pageHeight));
                widthAccum += pageWidth + 1;
            }
        } else {
            float widthAccum = changedPage.getBounds(1.0f).left;
            for (final Page page : model.getPages(changedPage.index.viewIndex)) {
                final float pageWidth = height * page.getAspectRatio();
                final float pageHeight = pageWidth / page.getAspectRatio();
                page.setBounds(new RectF(widthAccum, 0, widthAccum + pageWidth, pageHeight));
                widthAccum += pageWidth + 1;
            }
        }
    }
}