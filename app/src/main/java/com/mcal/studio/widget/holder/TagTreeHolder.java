package com.mcal.studio.widget.holder;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mcal.studio.R;
import com.mcal.studio.adapter.AttrsAdapter;
import com.mcal.studio.text.HtmlCompat;
import com.unnamed.b.atv.model.TreeNode;

import org.jsoup.nodes.Element;

public class TagTreeHolder extends TreeNode.BaseNodeViewHolder<TagTreeHolder.TagTreeItem> {

    private ImageView arrow;

    public TagTreeHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, final TagTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_element, null, false);
        TextView tagName = view.findViewById(R.id.element_name);
        arrow = view.findViewById(R.id.element_arrow);
        final ImageButton overflow = view.findViewById(R.id.element_overflow);

        Spanned element = HtmlCompat.fromHtml("\t&lt;<font color=\"#f92672\">" + value.element.tagName() + "</font>&gt;");
        tagName.setText(element);

        if (node.isLeaf()) {
            arrow.setVisibility(View.GONE);
        }

        if (!node.isExpanded()) {
            arrow.setRotation(-90);
        }

        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final PopupMenu menu = new PopupMenu(context, overflow);
                menu.getMenuInflater().inflate(R.menu.menu_tag_options, menu.getMenu());
                if (value.element.tagName().equals("head") || value.element.tagName().equals("body")) {
                    menu.getMenu().findItem(R.id.action_tag_remove).setVisible(false);
                }

                if (!value.element.isBlock()) {
                    menu.getMenu().findItem(R.id.action_tag_add).setVisible(false);
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_tag_add:
                                View rootView = inflater.inflate(R.layout.dialog_element_add, null, false);
                                final TextInputEditText nameText = rootView.findViewById(R.id.element_name_text);
                                final TextInputEditText textText = rootView.findViewById(R.id.element_text_text);

                                final AlertDialog dialog = new AlertDialog.Builder(context)
                                        .setTitle("Add to " + value.element.tagName())
                                        .setView(rootView)
                                        .setPositiveButton("ADD", null)
                                        .setNegativeButton("CANCEL", null)
                                        .create();

                                dialog.show();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (!nameText.getText().toString().isEmpty()) {
                                            tView.addNode(node, new TreeNode(new TagTreeItem(value.element.appendElement(nameText.getText().toString()).text(textText.getText().toString()))));
                                            dialog.dismiss();
                                        } else {
                                            nameText.setError("Please enter a name for the element.");
                                        }
                                    }
                                });
                                return true;
                            case R.id.action_tag_edit:
                                final Element element = value.element;
                                View rootView2 = inflater.inflate(R.layout.dialog_element_info, null, false);
                                RecyclerView elementAttrs = rootView2.findViewById(R.id.element_attrs);
                                AttrsAdapter adapter = new AttrsAdapter(context, element);
                                ImageButton tagEdit = rootView2.findViewById(R.id.element_tag_edit);
                                ImageButton textEdit = rootView2.findViewById(R.id.element_text_edit);
                                LinearLayoutManager manager = new LinearLayoutManager(context);
                                final TextView elementTag = rootView2.findViewById(R.id.element_tag);
                                final TextView elementText = rootView2.findViewById(R.id.element_text);
                                elementAttrs.setLayoutManager(manager);
                                elementAttrs.addItemDecoration(new DividerItemDecoration(context, manager.getOrientation()));
                                elementAttrs.setHasFixedSize(true);
                                elementAttrs.setAdapter(adapter);
                                elementTag.setText(element.tagName());
                                tagEdit.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View viewElement = inflater.inflate(R.layout.dialog_input_single, null, false);
                                        final TextInputEditText editText = viewElement.findViewById(R.id.input_text);
                                        editText.setHint("Value");
                                        editText.setSingleLine(true);
                                        editText.setMaxLines(1);
                                        editText.setText(element.tagName());

                                        final AlertDialog elementDialog = new AlertDialog.Builder(context)
                                                .setTitle("Change element tag")
                                                .setView(viewElement)
                                                .setPositiveButton("CHANGE", null)
                                                .setNegativeButton("CANCEL", null)
                                                .create();

                                        elementDialog.show();
                                        elementDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if (!editText.getText().toString().isEmpty()) {
                                                    element.tagName(editText.getText().toString());
                                                    elementTag.setText(editText.getText().toString());
                                                    TreeNode parent = node.getParent();
                                                    tView.removeNode(node);
                                                    tView.addNode(parent, new TreeNode(new TagTreeItem(value.element)));
                                                    elementDialog.dismiss();
                                                } else {
                                                    editText.setError("Please enter a name for the element.");
                                                }
                                            }
                                        });
                                    }
                                });

                                elementText.setText(element.ownText().isEmpty() ? "empty" : element.ownText());
                                textEdit.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View viewElement = inflater.inflate(R.layout.dialog_input_single, null, false);
                                        final TextInputEditText editText = viewElement.findViewById(R.id.input_text);
                                        editText.setHint("Value");
                                        editText.setSingleLine(true);
                                        editText.setMaxLines(1);
                                        editText.setText(element.ownText());

                                        new AlertDialog.Builder(context)
                                                .setTitle("Change element text")
                                                .setView(viewElement)
                                                .setPositiveButton("CHANGE", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        element.text(editText.getText().toString());
                                                        elementText.setText(editText.getText().toString());
                                                    }
                                                })
                                                .setNegativeButton("CANCEL", null)
                                                .show();
                                    }
                                });

                                new AlertDialog.Builder(context)
                                        .setView(rootView2)
                                        .show();
                                return true;
                            case R.id.action_tag_remove:
                                tView.removeNode(node);
                                value.element.remove();
                                return true;
                        }

                        return true;
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

    public static class TagTreeItem {

        Element element;

        public TagTreeItem(Element element) {
            this.element = element;
        }
    }
}
