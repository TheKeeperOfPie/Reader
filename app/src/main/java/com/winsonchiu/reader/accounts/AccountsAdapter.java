/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.accounts;

import android.accounts.Account;
import android.graphics.PorterDuff;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.ViewHolderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by TheKeeperOfPie on 3/27/2016.
 */
public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {

    public static final String TAG = AccountsAdapter.class.getCanonicalName();

    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_ADD = 1;
    public static final int TYPE_LOGOUT = 2;

    private final CustomColorFilter colorFilterPrimary;
    private final Listener listener;

    private List<Account> accounts = new ArrayList<>();

    public AccountsAdapter(int colorFilterPrimary, Listener listener) {
        this.colorFilterPrimary = new CustomColorFilter(colorFilterPrimary, PorterDuff.Mode.SRC_IN);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 2) {
            return TYPE_ADD;
        }
        if (position == getItemCount() - 1) {
            return TYPE_LOGOUT;
        }

        return TYPE_ACCOUNT;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_ACCOUNT:
                return new ViewHolderAccount(parent, colorFilterPrimary, listener);
            case TYPE_ADD:
                return new ViewHolderAdd(parent, colorFilterPrimary, listener);
            case TYPE_LOGOUT:
                return new ViewHolderLogout(parent, colorFilterPrimary, listener);
        }

        throw new IllegalArgumentException(TAG + " viewType " + viewType + " not valid");
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_ACCOUNT:
                holder.bindData(accounts.get(position));
                break;
            case TYPE_ADD:
            case TYPE_LOGOUT:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return accounts.size() + 2;
    }

    public void setAccounts(Account[] accounts) {
        this.accounts.clear();
        Collections.addAll(this.accounts, accounts);
        notifyDataSetChanged();
    }

    public static abstract class ViewHolder extends ViewHolderBase {

        @Bind(R.id.layout_account) ViewGroup layoutAccount;
        @Bind(R.id.image_action) ImageView imageAction;
        @Bind(R.id.text_account) TextView textAccount;

        protected CustomColorFilter colorFilterPrimary;
        protected Listener listener;
        protected Account account;

        public ViewHolder(ViewGroup parent, CustomColorFilter colorFilterPrimary, Listener listener) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_account, parent, false));
            ButterKnife.bind(this, itemView);

            this.colorFilterPrimary = colorFilterPrimary;
            this.listener = listener;
        }

        @CallSuper
        public void bindData(Account account) {
            this.account = account;
        }
    }

    public static class ViewHolderAccount extends ViewHolder {

        public ViewHolderAccount(ViewGroup parent, CustomColorFilter colorFilterPrimary, final Listener listener) {
            super(parent, colorFilterPrimary, listener);

            textAccount.setTextColor(colorFilterPrimary.getColor());
            imageAction.setImageResource(R.drawable.ic_delete_white_24dp);
            imageAction.setColorFilter(colorFilterPrimary);
        }

        @Override
        public void bindData(Account account) {
            super.bindData(account);
            textAccount.setText(account.name);
        }

        @OnClick(R.id.layout_account)
        public void setAccount() {
            listener.setAccount(account);
        }

        @OnClick(R.id.image_action)
        public void deleteAccount() {
            listener.deleteAccount(account);
        }
    }

    public static class ViewHolderAdd extends ViewHolder {

        public ViewHolderAdd(ViewGroup parent, CustomColorFilter colorFilterPrimary, final Listener listener) {
            super(parent, colorFilterPrimary, listener);

            textAccount.setText(R.string.add_account);
            textAccount.setTextColor(colorFilterPrimary.getColor());
            imageAction.setImageResource(R.drawable.ic_add_white_24dp);
            imageAction.setColorFilter(colorFilterPrimary);
        }

        @OnClick(R.id.layout_account)
        public void addNewAccount() {
            listener.addNewAccount();
        }
    }

    public static class ViewHolderLogout extends ViewHolder {

        public ViewHolderLogout(ViewGroup parent, CustomColorFilter colorFilterPrimary, final Listener listener) {
            super(parent, colorFilterPrimary, listener);

            textAccount.setText(R.string.logout);
            textAccount.setTextColor(colorFilterPrimary.getColor());
            imageAction.setImageResource(R.drawable.ic_exit_to_app_white_24dp);
            imageAction.setColorFilter(colorFilterPrimary);
        }

        @OnClick(R.id.layout_account)
        public void clearAccount() {
            listener.clearAccount();
        }
    }

    public interface Listener {
        void addNewAccount();
        void clearAccount();
        void setAccount(Account account);
        void deleteAccount(Account account);
    }
}
