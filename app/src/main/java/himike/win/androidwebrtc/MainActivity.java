package himike.win.androidwebrtc;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * just a demo to show the webrtc how to work
 */
public class MainActivity extends AppCompatActivity implements SdpObserver, PeerConnection.Observer {
    GLSurfaceView mGLSurfaceView;
    PeerConnection pc;
    PeerConnectionFactory factory;
    PubNub pubnub;
    Button switchCamera;
    Button send;
    VideoCapturerAndroid videoCapturer;
    boolean isSend = false;

    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    public List<PeerConnection.IceServer> getXirSysIceServers() {
        List<PeerConnection.IceServer> servers = new ArrayList<>();
        try {
            servers = new XirSysRequest().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return servers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface);
        mGLSurfaceView.setKeepScreenOn(true);
        switchCamera = (Button) findViewById(R.id.switch_camera);
        send = (Button) findViewById(R.id.send);

        List<PeerConnection.IceServer> iceServers = getXirSysIceServers();

        // 0. initializeAndroidGlobals
        PeerConnectionFactory.initializeAndroidGlobals(
                this,//上下文，可自定义监听
                true,//是否初始化音频，布尔值
                true,//是否初始化视频，布尔值
                true);//是否支持硬件加速，布尔值

        // 1. create PeerConnectionFactory
        factory = new PeerConnectionFactory();

        // 2. get VideoSource and AudioSource
        String frontCameraName = CameraUtils.getNameOfFrontFacingDevice();
        videoCapturer = VideoCapturerAndroid.create(frontCameraName, null);

        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1280"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "720"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "640"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "480"));

        VideoSource videoSource = factory.createVideoSource(videoCapturer, videoConstraints);
        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());

        // 3. get VideoTrack and AudioTrack，the id can be any string that uniquely
        // identifies that track in your application
        VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);

        // 4. add renderer
        VideoRendererGui.setView(mGLSurfaceView, null);
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        // just add local,the remoteRenderer will be added when remoteRender is ready in onAddStream
        videoTrack.addRenderer(new VideoRenderer(localRender));

        // 5. get local MediaStream, label can be any string, now you can see yourself
        MediaStream localMS = factory.createLocalMediaStream("ARDAMS");
        localMS.addTrack(videoTrack);
        localMS.addTrack(audioTrack);

        // List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        // iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        // iceServers.add(new PeerConnection.IceServer("stun.voipbuster.com"));
        // iceServers.add(new PeerConnection.IceServer("stun.wirlab.net"));

        final MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));


        // 6. PeerConnection , connect to the other peer
        pc = factory.createPeerConnection(
                iceServers,//ICE servers
                pcConstraints,//MediaConstraints
                this);//observer : SdpObserver, PeerConnection.Observer

        // 7. add your stream to connection to let others see you, but nobody can, now
        pc.addStream(localMS);

        // other(not webrtc). use PubNub to exchange data, you can use any other utils to do it the same way
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(BuildConfig.PUBNUB_SUBSCRIBE_KEY);
        pnConfiguration.setPublishKey(BuildConfig.PUBNUB_PUBLISH_KEY);
        pnConfiguration.setSecure(false);

        pubnub = new PubNub(pnConfiguration);
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                Logger.d("status in listener:" + status);
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                // exchange Description and IceCandidate
                Logger.d("message:" + message.getPublisher() + "\n" + message.getChannel() + " " + isSend);
                if (isSend) {
                    if (message.getChannel().equals("frost")) {
                        MessageDto dto = new Gson().fromJson(message.getMessage(), MessageDto.class);
                        if (dto.getType().equals("ice")) {
                            pc.addIceCandidate(dto.getCandidate());
                        } else {
                            pc.setRemoteDescription(MainActivity.this, dto.getDescription());
                        }
                    }
                } else {
                    if (message.getChannel().equals("mike")) {
                        Logger.d("set from remote");
                        MessageDto dto = new Gson().fromJson(message.getMessage(), MessageDto.class);
                        if (dto.getType().equals("ice")) {
                            pc.addIceCandidate(dto.getCandidate());
                        } else {
                            pc.setRemoteDescription(MainActivity.this, dto.getDescription());
                            pc.createAnswer(MainActivity.this, pcConstraints);
                        }
                    }
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });
        // subscribe to two channel
        pubnub.subscribe()
                .channels(Arrays.asList("mike", "frost")) // subscribe to channels
                .execute();

        // 8. createOffer, when success, setLocalDescription and send it to the other peer
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSend = true;
                pc.createOffer(MainActivity.this, pcConstraints);
            }
        });
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoCapturer.switchCamera(new VideoCapturerAndroid.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(boolean b) {

                    }

                    @Override
                    public void onCameraSwitchError(String s) {

                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pc.dispose();
        factory.dispose();
        pubnub.disconnect();
        pubnub.destroy();
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Logger.d("onSignalingChange:" + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Logger.d("onIceConnectionChange:" + iceConnectionState.toString());
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Logger.d("onIceConnectionReceivingChange:" + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Logger.d("onIceGatheringChange:" + iceGatheringState);
    }

    /**
     * 9. exchange IceCandidate
     *
     * @param iceCandidate
     */
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Logger.d("onIceCandidate:" + iceCandidate.toString());
        pubnub.publish().channel(isSend ? "mike" : "frost").message(new MessageDto(iceCandidate, "ice")).async(new PNCallback<PNPublishResult>() {
            @Override
            public void onResponse(PNPublishResult result, PNStatus status) {

            }
        });
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        Logger.d("onAddStream：" + mediaStream.videoTracks.size());
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED, scalingType, true);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        pc.removeStream(mediaStream);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Logger.d("onDataChannel:" + dataChannel);
    }

    @Override
    public void onRenegotiationNeeded() {

    }

    /////////////////////SdpObserver//////////////////////////////////

    /**
     * 8. createOffer and createAnswer success, peer to peer will exchange SessionDescription here
     *
     * @param sessionDescription
     */
    @Override
    public void onCreateSuccess(final SessionDescription sessionDescription) {
        Logger.d("onCreateSuccess:" + sessionDescription.description);
        pc.setLocalDescription(this, sessionDescription);
        pubnub.publish().channel(isSend ? "mike" : "frost").message(new MessageDto(sessionDescription, "des")).async(new PNCallback<PNPublishResult>() {
            @Override
            public void onResponse(PNPublishResult result, PNStatus status) {
                Logger.d(result + " " + status);
            }
        });
    }

    @Override
    public void onSetSuccess() {
        Logger.d("onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        Logger.d("onCreateFailure:" + s);
    }

    @Override
    public void onSetFailure(String s) {
        Logger.d("onSetFailure:" + s);
    }
    /////////////////////////////////////////////////////
}
