package himike.win.androidwebrtc;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.webrtc.PeerConnection;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GleasonK on 11/12/15.
 */
public class XirSysRequest extends AsyncTask<Void, Void, List<PeerConnection.IceServer>> {

    public List<PeerConnection.IceServer> doInBackground(Void... params) {
        List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>();
        URL url = null;
        try {
            url = new URL("https://service.xirsys.com/ice?room=default&application=default&domain=himike.win&ident=mikekee&secret=" + BuildConfig.XIR_SYS_KEY + "&secure=1");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null; ) {
                builder.append(line).append("\n");
            }
            JSONTokener tokener = new JSONTokener(builder.toString());
            JSONObject json = new JSONObject(tokener);
            if (json.isNull("e")) {
                JSONArray iceServers = json.getJSONObject("d").getJSONArray("iceServers");
                for (int i = 0; i < iceServers.length(); i++) {
                    JSONObject srv = iceServers.getJSONObject(i);
                    PeerConnection.IceServer is;
                    if (srv.has("username"))
                        is = new PeerConnection.IceServer(srv.getString("url"), srv.getString("username"), srv.getString("credential"));
                    else
                        is = new PeerConnection.IceServer(srv.getString("url"));
                    servers.add(is);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("XIRSYS", "Servers: " + servers.toString());
        return servers;
    }
}
