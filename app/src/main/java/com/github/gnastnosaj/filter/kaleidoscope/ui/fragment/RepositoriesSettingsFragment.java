package com.github.gnastnosaj.filter.kaleidoscope.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.gnastnosaj.filter.kaleidoscope.R;
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.PluginApi;
import org.adblockplus.libadblockplus.android.settings.BaseSettingsFragment;

import java.util.ArrayList;
import java.util.HashSet;

public class RepositoriesSettingsFragment extends BaseSettingsFragment<RepositoriesSettingsFragment.Listener> {

    private SharedPreferences sharedPreferences;

    private EditText repository;
    private ImageView addRepositoryButton;
    private ListView listView;
    private Adapter adapter;

    public interface Listener extends BaseSettingsFragment.Listener {
        boolean isValidRepository(RepositoriesSettingsFragment fragment, String repository);

        void onKaleidoSettingsChanged(RepositoriesSettingsFragment fragment);
    }

    public RepositoriesSettingsFragment() {
        // required empty public constructor
    }

    public static RepositoriesSettingsFragment newInstance() {
        return new RepositoriesSettingsFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = castOrThrow(activity, Listener.class);
        sharedPreferences = activity.getSharedPreferences(PluginApi.PREF_REPOSITORIES, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_kaleido_repo_settings, container, false);

        bindControls(rootView);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initControls();
    }

    private void bindControls(View rootView) {
        repository = rootView.findViewById(R.id.fragment_repo_add_label);
        addRepositoryButton = rootView.findViewById(R.id.fragment_repo_add_button);
        listView = rootView.findViewById(R.id.fragment_repo_listview);
    }

    private class Holder {
        TextView repository;
        ImageView removeButton;

        Holder(View rootView) {
            repository = rootView.findViewById(R.id.fragment_kaleido_repo_item_title);
            removeButton = rootView.findViewById(R.id.fragment_kaleido_repo_item_remove);
        }
    }

    private View.OnClickListener removeRepositoryClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // update and save settings
            int position = ((Integer) v.getTag()).intValue();
            ArrayList<String> repositories = new ArrayList<>(sharedPreferences.getStringSet(PluginApi.PREF_REPOSITORIES, new HashSet<>()));
            String removeRepository = repositories.get(position);
            repositories.remove(removeRepository);
            sharedPreferences.edit().putStringSet(PluginApi.PREF_REPOSITORIES, new HashSet<>(repositories)).apply();

            // signal event
            listener.onKaleidoSettingsChanged(RepositoriesSettingsFragment.this);

            // update UI
            adapter.notifyDataSetChanged();
        }
    };

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return sharedPreferences.getStringSet(PluginApi.PREF_REPOSITORIES, new HashSet<>()).size();
        }

        @Override
        public Object getItem(int position) {
            return new ArrayList<>(sharedPreferences.getStringSet(PluginApi.PREF_REPOSITORIES, new HashSet<>())).get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.fragment_kaleido_repo_item,
                        parent, false);
                convertView.setTag(new Holder(convertView));
            }

            String repository = (String) getItem(position);

            Holder holder = (Holder) convertView.getTag();
            holder.repository.setText(repository);

            holder.removeButton.setOnClickListener(removeRepositoryClickListener);
            holder.removeButton.setTag(Integer.valueOf(position));

            return convertView;
        }
    }

    private void initControls() {
        addRepositoryButton.setOnClickListener(v -> {
            String preparedRepository = prepareRepository(repository.getText().toString());
            if (listener.isValidRepository(
                    RepositoriesSettingsFragment.this,
                    preparedRepository)) {
                addRepository(preparedRepository);
            }
        });

        adapter = new Adapter();
        listView.setAdapter(adapter);
    }

    private String prepareRepository(String repository) {
        return repository.trim();
    }

    public void addRepository(String newRepository) {
        // update and save settings
        HashSet<String> repositories = new HashSet<>(sharedPreferences.getStringSet(PluginApi.PREF_REPOSITORIES, new HashSet<>()));
        repositories.add(newRepository);
        sharedPreferences.edit().putStringSet(PluginApi.PREF_REPOSITORIES, repositories).apply();

        // signal event
        listener.onKaleidoSettingsChanged(RepositoriesSettingsFragment.this);

        // update UI
        adapter.notifyDataSetChanged();
        repository.getText().clear();
        repository.clearFocus();
    }
}
