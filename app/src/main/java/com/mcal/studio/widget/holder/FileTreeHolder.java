package com.mcal.studio.widget.holder;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mcal.studio.R;
import com.mcal.studio.helper.Clipboard;
import com.mcal.studio.helper.ResourceHelper;
import com.unnamed.b.atv.model.TreeNode;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class FileTreeHolder extends TreeNode.BaseNodeViewHolder<FileTreeHolder.FileTreeItem> {

    private static final String TAG = FileTreeHolder.class.getSimpleName();

    private ImageView arrow;

    public FileTreeHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, final FileTreeItem value) {
        View view = View.inflate(context, R.layout.item_file_browser, null);
        final TextView nodeFile = view.findViewById(R.id.file_browser_name);
        final ImageView fileIcon = view.findViewById(R.id.file_browser_icon);
        arrow = view.findViewById(R.id.file_browser_arrow);
        final ImageButton overflow = view.findViewById(R.id.file_browser_options);

        nodeFile.setText(value.file.getName());
        fileIcon.setImageResource(value.icon);

        if (node.isLeaf()) {
            arrow.setVisibility(View.GONE);
        }

        if (!node.isExpanded()) {
            arrow.setRotation(-90);
        }

        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final File file = new File(value.file.getPath());
                final PopupMenu menu = new PopupMenu(context, overflow);
                menu.getMenuInflater().inflate(R.menu.menu_file_options, menu.getMenu());
                if (file.isFile()) {
                    menu.getMenu().findItem(R.id.action_new).setVisible(false);
                    menu.getMenu().findItem(R.id.action_paste).setVisible(false);
                    if (file.getName().equals("build.gradle")) {
                        menu.getMenu().findItem(R.id.action_rename).setVisible(false);
                    }
                } else {
                    menu.getMenu().findItem(R.id.action_paste).setEnabled(Clipboard.getInstance().getCurrentFile() != null);
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_new_file:
                                View newFileRootView = View.inflate(context, R.layout.dialog_input_single, null);
                                final TextInputEditText fileName = newFileRootView.findViewById(R.id.input_text);
                                fileName.setHint(R.string.file_name);

                                final AlertDialog newFileDialog = new AlertDialog.Builder(context)
                                        .setTitle("New file")
                                        .setView(newFileRootView)
                                        .setPositiveButton(R.string.create, null)
                                        .setNegativeButton(R.string.cancel, null)
                                        .create();

                                newFileDialog.show();
                                newFileDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (fileName.getText().toString().isEmpty()) {
                                            fileName.setError("Please enter a file name");
                                        } else {
                                            newFileDialog.dismiss();
                                            String fileStr = fileName.getText().toString();
                                            File newFile = new File(file, fileStr);
                                            try {
                                                FileUtils.writeStringToFile(newFile, "\n", Charset.defaultCharset());
                                            } catch (IOException e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }

                                            Snackbar.make(value.view, "Created " + fileStr + ".", Snackbar.LENGTH_SHORT).show();
                                            TreeNode newFileNode = new TreeNode(new FileTreeItem(ResourceHelper.getIcon(newFile), newFile, value.view));
                                            node.addChild(newFileNode);
                                            arrow.setVisibility(View.VISIBLE);
                                            tView.expandNode(node);
                                        }
                                    }
                                });

                                return true;
                            case R.id.action_new_folder:
                                View newFolderRootView = View.inflate(context, R.layout.dialog_input_single, null);
                                final TextInputEditText folderName = newFolderRootView.findViewById(R.id.input_text);
                                folderName.setHint(R.string.folder_name);

                                final AlertDialog newFolderDialog = new AlertDialog.Builder(context)
                                        .setTitle("New folder")
                                        .setView(newFolderRootView)
                                        .setPositiveButton(R.string.create, null)
                                        .setNegativeButton(R.string.cancel, null)
                                        .create();

                                newFolderDialog.show();
                                newFolderDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (folderName.getText().toString().isEmpty()) {
                                            folderName.setError("Please enter a folder name");
                                        } else {
                                            newFolderDialog.dismiss();
                                            String folderStr = folderName.getText().toString();
                                            File newFolder = new File(file, folderStr);
                                            try {
                                                FileUtils.forceMkdir(newFolder);
                                            } catch (IOException e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }

                                            Snackbar.make(value.view, "Created " + folderStr + ".", Snackbar.LENGTH_SHORT).show();
                                            TreeNode newFolderNode = new TreeNode(new FileTreeItem(R.drawable.ic_folder, newFolder, value.view));
                                            node.addChild(newFolderNode);
                                            arrow.setVisibility(View.VISIBLE);
                                            tView.expandNode(node);
                                        }
                                    }
                                });

                                return true;
                            case R.id.action_rename:
                                View renameRootView = View.inflate(context, R.layout.dialog_input_single, null);
                                final TextInputEditText renameTo = renameRootView.findViewById(R.id.input_text);
                                renameTo.setHint(R.string.new_name);
                                renameTo.setText(value.file.getName());

                                final AlertDialog renameDialog = new AlertDialog.Builder(context)
                                        .setTitle("Rename " + value.file.getName())
                                        .setView(renameRootView)
                                        .setPositiveButton("RENAME", null)
                                        .setNegativeButton(R.string.cancel, null)
                                        .create();

                                renameDialog.show();
                                renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (renameTo.getText().toString().isEmpty()) {
                                            renameTo.setError("Please enter a name");
                                        } else {
                                            renameDialog.dismiss();
                                            String renameStr = renameTo.getText().toString();
                                            File rename = new File(file.getPath().replace(file.getName(), renameStr));
                                            if (!file.isDirectory()) {
                                                try {
                                                    FileUtils.moveFile(file, rename);
                                                } catch (IOException e) {
                                                    Log.e(TAG, e.toString());
                                                    Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                try {
                                                    FileUtils.moveDirectory(file, rename);
                                                } catch (IOException e) {
                                                    Log.e(TAG, e.toString());
                                                    Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                                }
                                            }

                                            Snackbar.make(value.view, "Renamed " + value.file.getName() + " to " + renameStr + ".", Snackbar.LENGTH_SHORT).show();
                                            value.file = rename;
                                            value.icon = ResourceHelper.getIcon(rename);
                                            nodeFile.setText(value.file.getName());
                                            fileIcon.setImageResource(value.icon);
                                        }
                                    }
                                });

                                return true;
                            case R.id.action_copy:
                                Clipboard.getInstance().setCurrentFile(file);
                                Clipboard.getInstance().setCurrentNode(node);
                                Clipboard.getInstance().setType(Clipboard.Type.COPY);
                                Snackbar.make(value.view, value.file.getName() + " selected to be copied.", Snackbar.LENGTH_SHORT).show();
                                return true;
                            case R.id.action_cut:
                                Clipboard.getInstance().setCurrentFile(file);
                                Clipboard.getInstance().setCurrentNode(node);
                                Clipboard.getInstance().setType(Clipboard.Type.CUT);
                                Snackbar.make(value.view, value.file.getName() + " selected to be moved.", Snackbar.LENGTH_SHORT).show();
                                return true;
                            case R.id.action_paste:
                                File currentFile = Clipboard.getInstance().getCurrentFile();
                                TreeNode currentNode = Clipboard.getInstance().getCurrentNode();
                                FileTreeItem currentItem = (FileTreeItem) currentNode.getValue();
                                switch (Clipboard.getInstance().getType()) {
                                    case COPY:
                                        if (currentFile.isDirectory()) {
                                            try {
                                                FileUtils.copyDirectoryToDirectory(currentFile, file);
                                            } catch (Exception e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            try {
                                                FileUtils.copyFileToDirectory(currentFile, file);
                                            } catch (Exception e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        }

                                        Snackbar.make(value.view, "Successfully copied " + currentFile.getName() + ".", Snackbar.LENGTH_SHORT).show();
                                        File copyFile = new File(file, currentFile.getName());
                                        TreeNode copyNode = new TreeNode(new FileTreeItem(ResourceHelper.getIcon(copyFile), copyFile, currentItem.view));
                                        node.addChild(copyNode);
                                        arrow.setVisibility(View.VISIBLE);
                                        tView.expandNode(node);
                                        break;
                                    case CUT:
                                        if (currentFile.isDirectory()) {
                                            try {
                                                FileUtils.moveDirectoryToDirectory(currentFile, file, false);
                                            } catch (Exception e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            try {
                                                FileUtils.moveFileToDirectory(currentFile, file, false);
                                            } catch (Exception e) {
                                                Log.e(TAG, e.toString());
                                                Snackbar.make(value.view, e.toString(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        }

                                        Snackbar.make(value.view, "Successfully moved " + currentFile.getName() + ".", Snackbar.LENGTH_SHORT).show();
                                        Clipboard.getInstance().setCurrentFile(null);
                                        File cutFile = new File(file, currentFile.getName());
                                        TreeNode cutNode = new TreeNode(new FileTreeItem(ResourceHelper.getIcon(cutFile), cutFile, currentItem.view));
                                        node.addChild(cutNode);
                                        arrow.setVisibility(View.VISIBLE);
                                        tView.expandNode(node);
                                        tView.removeNode(Clipboard.getInstance().getCurrentNode());
                                        break;
                                }

                                return true;
                            default:
                                return false;
                        }
                    }
                });

                menu.show();
            }
        });

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrow.animate().rotation(active ? 0 : -90).setDuration(150);
    }

    public static class FileTreeItem {

        @DrawableRes
        public int icon;

        public File file;

        public View view;

        public FileTreeItem(int icon, File file, View view) {
            this.icon = icon;
            this.file = file;
            this.view = view;
        }
    }
}
