package com.mcal.studio.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mcal.studio.R;
import com.mcal.studio.adapter.LogsAdapter;
import com.mcal.studio.data.Preferences;
import com.mcal.studio.helper.Constants;
import com.mcal.studio.helper.HyperServer;
import com.mcal.studio.helper.NetworkUtils;
import com.mcal.studio.helper.ProjectManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity to test projects
 */
public class WebActivity extends AppCompatActivity {

    private static final String TAG = WebActivity.class.getSimpleName();

    /**
     * WebView to display project
     */
    @BindView(R.id.web_view)
    WebView webView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.loading_progress)
    ProgressBar loadingProgress;
    String localUrl, localWithoutIndex;
    /**
     * ArrayList for JavaScript logs
     */
    private ArrayList<ConsoleMessage> jsLogs;

    /**
     * Method called when activity is created
     *
     * @param savedInstanceState previously stored state
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String project = getIntent().getStringExtra("appName");
        jsLogs = new ArrayList<>();
        NetworkUtils.setServer(new HyperServer(project));
        super.onCreate(savedInstanceState);

        try {
            NetworkUtils.getServer().start();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        setContentView(R.layout.activity_web);
        ButterKnife.bind(this);

        File indexFile = ProjectManager.getIndexFile(project);
        String indexPath = indexFile.getPath();
        indexPath = indexPath.replace(new File(Constants.PROJECT_ROOT + File.separator + project).getPath(), "");

        toolbar.setTitle(project);
        setSupportActionBar(toolbar);
        webView.getSettings().setJavaScriptEnabled(true);
        localUrl = (NetworkUtils.getServer().wasStarted() && NetworkUtils.getServer().isAlive() && NetworkUtils.getIpAddress() != null)
                ? "http://" + NetworkUtils.getIpAddress() + ":8080" + indexPath
                : getIntent().getStringExtra("localUrl");

        localWithoutIndex = localUrl.substring(0, localUrl.length() - 10);
        webView.loadUrl(localUrl);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                loadingProgress.setProgress(newProgress);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                jsLogs.add(consoleMessage);
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("Alert")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();

                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("Confirm")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();

                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String localUrl) {
                super.onPageFinished(view, localUrl);
                webView.animate().alpha(1);
            }
        });

        toolbar.setSubtitle(localUrl);
    }

    /**
     * Called when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (NetworkUtils.getServer() != null) {
            NetworkUtils.getServer().stop();
        }
    }

    /**
     * Called when menu is created
     *
     * @param menu object that holds menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_web, menu);
        return true;
    }

    /**
     * Called when menu item is selected
     *
     * @param item selected menu item
     * @return true if handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                webView.animate().alpha(0);
                webView.reload();
                return true;
            case R.id.user_agent:
                final int[] selectedI = new int[1];
                final String current = webView.getSettings().getUserAgentString();
                final LinkedList<String> agents = new LinkedList<>(Arrays.asList(Constants.USER_AGENTS));
                if (!agents.contains(current)) agents.add(0, current);
                LinkedList<String> parsedAgents = NetworkUtils.parseUAList(agents);
                new AlertDialog.Builder(this)
                        .setTitle("Change User Agent")
                        .setSingleChoiceItems(parsedAgents.toArray(new String[0]), parsedAgents.indexOf(NetworkUtils.parseUA(current)), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                selectedI[0] = i;
                            }
                        })
                        .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                webView.getSettings().setUserAgentString(agents.get(selectedI[0]));
                            }
                        })
                        .setNeutralButton("RESET", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                webView.getSettings().setUserAgentString(null);
                            }
                        })
                        .setNegativeButton("CUSTOM", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                View rootView = View.inflate(WebActivity.this, R.layout.dialog_input_single, null);
                                final AppCompatEditText fileName = rootView.findViewById(R.id.input_text);
                                fileName.setHint("Custom agent string");
                                fileName.setText(current);
                                new AlertDialog.Builder(WebActivity.this)
                                        .setTitle("Custom User Agent")
                                        .setView(rootView)
                                        .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                webView.getSettings().setUserAgentString(fileName.getText().toString());
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .show();
                            }
                        })
                        .show();
                return true;
            case R.id.web_browser:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(localUrl));
                startActivity(intent);
                return true;
            case R.id.web_logs:
                View layoutLog = View.inflate(this, R.layout.sheet_logs, null);
                if (Preferences.isNightModeEnabled()) {
                    layoutLog.setBackgroundColor(0xFF333333);
                }

                RecyclerView logsList = layoutLog.findViewById(R.id.logs_list);
                LinearLayoutManager manager = new LinearLayoutManager(this);
                RecyclerView.Adapter adapter = new LogsAdapter(localWithoutIndex, jsLogs);

                logsList.setLayoutManager(manager);
                logsList.addItemDecoration(new DividerItemDecoration(WebActivity.this, manager.getOrientation()));
                logsList.setAdapter(adapter);

                BottomSheetDialog dialogLog = new BottomSheetDialog(this);
                dialogLog.setContentView(layoutLog);
                dialogLog.show();
                return true;
            case R.id.web_settings:
                View layout = View.inflate(this, R.layout.sheet_web_settings, null);
                if (Preferences.isNightModeEnabled()) {
                    layout.setBackgroundColor(0xFF333333);
                }

                SwitchCompat allowContentAccess, allowFileAccess, allowFileAccessFromFileURLs, allowUniversalAccessFromFileURLs, blockNetworkImage, blockNetworkLoads, builtInZoomControls, database, displayZoomControls, domStorage, jsCanOpenWindows, js, loadOverview, imageLoad, saveForm, wideView;
                allowContentAccess = layout.findViewById(R.id.allow_content_access);
                allowFileAccess = layout.findViewById(R.id.allow_file_access);
                allowFileAccessFromFileURLs = layout.findViewById(R.id.allow_file_access_from_file_urls);
                allowUniversalAccessFromFileURLs = layout.findViewById(R.id.allow_universal_access_from_file_urls);
                blockNetworkImage = layout.findViewById(R.id.block_network_image);
                blockNetworkLoads = layout.findViewById(R.id.block_network_loads);
                builtInZoomControls = layout.findViewById(R.id.built_in_zoom_controls);
                database = layout.findViewById(R.id.database_enabled);
                displayZoomControls = layout.findViewById(R.id.display_zoom_controls);
                domStorage = layout.findViewById(R.id.dom_storage_enabled);
                jsCanOpenWindows = layout.findViewById(R.id.javascript_can_open_windows_automatically);
                js = layout.findViewById(R.id.javascript_enabled);
                loadOverview = layout.findViewById(R.id.load_with_overview_mode);
                imageLoad = layout.findViewById(R.id.loads_images_automatically);
                saveForm = layout.findViewById(R.id.save_form_data);
                wideView = layout.findViewById(R.id.use_wide_view_port);

                allowContentAccess.setChecked(webView.getSettings().getAllowContentAccess());
                allowFileAccess.setChecked(webView.getSettings().getAllowFileAccess());
                blockNetworkImage.setChecked(webView.getSettings().getBlockNetworkImage());
                blockNetworkLoads.setChecked(webView.getSettings().getBlockNetworkLoads());
                builtInZoomControls.setChecked(webView.getSettings().getBuiltInZoomControls());
                database.setChecked(webView.getSettings().getDatabaseEnabled());
                displayZoomControls.setChecked(webView.getSettings().getDisplayZoomControls());
                domStorage.setChecked(webView.getSettings().getDomStorageEnabled());
                jsCanOpenWindows.setChecked(webView.getSettings().getJavaScriptCanOpenWindowsAutomatically());
                js.setChecked(webView.getSettings().getJavaScriptEnabled());
                loadOverview.setChecked(webView.getSettings().getLoadWithOverviewMode());
                imageLoad.setChecked(webView.getSettings().getLoadsImagesAutomatically());
                saveForm.setChecked(webView.getSettings().getSaveFormData());
                wideView.setChecked(webView.getSettings().getUseWideViewPort());

                allowContentAccess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setAllowContentAccess(isChecked);
                    }
                });

                allowFileAccess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setAllowFileAccess(isChecked);
                    }
                });

                blockNetworkImage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setBlockNetworkImage(isChecked);
                    }
                });

                blockNetworkLoads.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setBlockNetworkLoads(isChecked);
                    }
                });

                builtInZoomControls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setBuiltInZoomControls(isChecked);
                    }
                });

                database.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setDatabaseEnabled(isChecked);
                    }
                });

                displayZoomControls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setDisplayZoomControls(isChecked);
                    }
                });

                domStorage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setDomStorageEnabled(isChecked);
                    }
                });

                jsCanOpenWindows.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(isChecked);
                    }
                });

                js.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setJavaScriptEnabled(isChecked);
                    }
                });

                loadOverview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setLoadWithOverviewMode(isChecked);
                    }
                });

                imageLoad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setLoadsImagesAutomatically(isChecked);
                    }
                });

                saveForm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setSaveFormData(isChecked);
                    }
                });

                wideView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        webView.getSettings().setUseWideViewPort(isChecked);
                    }
                });


                if (Build.VERSION.SDK_INT >= 16) {
                    allowFileAccessFromFileURLs.setChecked(webView.getSettings().getAllowFileAccessFromFileURLs());
                    allowUniversalAccessFromFileURLs.setChecked(webView.getSettings().getAllowUniversalAccessFromFileURLs());
                    allowFileAccessFromFileURLs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            webView.getSettings().setAllowFileAccessFromFileURLs(isChecked);
                        }
                    });

                    allowUniversalAccessFromFileURLs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            webView.getSettings().setAllowUniversalAccessFromFileURLs(isChecked);
                        }
                    });
                } else {
                    allowFileAccessFromFileURLs.setVisibility(View.GONE);
                    allowUniversalAccessFromFileURLs.setVisibility(View.GONE);
                }

                BottomSheetDialog dialog = new BottomSheetDialog(this);
                dialog.setContentView(layout);
                dialog.show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
