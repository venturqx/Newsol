package org.schabi.newpipe.fragments.detail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A pager adapter that lets offscreen fragments save state instead of keeping every tab resident.
 */
public final class StateSavingTabAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<String> fragmentTitleList = new ArrayList<>();

    public StateSavingTabAdapter(final FragmentManager fm) {
        // Keep legacy visibility semantics because several tabs still depend on them.
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
    }

    @NonNull
    @Override
    public Fragment getItem(final int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    public void addFragment(final Fragment fragment, final String title) {
        fragmentList.add(fragment);
        fragmentTitleList.add(title);
    }

    public void clearAllItems() {
        fragmentList.clear();
        fragmentTitleList.clear();
    }

    public void updateItem(final int position, final Fragment fragment) {
        fragmentList.set(position, fragment);
    }

    public void updateItem(final String title, final Fragment fragment) {
        final int index = fragmentTitleList.indexOf(title);
        if (index != -1) {
            updateItem(index, fragment);
        }
    }

    @Nullable
    public Fragment getFragment(final String title) {
        final int index = fragmentTitleList.indexOf(title);
        if (index == -1) {
            return null;
        }
        return fragmentList.get(index);
    }

    @Override
    public int getItemPosition(@NonNull final Object object) {
        if (fragmentList.contains(object)) {
            return fragmentList.indexOf(object);
        } else {
            return POSITION_NONE;
        }
    }

    public int getItemPositionByTitle(final String title) {
        return fragmentTitleList.indexOf(title);
    }

    @Nullable
    public String getItemTitle(final int position) {
        if (position < 0 || position >= fragmentTitleList.size()) {
            return null;
        }
        return fragmentTitleList.get(position);
    }

    public void notifyDataSetUpdate() {
        notifyDataSetChanged();
    }
}
