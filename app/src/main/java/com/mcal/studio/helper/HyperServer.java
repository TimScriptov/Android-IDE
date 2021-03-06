package com.mcal.studio.helper;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import fi.iki.elonen.NanoHTTPD;

/**
 * Web server class using NanoHTTPD
 */
public class HyperServer extends NanoHTTPD {

    /**
     * Log TAG
     */
    private static final String TAG = HyperServer.class.getSimpleName();

    /**
     * File types and respective mimes
     */
    private final String[] mTypes = {"css", "js", "ico", "png", "jpg", "jpe", "svg", "bm", "gif", "ttf", "otf", "woff", "woff2", "eot", "sfnt"};
    private final String[] mMimes = {"text/css", "text/js", "image/x-icon", "image/png", "image/jpg", "image/jpeg", "image/svg+xml", "image/bmp", "image/gif", "application/x-font-ttf", "application/x-font-opentype", "application/font-woff", "application/font-woff2", "application/vnd.ms-fontobject", "application/font-sfnt"};

    /**
     * ProjectManager to host web server for
     */
    private String mProject;

    /**
     * public Constructor
     *
     * @param project to host server for
     */
    public HyperServer(String project) {
        super(8080);
        mProject = project;
    }

    /**
     * Serving files on server
     *
     * @param session not sure what this is
     * @return response
     */
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String mimeType = getMimeType(uri);

        if (uri.equals("/")) {
            File indexFile = ProjectManager.getIndexFile(mProject);
            String indexPath = indexFile.getPath();
            indexPath = indexPath.replace(new File(Constants.PROJECT_ROOT + File.separator + mProject).getPath(), "");
            uri = File.separator + indexPath;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(Constants.PROJECT_ROOT + File.separator + mProject + uri);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        try {
            return newFixedLengthResponse(Response.Status.OK, mimeType, IOUtils.toString(inputStream, Charset.defaultCharset()));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return newFixedLengthResponse(e.toString());
        }
    }

    /**
     * Get mimetype from uri
     *
     * @param uri of file
     * @return file mimetype
     */
    private String getMimeType(String uri) {
        for (int i = 0; i < mTypes.length; i++) {
            if (uri.endsWith("." + mTypes[i])) {
                return mMimes[i];
            }
        }

        return NanoHTTPD.MIME_HTML;
    }
}
