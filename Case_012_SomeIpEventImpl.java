package ts.car.someip.app;
import android.util.Log;
import java.util.ArrayList;
public class SomeIpEventImpl {
    private String tag = SomeIpEventImpl.class.getSimpleName();
    public static class SomeIpEventInfo {
        private boolean mSubscribed;
        private long mTopic;
        private String mDescription;
        private String mInfo = null;
        /**
         * Get the welcome info from server side.
         */
        public SomeIpEventInfo(long topic, boolean subscribed, String desc) {
            mTopic = topic;
            mSubscribed = subscribed;
            mDescription = desc;
        }
        public long getTopic() {
            return mTopic;
        }
        public boolean getSubscribedFlag() {
            return mSubscribed;
        }
        public String getEventName() {
            return mDescription;
        }
        public void setSubscribedFlag(boolean flag) {
            mSubscribed = flag;
        }
        public void setInfos(String info) {
            mInfo = info;
        }
        public String getInfos() {
            return mInfo;
        }
    }
    public interface SomeIpEventListListener {
        void notifyConfigChanged();
    }
    public interface SomeIpNotifyListListener {
        void notifyConfigChanged();
    }
    private ArrayList<SomeIpEventInfo> mArrayOfConfigs = new ArrayList<>();
    private ArrayList<SomeIpEventInfo> mArrayOfNotifys = new ArrayList<>();
    private static SomeIpEventImpl mSomeIpEventImpl;
    private SomeIpEventListListener mListener;
    private SomeIpNotifyListListener mNotifyListener;
    /**
     * Returns SomeIpServerImpl instance.
     * @return SomeIpServerImpl {@link SomeIpEventImpl} instance
     */
    public static SomeIpEventImpl getInstance() {
        if (mSomeIpEventImpl == null) {
            mSomeIpEventImpl = new SomeIpEventImpl();
        }
        return mSomeIpEventImpl;
    }
    /**
     * Register listerner.
     */
    public void setEventListListener(SomeIpEventListListener listener) {
        Log.d(tag, "setServerListListener");
        mListener = listener;
    }
    /**
     * Register listerner.
     */
    public void setNotifyListListener(SomeIpNotifyListListener listener) {
        Log.d(tag, "setNotifyListListener");
        mNotifyListener = listener;
    }
    /**
     * Get server list.
     * @return the list about the avaliable server.
     */
    public ArrayList<SomeIpEventInfo> getEventList() {
        Log.d(tag, "getServerList" + mArrayOfConfigs);
        return mArrayOfConfigs;
    }
    /**
     * Get notify list.
     * @return the list about the avaliable server.
     */
    public ArrayList<SomeIpEventInfo> getNotifyList() {
        Log.d(tag, "getNotifyList" + mArrayOfNotifys);
        return mArrayOfNotifys;
    }
    /**
     * Update event list.
     */
    public void replaceArrayInfo(int position, SomeIpEventInfo eventsInfo) {
        Log.d(tag, "replaceArrayInfo enter");
        if (eventsInfo != null) {
            mArrayOfConfigs.set(position,eventsInfo);
        }
        if (mListener != null) {
            Log.d(tag, "replaceServerInfo mListener is not null, will update ServerList");
            mListener.notifyConfigChanged();
        }
    }
    /**
     * Add event list from String value.
     */
    public void arrayEventFromString(String serverStr) {
        Log.d(tag, "arrayFromString");
        if (serverStr.isEmpty()) {
            Log.d(tag, "arrayFromString String is null");
            return;
        }
        String[] serverString = serverStr.split("\\s*;\\s*");
        for (String eventStrings : serverString) {
            SomeIpEventInfo eventInfo = fromString(eventStrings);
            if (eventInfo != null) {
                mArrayOfConfigs.add(eventInfo);
            }
        }
        if (mListener != null) {
            Log.d(tag, "replaceServerInfo mListener is not null, will update ServerList");
            mListener.notifyConfigChanged();
        }
    }
    /**
     * Add notify list.
     */
    public void addNotifyArray(SomeIpEventInfo eventsInfo) {
        if (eventsInfo != null) {
            mArrayOfNotifys.add(eventsInfo);
        }
        if (mNotifyListener != null) {
            Log.d(tag, "addNotifyArray mListener notify");
            mNotifyListener.notifyConfigChanged();
        }
    }
    /**
     * Remove notify list.
     */
    public void removeNotifyArray(SomeIpEventImpl.SomeIpEventInfo events) {
        if (events != null) {
            ArrayList<SomeIpEventImpl.SomeIpEventInfo> notifyLists = SomeIpEventImpl.getInstance().getNotifyList();
            for (int i = 0; i < notifyLists.size(); i++) {
                if (events.getEventName().equals(notifyLists.get(i).getEventName())) {
                    mArrayOfNotifys.remove(i);
                    break;
                }
            }
            if (mNotifyListener != null) {
                Log.d(tag, "removeNotifyArray mListener notify");
                mNotifyListener.notifyConfigChanged();
            }
        }
    }
    /**
     * Update notify list info.
     */
    public void replaceNotifyArray(int position, SomeIpEventInfo eventsInfo) {
        Log.d(tag, "replaceNotifyArray enter");
        if (eventsInfo != null) {
            mArrayOfNotifys.set(position,eventsInfo);
        }
        if (mNotifyListener != null) {
            Log.d(tag, "replaceNotifyArray mListener notify");
            mNotifyListener.notifyConfigChanged();
        }
    }
    /**
     * Decode the String to get the SomeIpServerInfo.
     */
    private SomeIpEventInfo fromString(String serverStr) {
        if (serverStr == null) {
            return null;
        }
        String[] mserver = serverStr.split("\\s*,\\s*");
        long topic;
        try {
            topic = Long.valueOf(mserver[0]).longValue();
            Log.d(tag, "arrayFromString topic is" + topic);
        } catch (NumberFormatException exception) {
            topic = 0;
        }
        boolean subscribed;
        try {
            subscribed = Boolean.parseBoolean(mserver[1]);
            Log.d(tag, "arrayFromString instance is" + subscribed);
        } catch (NumberFormatException exception) {
            subscribed = false;
        }
        return new SomeIpEventInfo(topic,subscribed,mserver[2]);
    }
}
