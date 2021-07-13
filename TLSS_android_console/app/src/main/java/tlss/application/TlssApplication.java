package tlss.application;

import android.app.Application;

import tlss.controller.TlssNetTransportServer;
import tlss.controller.TlssTransceiver;

public class TlssApplication extends Application {
    private Settings settings = null;
    private TlssTransceiver tlssTransceiver = null;
    private TlssNetTransportServer netTransportServer = null;

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setTlssTransceiver(TlssTransceiver tlssTransceiver) {
        this.tlssTransceiver = tlssTransceiver;
    }

    public void setNetTransportServer(TlssNetTransportServer netTransportServer) {
        this.netTransportServer = netTransportServer;
    }

    public Settings getSettings() {
        return settings;
    }

    public TlssTransceiver getTlssTransceiver() {
        return tlssTransceiver;
    }

    public TlssNetTransportServer getNetTransportServer() {
        return netTransportServer;
    }
}