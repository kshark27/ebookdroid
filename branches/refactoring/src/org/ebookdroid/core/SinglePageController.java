package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.curl.PageAnimator;
import org.ebookdroid.core.curl.PageAnimatorProxy;
import org.ebookdroid.core.curl.SinglePageView;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.touch.DefaultGestureDetector;
import org.ebookdroid.core.touch.IGestureDetector;
import org.ebookdroid.core.touch.IMultiTouchZoom;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

import org.emdev.ui.hwa.IHardwareAcceleration;

/**
 * The Class SinglePageController.
 * 
 * Used in single page view mode
 */
public class SinglePageController extends AbstractViewController {

    /** The curler. */
    private final PageAnimatorProxy curler = new PageAnimatorProxy(new SinglePageView(this));

    /**
     * Instantiates a new single page document view.
     * 
     * @param baseActivity
     *            the base activity
     */
    public SinglePageController(final IActivityController baseActivity) {
        super(baseActivity, DocumentViewMode.SINGLE_PAGE);
        updateAnimationType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#goToPageImpl(int)
     */
    @Override
    public final void goToPage(final int toPage) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            dm.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);

            final ViewState viewState = new EventScrollTo(this, page.index.viewIndex).process();
            view.redrawView(viewState);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#goToPageImpl(int, float, float)
     */
    @Override
    public void goToPage(final int toPage, final float offsetX, final float offsetY) {
        final DocumentModel dm = getBase().getDocumentModel();
        if (toPage >= 0 && toPage < dm.getPageCount()) {
            final Page page = dm.getPageObject(toPage);
            dm.setCurrentPageIndex(page.index);
            curler.setViewDrawn(false);
            curler.resetPageIndexes(page.index.viewIndex);

            final RectF bounds = page.getBounds(getBase().getZoomModel().getZoom());
            final float left = bounds.left + offsetX * bounds.width();
            final float top = bounds.top + offsetY * bounds.height();
            view.scrollTo((int) left, (int) top);

            final ViewState viewState = new EventScrollTo(this, page.index.viewIndex).process();
            view.redrawView(viewState);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#onScrollChanged(int, int)
     */
    @Override
    public void onScrollChanged(final int dX, final int dY) {
        // bounds could be not updated
        if (inZoom.get()) {
            return;
        }

        AbstractEventScroll cmd = dX > 0 ? new EventScrollDown(this) : new EventScrollUp(this);
        final ViewState viewState = cmd.process();

        if (cmd.model != null) {
            final Page page = cmd.model.getCurrentPageObject();
            if (page != null) {
                updatePosition(cmd.model, page, viewState);
            }
        }
        view.redrawView(viewState);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState) {
        return getBase().getDocumentModel().getCurrentViewPageIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#verticalConfigScroll(int)
     */
    @Override
    public final void verticalConfigScroll(final int direction) {
        curler.animate(direction);
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
        final float zoom = getBase().getZoomModel().getZoom();

        final DocumentModel dm = getBase().getDocumentModel();
        final Page page = dm != null ? dm.getCurrentPageObject() : null;

        if (page != null) {
            final RectF bounds = page.getBounds(zoom);
            final int top = ((int) bounds.top > 0) ? 0 : (int) bounds.top;
            final int left = ((int) bounds.left > 0) ? 0 : (int) bounds.left;
            final int bottom = ((int) bounds.bottom < height) ? 0 : (int) bounds.bottom - height;
            final int right = ((int) bounds.right < width) ? 0 : (int) bounds.right - width;

            return new Rect(left, top, right, bottom);
        }

        return new Rect(0, 0, 0, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#initGestureDetectors(java.util.List)
     */
    @Override
    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        list.add(IMultiTouchZoom.Factory.createImpl(base.getZoomModel()));
        list.add(curler);
        list.add(new DefaultGestureDetector(base.getContext(), new GestureListener()));
        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#drawView(org.ebookdroid.core.EventDraw)
     */
    @Override
    public void drawView(EventDraw eventDraw) {
        curler.draw(eventDraw);
    }

    public final ViewState invalidatePages(final ViewState oldState, final Page... pages) {
        return new EventScrollTo(this, pages[0].index.viewIndex).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#invalidatePageSizes(org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isShown()) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();

        if (changedPage == null) {
            for (final Page page : getBase().getDocumentModel().getPages()) {
                invalidatePageSize(page, width, height);
            }
        } else {
            invalidatePageSize(changedPage, width, height);
        }

        curler.setViewDrawn(false);
    }

    private void invalidatePageSize(final Page page, final int width, final int height) {
        final BookSettings bookSettings = SettingsManager.getBookSettings();
        if (bookSettings == null) {
            return;
        }
        PageAlign effectiveAlign = bookSettings.pageAlign;
        if (effectiveAlign == null) {
            effectiveAlign = PageAlign.WIDTH;
        } else if (effectiveAlign == PageAlign.AUTO) {
            final float pageHeight = width / page.getAspectRatio();
            effectiveAlign = pageHeight > height ? PageAlign.HEIGHT : PageAlign.WIDTH;
        }

        if (effectiveAlign == PageAlign.WIDTH) {
            final float pageHeight = width / page.getAspectRatio();
            page.setBounds(new RectF(0, 0, width, pageHeight));
        } else {
            final float pageWidth = height * page.getAspectRatio();
            final float widthDelta = (width - pageWidth) / 2;
            page.setBounds(new RectF(widthDelta, 0, pageWidth + widthDelta, height));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#isPageVisible(org.ebookdroid.core.Page,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    public final boolean isPageVisible(final Page page, final ViewState viewState) {
        return curler.isPageVisible(page, viewState);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#updateAnimationType()
     */
    @Override
    public final void updateAnimationType() {
        final PageAnimationType animationType = SettingsManager.getBookSettings().animationType;
        final PageAnimator newCurler = PageAnimationType.create(animationType, this);

        IHardwareAcceleration.Factory.getInstance().setMode(getView().getView(),
                SettingsManager.getAppSettings().isHWAEnabled(), animationType.isHardwareAccelSupported());

        newCurler.init();
        curler.switchCurler(newCurler);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#pageUpdated(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.Page)
     */
    @Override
    public void pageUpdated(final ViewState viewState, final Page page) {
        curler.pageUpdated(viewState, page);
    }
}