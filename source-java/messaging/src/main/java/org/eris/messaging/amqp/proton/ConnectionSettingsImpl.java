package org.eris.messaging.amqp.proton;

import org.eris.messaging.ConnectionSettings;

public class ConnectionSettingsImpl extends ConnectionSettings
{
    boolean _passive = false;

    ConnectionSettingsImpl(String host, int port)
    {
        _host = host;
        _port = port;
    }

    ConnectionSettingsImpl(String url)
    {
        parse(url);
    }

    //TODO error handling
    private void parse(String url)
    {
        int start = 0;
        int schemeEnd = url.indexOf("://", start);
        if (schemeEnd >= 0)
        {
            _scheme = url.substring(start, schemeEnd);
            start = schemeEnd + 3;
        }

        String uphp = url.substring(start);

        String hp;
        int at = uphp.indexOf('@');
        if (at >= 0)
        {
            String up = uphp.substring(0, at);
            hp = uphp.substring(at + 1);

            int colon = up.indexOf(':');
            if (colon >= 0)
            {
                _user = up.substring(0, colon);
                _pass = up.substring(colon + 1);
            }
            else
            {
                _user = up;
            }
        }
        else
        {
            hp = uphp;
        }

        if (hp.startsWith("["))
        {
            int close = hp.indexOf(']');
            if (close >= 0)
            {
                _host = hp.substring(1, close);
                if (hp.substring(close + 1).startsWith(":"))
                {
                    _port = Integer.parseInt(hp.substring(close + 2));
                }
            }
        }

        if (_host == null)
        {
            int colon = hp.indexOf(':');
            if (colon >= 0)
            {
                _host = hp.substring(0, colon);
                _port = Integer.parseInt(hp.substring(colon + 1));
            }
            else
            {
                _host = hp;
            }
        }

        if (_host.startsWith("~"))
        {
            _host = _host.substring(1);
            _passive = true;
        }
    }
}