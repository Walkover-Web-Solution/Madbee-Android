package com.madbeeapp.android.RemoteControlClient;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RemoteControlClientCompat {

    private static final String TAG = "RemoteControlCompat";

    private static Class sRemoteControlClientClass;

    // RCC short for RemoteControlClient
    private static Method sRCCEditMetadataMethod;
    private static Method sRCCSetPlayStateMethod;
    private static Method sRCCSetTransportControlFlags;

    private static boolean sHasRemoteControlAPIs = false;

    static {
        try {
            ClassLoader classLoader = RemoteControlClientCompat.class.getClassLoader();
            sRemoteControlClientClass = getActualRemoteControlClientClass(classLoader);
            // dynamically populate the playstate and flag values in case they change
            // in future versions.
            for (Field field : RemoteControlClientCompat.class.getFields()) {
                try {
                    Field realField = sRemoteControlClientClass.getField(field.getName());
                    Object realValue = realField.get(null);
                    field.set(null, realValue);
                } catch (NoSuchFieldException e) {
                    Log.w(TAG, "Could not get real field: " + field.getName());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    Log.w(TAG, "Error trying to pull field value for: " + field.getName()
                            + " " + e.getMessage());
                }
            }

            // get the required public methods on RemoteControlClient
            sRCCEditMetadataMethod = sRemoteControlClientClass.getMethod("editMetadata",
                    boolean.class);
            sRCCSetPlayStateMethod = sRemoteControlClientClass.getMethod("setPlaybackState",
                    int.class);
            sRCCSetTransportControlFlags = sRemoteControlClientClass.getMethod(
                    "setTransportControlFlags", int.class);

            sHasRemoteControlAPIs = true;
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException e) {
            // Silently fail when running on an OS before ICS.
        }
    }

    private Object mActualRemoteControlClient;

    public RemoteControlClientCompat(PendingIntent pendingIntent) {
        if (!sHasRemoteControlAPIs) {
            return;
        }
        try {
            mActualRemoteControlClient =
                    sRemoteControlClientClass.getConstructor(PendingIntent.class)
                            .newInstance(pendingIntent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getActualRemoteControlClientClass(ClassLoader classLoader)
            throws ClassNotFoundException {
        return classLoader.loadClass("android.media.RemoteControlClient");
    }

    /**
     * Creates a {@link android.media.RemoteControlClient.MetadataEditor}.
     *
     * @param startEmpty Set to false if you want the MetadataEditor to contain the metadata that
     *                   was previously applied to the RemoteControlClient, or true if it is to be created empty.
     * @return a new MetadataEditor instance.
     */
    public MetadataEditorCompat editMetadata(boolean startEmpty) {
        Object metadataEditor;
        if (sHasRemoteControlAPIs) {
            try {
                metadataEditor = sRCCEditMetadataMethod.invoke(mActualRemoteControlClient,
                        startEmpty);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            metadataEditor = null;
        }
        return new MetadataEditorCompat(metadataEditor);
    }

    /**
     * Sets the current playback state.
     */
    public void setPlaybackState(int state) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetPlayStateMethod.invoke(mActualRemoteControlClient, state);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sets the flags for the media transport control buttons that this client supports.
     */
    public void setTransportControlFlags(int transportControlFlags) {
        if (sHasRemoteControlAPIs) {
            try {
                sRCCSetTransportControlFlags.invoke(mActualRemoteControlClient,
                        transportControlFlags);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final Object getActualRemoteControlClientObject() {
        return mActualRemoteControlClient;
    }

    /**
     * editor, on which you set the metadata for the RemoteControlClient instance. Once all the
     * information has been set, use {@link #apply()} to make it the new metadata that should be
     * displayed for the associated client. Once the metadata has been "applied", you cannot reuse
     * this instance of the MetadataEditor.
     */
    public class MetadataEditorCompat {

        /**
         * The metadata key for the content artwork / album art.
         */
        public final static int METADATA_KEY_ARTWORK = 100;
        private Method mPutStringMethod;
        private Method mPutBitmapMethod;
        private Method mPutLongMethod;
        private Method mClearMethod;
        private Method mApplyMethod;
        private Object mActualMetadataEditor;

        private MetadataEditorCompat(Object actualMetadataEditor) {
            if (sHasRemoteControlAPIs && actualMetadataEditor == null) {
                throw new IllegalArgumentException("Remote Control API's exist, " +
                        "should not be given a null MetadataEditor");
            }
            if (sHasRemoteControlAPIs) {
                Class metadataEditorClass = actualMetadataEditor.getClass();

                try {
                    mPutStringMethod = metadataEditorClass.getMethod("putString",
                            int.class, String.class);
                    mPutBitmapMethod = metadataEditorClass.getMethod("putBitmap",
                            int.class, Bitmap.class);
                    mPutLongMethod = metadataEditorClass.getMethod("putLong",
                            int.class, long.class);
                    mClearMethod = metadataEditorClass.getMethod("clear", new Class[]{});
                    mApplyMethod = metadataEditorClass.getMethod("apply", new Class[]{});
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            mActualMetadataEditor = actualMetadataEditor;
        }

        /**
         * Adds textual information to be displayed.
         * Note that none of the information added after {@link #apply()} has been called,
         * will be displayed.
         */
        public MetadataEditorCompat putString(int key, String value) {
            if (sHasRemoteControlAPIs) {
                try {
                    mPutStringMethod.invoke(mActualMetadataEditor, key, value);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            return this;
        }

        /**
         * Sets the album / artwork picture to be displayed on the remote control.
         *
         * @param key    the identifier of the bitmap to set. The only valid value is
         *               {@link #METADATA_KEY_ARTWORK}
         * @param bitmap The bitmap for the artwork, or null if there isn't any.
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         * calls together.
         * @throws IllegalArgumentException
         * @see Bitmap
         */
        public MetadataEditorCompat putBitmap(int key, Bitmap bitmap) {
            if (sHasRemoteControlAPIs) {
                try {
                    mPutBitmapMethod.invoke(mActualMetadataEditor, key, bitmap);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            return this;
        }

        /**
         * Adds numerical information to be displayed.
         * Note that none of the information added after {@link #apply()} has been called,
         * will be displayed.
         *
         * @param key   the identifier of a the metadata field to set. Valid values are
         *              {@link android.media.MediaMetadataRetriever#METADATA_KEY_CD_TRACK_NUMBER},
         *              {@link android.media.MediaMetadataRetriever#METADATA_KEY_DISC_NUMBER},
         *              {@link android.media.MediaMetadataRetriever#METADATA_KEY_DURATION} (with a value
         *              expressed in milliseconds),
         *              {@link android.media.MediaMetadataRetriever#METADATA_KEY_YEAR}.
         * @param value The long value for the given key
         * @return Returns a reference to the same MetadataEditor object, so you can chain put
         * calls together.
         * @throws IllegalArgumentException
         */
        public MetadataEditorCompat putLong(int key, long value) {
            if (sHasRemoteControlAPIs) {
                try {
                    mPutLongMethod.invoke(mActualMetadataEditor, key, value);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            return this;
        }

        /**
         * Clears all the metadata that has been set since the MetadataEditor instance was
         */
        public void clear() {
            if (sHasRemoteControlAPIs) {
                try {
                    mClearMethod.invoke(mActualMetadataEditor, (Object[]) null);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        /**
         * Associates all the metadata that has been set since the MetadataEditor instance was
         * MetadataEditor cannot be reused to edit the RemoteControlClient's metadata.
         */
        public void apply() {
            if (sHasRemoteControlAPIs) {
                try {
                    mApplyMethod.invoke(mActualMetadataEditor, (Object[]) null);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }
}
