package org.ebookdroid.core.models;

import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.EventDispatcher;
import org.ebookdroid.core.log.LogContext;

public class CurrentPageModel extends EventDispatcher {

    private static final LogContext LCTX = LogContext.ROOT.lctx("DocModel");

    private int currentDocPageIndex;

    private int currentViewPageIndex;

    public void setCurrentPageIndex(final int currentDocPageIndex, final int currentViewPageIndex) {
        if (this.currentViewPageIndex != currentViewPageIndex) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("Current page changed: " + "[" + this.currentDocPageIndex + ", "
                        + this.currentViewPageIndex + "]" + " -> " + "[" + currentDocPageIndex + ", "
                        + currentViewPageIndex + "]");
            }

            this.currentDocPageIndex = currentDocPageIndex;
            this.currentViewPageIndex = currentViewPageIndex;

            dispatch(new CurrentPageListener.CurrentPageChangedEvent(currentDocPageIndex, currentViewPageIndex));
        }
    }

    public int getCurrentViewPageIndex() {
        return this.currentViewPageIndex;
    }

    public int getCurrentDocPageIndex() {
        return currentDocPageIndex;
    }
}