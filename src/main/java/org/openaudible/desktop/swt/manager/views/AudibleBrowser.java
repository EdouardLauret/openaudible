package org.openaudible.desktop.swt.manager.views;

import com.gargoylesoftware.htmlunit.util.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.openaudible.desktop.swt.gui.SWTAsync;

import java.util.ArrayList;
import java.util.Collection;

public class AudibleBrowser {
    public final static Log logger = LogFactory.getLog(AudibleBrowser.class);
    // static ResourceBundle resourceBundle = ResourceBundle.getBundle("examples_browser");
    int index;
    boolean busy;
    Image icon = null;
    boolean title = false;
    Composite parent;
    Text locationBar;
    Browser browser;
    ToolBar toolbar;
    Canvas canvas;
    ToolItem itemBack, itemForward;
    Label status;
    ProgressBar progressBar;
    SWTError error = null;
    Collection<Cookie> cookies;

    public AudibleBrowser(Composite parent, String url) {
        this.parent = parent;
        try {
            browser = new Browser(parent, SWT.BORDER);
            browser.addTitleListener(event -> getShell().setText(event.title));
        } catch (SWTError e) {
            error = e;
            /* Browser widget could not be instantiated */
            parent.setLayout(new FillLayout());
            Label label = new Label(parent, SWT.CENTER | SWT.WRAP);
            label.setText(getResourceString("BrowserNotCreated"));
            // label.requestLayout();
            return;
        }
        initResources();
        if (url.length() > 0)
            browser.setUrl(getResourceString(url));

        if (true)
            show(false, null, null, true, true, true, true);
        else
            show(false, null, null, false, false, false, false);

    }

    /**
     * Gets a string from the resource bundle. We don't want to crash because of a missing String. Returns the key if not found.
     */
    static String getResourceString(String key) {
        switch (key) {
            case "window.title":
                return "Audible";
        }
        return key;
    }

    public static AudibleBrowser newBrowserWindow(final Display display, String url) {
        Shell shell = new Shell(display);
        FillLayout layout = new FillLayout();

        shell.setLayout(layout);
        shell.setText(url);
        shell.setSize(900, 800);
        AudibleBrowser app = new AudibleBrowser(shell, url);
        shell.open();
        return app;
    }

    public static void main(String[] args) {
        Display display = new Display();

        AudibleBrowser app = newBrowserWindow(display, "https://audible.com");

        while (!app.getShell().isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        app.dispose();
        display.dispose();
    }

    /**
     * Disposes of all resources associated with a particular instance of the BrowserApplication.
     */
    public void dispose() {
        freeResources();
    }

    public SWTError getError() {
        return error;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void setShellDecoration(Image icon, boolean title) {
        this.icon = icon;
        this.title = title;
    }

    void show(boolean owned, Point location, Point size, boolean addressBar, boolean menuBar, boolean statusBar, boolean toolBar) {
        final Shell shell = browser.getShell();
        if (owned) {
            if (location != null)
                shell.setLocation(location);
            if (size != null)
                shell.setSize(shell.computeSize(size.x, size.y));
        }
        statusBar = true;

        FormData data = null;
        if (toolBar) {
            toolbar = new ToolBar(parent, SWT.NONE);
            data = new FormData();
            data.top = new FormAttachment(0, 5);
            toolbar.setLayoutData(data);
            itemBack = new ToolItem(toolbar, SWT.PUSH);
            itemBack.setText(getResourceString("Back"));
            itemForward = new ToolItem(toolbar, SWT.PUSH);
            itemForward.setText(getResourceString("Forward"));
            final ToolItem itemStop = new ToolItem(toolbar, SWT.PUSH);
            itemStop.setText(getResourceString("Stop"));
            final ToolItem itemRefresh = new ToolItem(toolbar, SWT.PUSH);
            itemRefresh.setText(getResourceString("Refresh"));
            final ToolItem itemGo = new ToolItem(toolbar, SWT.PUSH);
            itemGo.setText(getResourceString("Go"));

            final ToolItem itemTest = new ToolItem(toolbar, SWT.PUSH);
            itemTest.setText(getResourceString("Test"));

            itemBack.setEnabled(browser.isBackEnabled());
            itemForward.setEnabled(browser.isForwardEnabled());
            Listener listener = event -> {
                ToolItem item = (ToolItem) event.widget;
                if (item == itemBack)
                    browser.back();
                else if (item == itemForward)
                    browser.forward();
                else if (item == itemStop)
                    browser.stop();
                else if (item == itemRefresh)
                    browser.refresh();
                else if (item == itemGo)
                    browser.setUrl(locationBar.getText());
                if (item == itemTest)
                    test();
            };
            itemBack.addListener(SWT.Selection, listener);
            itemForward.addListener(SWT.Selection, listener);
            itemStop.addListener(SWT.Selection, listener);
            itemRefresh.addListener(SWT.Selection, listener);
            itemGo.addListener(SWT.Selection, listener);
            itemTest.addListener(SWT.Selection, listener);

            canvas = new Canvas(parent, SWT.NO_BACKGROUND);
            data = new FormData();
            data.width = 24;
            data.height = 24;
            data.top = new FormAttachment(0, 5);
            data.right = new FormAttachment(100, -5);
            canvas.setLayoutData(data);

            canvas.addListener(SWT.MouseDown, e -> browser.setUrl(getResourceString("Startup")));

        }
        if (addressBar) {
            locationBar = new Text(parent, SWT.BORDER);
            data = new FormData();
            if (toolbar != null) {
                data.top = new FormAttachment(toolbar, 0, SWT.TOP);
                data.left = new FormAttachment(toolbar, 5, SWT.RIGHT);
                data.right = new FormAttachment(canvas, -5, SWT.DEFAULT);
            } else {
                data.top = new FormAttachment(0, 0);
                data.left = new FormAttachment(0, 0);
                data.right = new FormAttachment(100, 0);
            }
            locationBar.setLayoutData(data);
            locationBar.addListener(SWT.DefaultSelection, e -> browser.setUrl(locationBar.getText()));
        }
        if (statusBar) {
            status = new Label(parent, SWT.NONE);
            progressBar = new ProgressBar(parent, SWT.NONE);

            data = new FormData();
            data.left = new FormAttachment(0, 5);
            data.right = new FormAttachment(progressBar, 0, SWT.DEFAULT);
            data.bottom = new FormAttachment(100, -5);
            status.setLayoutData(data);

            data = new FormData();
            data.right = new FormAttachment(100, -5);
            data.bottom = new FormAttachment(100, -5);
            progressBar.setLayoutData(data);

            browser.addStatusTextListener(event -> status.setText(event.text));
        }

		/* Define the function to call from JavaScript */
        new BrowserFunction(browser, "cookieCallback") {
            @Override
            public Object function(Object[] objects) {
                ArrayList<Cookie> list = new ArrayList<Cookie>();
                String u = browser.getUrl();
                if (u.contains("www.audible.com")) {
                    Object[] keyValuePairs = (Object[]) objects[0];
                    for (Object o : keyValuePairs) {
                        Object arr[] = (Object[]) o;
                        Cookie c = new Cookie("www.audible.com", arr[0].toString(), arr[1].toString());
                        list.add(c);
                    }
                    cookies = list;
                } else
                    logger.info("Expected url to include audible, instead: " + u);

                return null;
            }
        };

        parent.setLayout(new FormLayout());

        Control aboveBrowser = toolBar ? canvas : (addressBar ? locationBar : null);
        data = new FormData();
        data.left = new FormAttachment(0, 0);
        data.top = aboveBrowser != null ? new FormAttachment(aboveBrowser, 5, SWT.DEFAULT) : new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = status != null ? new FormAttachment(status, -5, SWT.DEFAULT) : new FormAttachment(100, 0);
        browser.setLayoutData(data);

        if (statusBar || toolBar) {
            browser.addProgressListener(new ProgressListener() {
                @Override
                public void changed(ProgressEvent event) {
                    if (event.total == 0)
                        return;
                    int ratio = event.current * 100 / event.total;
                    if (progressBar != null)
                        progressBar.setSelection(ratio);
                    busy = event.current != event.total;
                    if (!busy) {
                        index = 0;
                        if (canvas != null)
                            canvas.redraw();
                    }
                }

                @Override
                public void completed(ProgressEvent event) {
                    if (progressBar != null)
                        progressBar.setSelection(0);
                    busy = false;
                    index = 0;
                    if (canvas != null) {
                        itemBack.setEnabled(browser.isBackEnabled());
                        itemForward.setEnabled(browser.isForwardEnabled());
                        canvas.redraw();
                    }
                }
            });
        }
        if (addressBar || statusBar || toolBar) {
            browser.addLocationListener(new LocationListener() {
                @Override
                public void changed(LocationEvent event) {
                    busy = true;
                    if (event.top && locationBar != null)
                        locationBar.setText(event.location);
                }

                @Override
                public void changing(LocationEvent event) {
                }
            });
        }

        parent.layout(true);
        if (owned)
            shell.open();
    }

    private void test() {

    }

    /**
     * Grabs input focus
     */
    public void focus() {
        if (locationBar != null)
            locationBar.setFocus();
        else if (browser != null)
            browser.setFocus();
        else
            parent.setFocus();
    }

    /**
     * Frees the resources
     */
    void freeResources() {

    }

    public Collection<Cookie> getCookies() {
        cookies = null;
        SWTAsync.block(new SWTAsync("getCookies") {
                           @Override
                           public void task() {
                               browser.execute("cookieCallback(document.cookie.split( ';' ).map( function( x ) { return x.trim().split( '=' ); } ));");

                           }
                       }
        );

        return cookies;
    }

    /**
     * Loads the resources
     */
    void initResources() {
    }

    private Shell getShell() {
        return browser.getShell();
    }
}