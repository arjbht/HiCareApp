package com.ab.hicarerun.fragments;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ab.hicarerun.R;
import com.ab.hicarerun.adapter.RecommendationsAdapter;
import com.ab.hicarerun.databinding.FragmentConsultationThirdBinding;
import com.ab.hicarerun.handler.UserRecommendationHandler;
import com.ab.hicarerun.network.NetworkCallController;
import com.ab.hicarerun.network.NetworkResponseListner;
import com.ab.hicarerun.network.models.ConsulationModel.Recommendations;
import com.ab.hicarerun.network.models.GeneralModel.GeneralData;
import com.ab.hicarerun.utils.AppUtils;
import com.ab.hicarerun.utils.LocaleHelper;

import java.util.List;

import io.realm.RealmResults;

import static com.ab.hicarerun.BaseApplication.getRealm;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConsultationThirdFragment extends Fragment implements UserRecommendationHandler {
    FragmentConsultationThirdBinding mFragmentConsultationThirdBinding;
    private static final int RECOMMENDATION_REQ = 1000;
    private RecommendationsAdapter mAdapter;
    private RealmResults<GeneralData> mTaskDetailsData = null;
    private ProgressDialog progressD;
    private String type = "";

    public ConsultationThirdFragment(String type) {
        // Required empty public constructor
        this.type = type;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mFragmentConsultationThirdBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_consultation_third, container, false);
        mFragmentConsultationThirdBinding.setHandler(this);
        progressD = new ProgressDialog(getActivity(), R.style.TransparentProgressDialog);
        progressD.setCancelable(false);
        return mFragmentConsultationThirdBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTaskDetailsData =
                getRealm().where(GeneralData.class).findAll();
        mAdapter = new RecommendationsAdapter(getActivity(), type);
        mFragmentConsultationThirdBinding.recycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFragmentConsultationThirdBinding.recycleView.setHasFixedSize(true);
        mFragmentConsultationThirdBinding.recycleView.setClipToPadding(false);
        mFragmentConsultationThirdBinding.recycleView.setAdapter(mAdapter);
        mFragmentConsultationThirdBinding.btnHome.setEnabled(false);
        mFragmentConsultationThirdBinding.btnHome.setAlpha(0.6f);
        mFragmentConsultationThirdBinding.txtTitle.setTypeface(mFragmentConsultationThirdBinding.txtTitle.getTypeface(), Typeface.BOLD);
        mFragmentConsultationThirdBinding.chkAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mFragmentConsultationThirdBinding.btnHome.setEnabled(true);
                mFragmentConsultationThirdBinding.btnHome.setAlpha(1f);
            } else {
                mFragmentConsultationThirdBinding.btnHome.setEnabled(false);
                mFragmentConsultationThirdBinding.btnHome.setAlpha(0.6f);
            }
        });
        if (AppUtils.isInspectionDone) {
            mFragmentConsultationThirdBinding.chkAgree.setVisibility(View.GONE);
            mFragmentConsultationThirdBinding.btnHome.setEnabled(true);
            mFragmentConsultationThirdBinding.btnHome.setAlpha(1f);
        } else {
            mFragmentConsultationThirdBinding.chkAgree.setVisibility(View.VISIBLE);
        }
        getRecommendations();
    }

    private void getRecommendations() {
        try {
            if (mTaskDetailsData != null && mTaskDetailsData.size() > 0) {
                progressD.show();
                NetworkCallController controller = new NetworkCallController();
                controller.setListner(new NetworkResponseListner<List<Recommendations>>() {
                    @Override
                    public void onResponse(int requestCode, List<Recommendations> items) {
                        progressD.dismiss();
                        if (items != null && items.size() > 0) {
                            AppUtils.isInspectionDone = true;
//
                            if (type.equals("CMS")) {
                                if(items.get(0).getOverallInfestationLevel()!=null && !items.get(0).getOverallInfestationLevel().equals("")){
                                    mFragmentConsultationThirdBinding.txtPart.setText("RECOMMENDATIONS " + "(" + items.get(0).getOverallInfestationLevel() + ")");
                                }else {
                                    mFragmentConsultationThirdBinding.txtPart.setText("RECOMMENDATIONS");
                                }
                                AppUtils.infestationLevel = items.get(0).getOverallInfestationLevel();
                            }
                            mFragmentConsultationThirdBinding.recycleView.setVisibility(View.VISIBLE);
                            mFragmentConsultationThirdBinding.txtEmpty.setVisibility(View.GONE);
                            mAdapter.setData(items);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mFragmentConsultationThirdBinding.chkAgree.setVisibility(View.GONE);
                            mFragmentConsultationThirdBinding.recycleView.setVisibility(View.GONE);
                            mFragmentConsultationThirdBinding.txtEmpty.setVisibility(View.VISIBLE);
                            mFragmentConsultationThirdBinding.btnHome.setEnabled(true);
                            mFragmentConsultationThirdBinding.btnHome.setAlpha(1f);
                        }
                    }

                    @Override
                    public void onFailure(int requestCode) {

                    }
                });
                controller.getRecommendations(RECOMMENDATION_REQ, mTaskDetailsData.get(0).getResourceId(), mTaskDetailsData.get(0).getTaskId(), LocaleHelper.getLanguage(getActivity()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHomeButtonClicked(View view) {
        ChildFragment3Listener listener = (ChildFragment3Listener) getParentFragment();
        listener.onHomeButtonClicked();
    }

    interface ChildFragment3Listener {
        void onHomeButtonClicked();
    }
}
