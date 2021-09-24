package com.ab.hicarerun.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ab.hicarerun.BaseApplication;
import com.ab.hicarerun.BaseFragment;
import com.ab.hicarerun.R;
import com.ab.hicarerun.activities.QuizResultActivity;
import com.ab.hicarerun.adapter.QuizOptionAdapter;
import com.ab.hicarerun.adapter.QuizVideoParentAdapter;
import com.ab.hicarerun.databinding.FragmentQuizBinding;
import com.ab.hicarerun.network.NetworkCallController;
import com.ab.hicarerun.network.NetworkResponseListner;
import com.ab.hicarerun.network.models.LoginResponse;
import com.ab.hicarerun.network.models.QuizModel.QuizAnswer;
import com.ab.hicarerun.network.models.QuizModel.QuizData;
import com.ab.hicarerun.network.models.QuizModel.QuizPuzzleStats;
import com.ab.hicarerun.network.models.QuizModel.QuizSaveAnswers;
import com.ab.hicarerun.network.models.QuizModel.VideoDependentQuest;
import com.ab.hicarerun.network.models.QuizSaveModel.QuizSaveResponseBase;
import com.ab.hicarerun.utils.AppUtils;
import com.ab.hicarerun.utils.LocaleHelper;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.realm.RealmResults;

public class QuizFragment extends BaseFragment implements Player.EventListener {
    FragmentQuizBinding mFragmentQuizBinding;
    private static final int REQ_QUESTIONS = 1000;
    public static final String ARGS_ID = "ARGS_ID";
    private QuizOptionAdapter mAdapter;
    private QuizVideoParentAdapter mVideoAdapter;
    List<QuizData> questions;
    List<QuizAnswer> answerList;
    List<VideoDependentQuest> vidQuestions;
    VideoDependentQuest vQ;
    int index = 0;
    int oldScore = 0;
    int vidIndex = 0;
    QuizData question;
    CountDownTimer timer;
    private int puzzleId = 0;
    private final int SECONDS = 1000;
    private int MIN_BUFF_MS = 3000;
    private int MAX_BUFF_MS = 5000;
    private int BUFF_PLAYBACK_MS = 1000;
    private int REBUFF_MS = 2500;
    private int TARGET_BUFF_BYTES = -1;
    SimpleExoPlayer player;
    private PlayerView videoSurfaceView;
    private List<QuizSaveAnswers> saveAnswers;
    RecyclerView.LayoutManager layoutManager;
    int points = 0;
    String resourceId = "";
    boolean optionSelected = false;
    boolean isConfirmPressedOnce = false;
    List<String> givenAnswers;
    List<String> normalCorrectAns;
    String prevTitle = "";
    String puzzleType = "";
    public static String puzzleT = "";

    public QuizFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static QuizFragment newInstance(int puzzleId, String puzzleTitle) {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_ID, puzzleId);
        fragment.setArguments(args);
        puzzleT = R.string.shiksha+" - "+puzzleTitle;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            puzzleId = getArguments().getInt(ARGS_ID);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mFragmentQuizBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_quiz, container, false);
        requireActivity().setTitle("");
        RealmResults<LoginResponse> LoginRealmModels =
                BaseApplication.getRealm().where(LoginResponse.class).findAll();
        if (LoginRealmModels != null && LoginRealmModels.size() > 0) {
            resourceId = LoginRealmModels.get(0).getUserID();
        }
        mFragmentQuizBinding.questionTitleTv.setText(puzzleT);
        mFragmentQuizBinding.questionCounter.setTypeface(mFragmentQuizBinding.questionCounter.getTypeface(), Typeface.BOLD);
        return mFragmentQuizBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        questions = new ArrayList<>();
        vidQuestions = new ArrayList<>();
        saveAnswers = new ArrayList<>();
        answerList = new ArrayList<>();
        givenAnswers = new ArrayList<>();
        normalCorrectAns = new ArrayList<>();
//        mFragmentQuizBinding.recycleView.setHasFixedSize(true);
//        mFragmentQuizBinding.recycleView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
//        mAdapter = new QuizOptionAdapter(getActivity());
//        mFragmentQuizBinding.recycleView.setAdapter(mAdapter);
        getQuestions();
        resetTimer();
        if (index == questions.size()-1){
            mFragmentQuizBinding.nextBtn.setText(R.string.shiksha_finish);
        }
        mFragmentQuizBinding.nextBtn.setOnClickListener(view1 -> {
            givenAnswers.clear();
//            reset();
            if (puzzleType.equalsIgnoreCase("video")){
                if (vidIndex < vidQuestions.size() - 1) {
                    vidIndex++;
                    setNextQuestion(true);
                }else {
                    if (index < questions.size() - 1) {
                        vidIndex = 0;
                        index++;
                        setNextQuestion(true);
                    } else {
                        if (saveAnswers.isEmpty()) {
                            requireActivity().finish();
                        } else {
                            mFragmentQuizBinding.nextBtn.setEnabled(false);
                            savePuzzle();
                        }
                    }
                }
            }else {
                if (index < questions.size() - 1) {
                    vidIndex = 0;
                    index++;
                    setNextQuestion(true);
                } else {
//                Intent intent = new Intent(this, ResultActivity.class);
//                intent.putExtra("correct", correctAnswers);
//                intent.putExtra("total", questions.size());
//                startActivity(intent);
                    //Toast.makeText(getActivity(), "Quiz Finished.", Toast.LENGTH_SHORT).show();
                    if (saveAnswers.isEmpty()) {
                        requireActivity().finish();
                    } else {
                        mFragmentQuizBinding.nextBtn.setEnabled(false);
                        savePuzzle();
                    }
                }
            }
        });
        mFragmentQuizBinding.quitBtn.setOnClickListener(v -> {
            showQuitDialog();
        });
    }

    @Override
    public void onStart() {
        getPuzzleStatsForRes();
        super.onStart();
    }

    private void showQuitDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.shiksha_alert);
        builder.setMessage(R.string.shiksha_quit_alert);
        builder.setPositiveButton(R.string.shiksha_quit, (dialog, which) -> {
            dialog.cancel();
            if (saveAnswers.isEmpty()) {
                Log.d("TAG", "Finish called");
                requireActivity().finish();
            }else {
                if (isConfirmPressedOnce) {
                    Log.d("TAG", "Save called");
                    savePuzzle();
                }else {
                    Log.d("TAG", "Finish called");
                    requireActivity().finish();
                }
            }
        });
        builder.setNegativeButton(R.string.shiksha_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showNoQuizDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.shiksha_alert);
        builder.setMessage(R.string.shiksha_noquiz_alert);
        builder.setPositiveButton(R.string.shiksha_quit, (dialog, which) -> {
            dialog.cancel();
            requireActivity().finish();
        });
        builder.show();
    }

    void resetTimer() {
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mFragmentQuizBinding.timer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                /*if (index < questions.size() - 1) {
                    index++;
                    setNextQuestion(true);
                } else {
//                Intent intent = new Intent(this, ResultActivity.class);
//                intent.putExtra("correct", correctAnswers);
//                intent.putExtra("total", questions.size());
//                startActivity(intent);
                    Toast.makeText(getActivity(), "Quiz Finished.", Toast.LENGTH_SHORT).show();
                    if (!saveAnswers.isEmpty()){
                        savePuzzle();
                    }else {
                        requireActivity().finish();
                    }
                }*/
            }
        };
    }

    private void savePuzzle(){
        NetworkCallController controller = new NetworkCallController(this);
        controller.setListner(new NetworkResponseListner<QuizSaveResponseBase>(){
            @Override
            public void onResponse(int requestCode, QuizSaveResponseBase response) {
                if (response.isSuccess()){
                    Log.d("TAG", response.getData().getTotalPoints().toString());
                    Intent intent = new Intent(requireContext(), QuizResultActivity.class);
                    intent.putExtra("points", response.getData().getTotalPoints().toString());
                    intent.putExtra("earned", Integer.toString(response.getData().getTotalPoints()-oldScore));
                    intent.putExtra("levelName", response.getData().getCurrentLevelName());
                    intent.putExtra("upGrdLevelName", response.getData().getUpgradedLevelName());
                    intent.putExtra("resMessage", response.getData().getResourceMessage());
                    intent.putExtra("currLID", response.getData().getCurrentLevelId());
                    intent.putExtra("upgrdLID", response.getData().getUpgradedLevelId());
                    intent.putExtra("currLIC", response.getData().getCurrentIconUrl());
                    intent.putExtra("upgrdLIC", response.getData().getUpgradedIconUrl());
                    startActivity(intent);
                    requireActivity().finish();
                }
            }

            @Override
            public void onFailure(int requestCode) {

            }
        });
        Log.d("TAG", "Saveing answers: "+ saveAnswers.toString());
        controller.savePuzzleAnswers(2021262, saveAnswers);
    }
    private void getQuestions() {
        try {
            RealmResults<LoginResponse> LoginRealmModels =
                    BaseApplication.getRealm().where(LoginResponse.class).findAll();
            if (LoginRealmModels != null && LoginRealmModels.size() > 0) {
                String resourceId = Objects.requireNonNull(LoginRealmModels.get(0)).getUserID();
                NetworkCallController controller = new NetworkCallController(this);
                controller.setListner(new NetworkResponseListner<List<QuizData>>() {
                    @Override
                    public void onResponse(int requestCode, List<QuizData> item) {
                        if (item != null && item.size() > 0) {
                            if (index < item.size()) {
                                questions = item;
                                setNextQuestion(false);
                            }
                        }else {
                            showNoQuizDialog();
                        }
                    }

                    @Override
                    public void onFailure(int requestCode) {

                    }
                });
                controller.getQuizQuestions(REQ_QUESTIONS, resourceId, puzzleId, LocaleHelper.getLanguage(requireContext()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setNextQuestion(boolean isNextPressed) {
        if (index == questions.size()-1){
            mFragmentQuizBinding.nextBtn.setText(R.string.shiksha_finish);
        }
        mFragmentQuizBinding.nextBtn.setEnabled(false);
        if (timer != null) {
            timer.start();
            timer.cancel();
        }
        //mFragmentQuizBinding.timer.setVisibility(View.VISIBLE);
        if (index < questions.size()) {
            mFragmentQuizBinding.questionCounter.setText(points+"");
            //mFragmentQuizBinding.questionCounter.setText(String.format("%d/%d", (index + 1), questions.size()));
            question = questions.get(index);
            normalCorrectAns.clear();
            givenAnswers.clear();
            if (question.getCorrectAnswers() != null) {
                for (QuizAnswer s : question.getCorrectAnswers()) {
                    normalCorrectAns.add(s.getOptionId().toString());
                }
            }

            if (question.getIsDependentQuestionExist()) {
                puzzleType = "video";
                //TODO : This is to make the next button enable like normal questions.
                /*int totalQuestions = question.getDependentQuestionList().size();
                AtomicInteger givenAnsInts = new AtomicInteger();
                AtomicInteger videoIndex = new AtomicInteger();*/
                vidQuestions = question.getDependentQuestionList();
                vQ = vidQuestions.get(vidIndex);
                Log.d("TAG", "Called");

                //TODO : This is to make the next button enable like normal questions.
                normalCorrectAns.clear();
                givenAnswers.clear();
                if (vQ.getCorrectAnswers() != null){
                    for (QuizAnswer s : vQ.getCorrectAnswers()){
                        normalCorrectAns.add(s.getOptionId().toString());
                    }
                }
                timer.cancel();
                mFragmentQuizBinding.timer.setVisibility(View.INVISIBLE);
                //                GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
//                layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//                    @Override
//                    public int getSpanSize(int pos) {
//                        if (question.getDependentQuestionList().get(0).getPuzzleQuestionSelectionType().equalsIgnoreCase("Image")) {
//                            return 1;
//                        } else {
//                            return 2;
//                        }
//                    }
//                });
                layoutManager = new LinearLayoutManager(getActivity());
                mFragmentQuizBinding.recycleView.setLayoutManager(layoutManager);
                mFragmentQuizBinding.recycleView.setNestedScrollingEnabled(false);
                mVideoAdapter = new QuizVideoParentAdapter(getActivity(), (position, option) -> {
                    if (vidIndex < question.getDependentQuestionList().size() - 1) {
                        //vidIndex++;
                    }
                });
                for (int i = 0; i<question.getDependentQuestionList().size(); i++){
                    answerList.addAll(question.getDependentQuestionList().get(i).getCorrectAnswers());
                }
                //mVideoAdapter.setData(question.getDependentQuestionList(), vidQuestions.get(vidIndex).getCorrectAnswers());
                mVideoAdapter.setData(vQ, vQ.getCorrectAnswers());
                mVideoAdapter.setOnVideoOptionClickListener((position, quizOption, title, optionType, childHolder) -> {
                    /*if (prevTitle.equalsIgnoreCase("")){
                        prevTitle = title;
                    }
                    if (!prevTitle.equals("") && !prevTitle.equals(title)){
                        prevTitle = title;
                    }*/
                    optionSelected = true;
                    mFragmentQuizBinding.nextBtn.setEnabled(true);
                    Log.d("TAG-Title", title);
                    int count = -1;
                    for (int i = 0; i < vidQuestions.size(); i++){
                        count++;
                        if (vidQuestions.get(i).getPuzzleQuestionTitle().trim().equalsIgnoreCase(title)){
                            //videoIndex.set(count);
                            break;
                        }
                    }
                    /*Log.d("TAG-Video", count+" "+question.getPuzzleId());
                    Log.d("TAG-Video", count+" "+question.getDependentQuestionList().get(count).getPuzzleQuestionId());
                    Log.d("TAG-Video", count+" "+question.getDependentQuestionList().get(count).getCorrectAnswerIds());
                    Log.d("TAG-Video", count+" "+quizOption.getOptionId().toString());
                    Log.d("TAG-Video", count+" "+question.getDependentQuestionList().get(count).getPoints());*/
                    int found = 0;
                    if (optionType.equalsIgnoreCase("radio")) {
                        saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getDependentQuestionList().get(count).getPuzzleQuestionId(),
                                question.getDependentQuestionList().get(count).getCorrectAnswerIds(), quizOption.getOptionId().toString(),
                                resourceId, question.getDependentQuestionList().get(count).getPoints()));

                        if (question.getDependentQuestionList().get(count).getCorrectAnswerIds().contains(quizOption.getOptionId().toString())) {
                            points = points + question.getDependentQuestionList().get(count).getPoints();
                            Log.d("TAG-pts", "called");
                        }
                    }
                    if (optionType.equalsIgnoreCase("checkbox")){
                        givenAnswers.add(quizOption.getOptionId().toString());
                        if (normalCorrectAns.size() < givenAnswers.size()){
                            points = points - question.getDependentQuestionList().get(count).getPoints();
                        }
                        if (normalCorrectAns.size() == givenAnswers.size()){
                            int containsAll = 1;
                            for (String s: givenAnswers){
                                if (!normalCorrectAns.contains(s)){
                                    containsAll = 0;
                                    break;
                                }
                            }
                            if (containsAll == 1){
                                Log.d("Called", "this");
                                mFragmentQuizBinding.nextBtn.setEnabled(true);
                                points = points + question.getDependentQuestionList().get(count).getPoints();
                            }else {
                                mFragmentQuizBinding.nextBtn.setEnabled(true);
                            }

                        }else {
                            for (String s: givenAnswers){
                                if (!normalCorrectAns.contains(s)){
                                    mFragmentQuizBinding.nextBtn.setEnabled(true);
                                }
                            }
                        }

                        if (!saveAnswers.isEmpty()){
                            for (int i=0; i < saveAnswers.size(); i++){
                                if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getDependentQuestionList().get(count).getPuzzleQuestionId())){
                                    String prev = "";
                                    prev = saveAnswers.get(i).getResourceGivenAnswerIds();
                                    prev = prev + "," + quizOption.getOptionId().toString();
                                    saveAnswers.get(i).setResourceGivenAnswerIds(prev);
                                    Log.d("TAG", saveAnswers.get(i).getResourceGivenAnswerIds());
                                    found = 1;
                                    break;
                                }
                            }
                            if (found == 0){
                                saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getDependentQuestionList().get(count).getPuzzleQuestionId(),
                                        question.getDependentQuestionList().get(count).getCorrectAnswerIds(), quizOption.getOptionId().toString(),
                                        resourceId, question.getDependentQuestionList().get(count).getPoints()));}
                        }else {
                            saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getDependentQuestionList().get(count).getPuzzleQuestionId(),
                                    question.getDependentQuestionList().get(count).getCorrectAnswerIds(), quizOption.getOptionId().toString(),
                                    resourceId, question.getDependentQuestionList().get(count).getPoints()));
                        }
                    }
                    /*if (question.getDependentQuestionList().get(count).getCorrectAnswerIds().contains(quizOption.getOptionId().toString())) {
                        points = points + question.getDependentQuestionList().get(count).getPoints();
                        Log.d("TAG-pts", "called");
                    }*/
                    mFragmentQuizBinding.questionCounter.setText(points+"");
                    /*if (vidIndex < question.getDependentQuestionList().size() - 1) {
                        vidIndex++;
                    }*/
                });
                mFragmentQuizBinding.recycleView.setAdapter(mVideoAdapter);
                if (!prevTitle.equalsIgnoreCase(question.getPuzzleQuestionURL())){
                    initializePlayer();
                    buildMediaSource(Uri.parse(question.getPuzzleQuestionURL()));
                    prevTitle = question.getPuzzleQuestionURL();
                }

                if (vidIndex == question.getDependentQuestionList().size() - 1 && index == questions.size() - 1) {
                    mFragmentQuizBinding.nextBtn.setText(R.string.shiksha_finish);
                }else {
                    mFragmentQuizBinding.nextBtn.setText(R.string.shiksha_next);
                }
            } else {
                puzzleType = "normal";
                GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
                layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int pos) {
                        if (question.getOptions().get(0).getOptionType().equalsIgnoreCase("Image")) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }
                });
                mFragmentQuizBinding.recycleView.setLayoutManager(layoutManager);
                mAdapter = new QuizOptionAdapter(getActivity(), question.getPuzzleQuestionType(), question.getPuzzleQuestionSelectionType(), question.getCorrectAnswers(), isNextPressed);
                mAdapter.setData(question.getOptions(), question.getCorrectAnswers());
                mAdapter.setOnOptionClickListener((position, quizOption, givenAnswersIds, title, optionType, holder) -> {
                    //Log.d("TAG", quizOption.getOptionId().toString());
                    List<String> collected = new ArrayList<>();
                    collected.add(quizOption.getOptionId().toString());
                    optionSelected = true;
                    Log.d("Called", "this "+optionType);
                    int found = 0;
                    if (optionType.equalsIgnoreCase("radio")) {
                        /*if (question.getCorrectAnswerIds().contains(quizOption.getOptionId().toString())) {
                            points = points + question.getPoints();
                        }*/
                        if (!saveAnswers.isEmpty()){
                            boolean alreadyPresent = false;
                            int presentAt = 0;
                            for (int i = 0; i < saveAnswers.size(); i++){
                                if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId())){
                                    saveAnswers.remove(i);
                                    saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                                    alreadyPresent = true;
                                    presentAt = i;
                                    break;
                                }
                            }
                            if (!alreadyPresent){
                                saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                            }
                        }else {
                            saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                        }
                        Log.d("Called", "or this");
                        //mFragmentQuizBinding.nextBtn.setEnabled(true);
                    }
                    if (optionType.equalsIgnoreCase("checkbox")){
                        if (!saveAnswers.isEmpty()){
                            boolean alreadyPresent = false;
                            int presentAt = 0;
                            for (int i = 0; i < saveAnswers.size(); i++){
                                if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId())){
                                    saveAnswers.remove(i);
                                    saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                                    alreadyPresent = true;
                                    presentAt = i;
                                    break;
                                }
                            }
                            if (!alreadyPresent){
                                saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                            }
                        }else {
                            saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                        }
                        /*givenAnswers.add(quizOption.getOptionId().toString());
                        if (normalCorrectAns.size() < givenAnswers.size()){
                            points = points - question.getPoints();
                        }
                        if (normalCorrectAns.size() == givenAnswers.size()){
                            int containsAll = 1;
                            for (String s: givenAnswers){
                                if (!normalCorrectAns.contains(s)){
                                    containsAll = 0;
                                    break;
                                }
                            }
                            if (containsAll == 1){
                                Log.d("Called", "this");
                                //mFragmentQuizBinding.nextBtn.setEnabled(true);
                                points = points + question.getPoints();
                            }else {
                                //mFragmentQuizBinding.nextBtn.setEnabled(true);
                            }

                        }else {
                            for (String s: givenAnswers){
                                if (!normalCorrectAns.contains(s)){
                                    //mFragmentQuizBinding.nextBtn.setEnabled(true);
                                }
                            }
                        }*/
                        /*if (!saveAnswers.isEmpty()){
                            for (int i=0; i < saveAnswers.size(); i++){
                                if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId())){
                                    String prev = "";
                                    prev = saveAnswers.get(i).getResourceGivenAnswerIds();
                                    prev = prev + "," + quizOption.getOptionId().toString();
                                    saveAnswers.get(i).setResourceGivenAnswerIds(prev);
                                    Log.d("TAG", saveAnswers.get(i).getResourceGivenAnswerIds());
                                    found = 1;
                                    break;
                                }
                                *//*if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId()) && saveAnswers.get(i).getOptionId().equals(quizOption.getOptionId())){
                                    String prev = "";
                                    prev = saveAnswers.get(i).getResourceGivenAnswerIds();
                                    prev = prev + "," + quizOption.getOptionId().toString();
                                    saveAnswers.get(i).setResourceGivenAnswerIds(prev);
                                    Log.d("TAG", saveAnswers.get(i).getResourceGivenAnswerIds());
                                    found = 1;
                                    break;
                                }*//*
                            }
                            if (found == 0){
                                saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                            }
                        }else {
                            saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), question.getCorrectAnswerIds(), quizOption.getOptionId().toString(), resourceId, question.getPoints()));
                        }*/
                    }
                    if (optionType.equalsIgnoreCase("checkboxConfirmed")){
                        String correctAnsIds = question.getCorrectAnswerIds().replaceAll("\\s+"," ");
                        correctAnsIds = correctAnsIds.replaceAll("\\s","");
                        Log.d("TAG", "Given IDS: "+ givenAnswersIds + " CorrectAnswers: "+correctAnsIds);
                        if (!saveAnswers.isEmpty()){
                            boolean alreadyPresent = false;
                            int presentAt = 0;
                            for (int i = 0; i < saveAnswers.size(); i++){
                                if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId())){
                                    saveAnswers.remove(i);
                                    saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), correctAnsIds, givenAnswersIds, resourceId, question.getPoints()));
                                    alreadyPresent = true;
                                    presentAt = i;
                                    break;
                                }
                            }
                            if (!alreadyPresent){
                                saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), correctAnsIds, givenAnswersIds, resourceId, question.getPoints()));
                            }
                        }else {
                            saveAnswers.add(new QuizSaveAnswers(quizOption.getOptionId(), question.getPuzzleId(), question.getPuzzleQuestionId(), correctAnsIds, givenAnswersIds, resourceId, question.getPoints()));
                        }
                        if (givenAnswersIds.equalsIgnoreCase(correctAnsIds)){
                            points = points + question.getPoints();
                            mFragmentQuizBinding.questionCounter.setText(points+"");
                        }
                    }
                    if (question.getCorrectAnswerIds().contains(quizOption.getOptionId().toString())) {
                        //points = points + question.getPoints();
                    }
                    if (!collected.isEmpty()){
                        mFragmentQuizBinding.confirmBtn.setEnabled(true);
                    }else {
                        mFragmentQuizBinding.confirmBtn.setEnabled(false);
                    }
                    mFragmentQuizBinding.confirmBtn.setOnClickListener(v -> {
                        isConfirmPressedOnce = true;
                        mAdapter.showCorrect(position, quizOption, "", optionType, holder);
                        mFragmentQuizBinding.confirmBtn.setVisibility(View.GONE);
                        mFragmentQuizBinding.nextBtn.setEnabled(true);
                        collected.clear();
                        mFragmentQuizBinding.confirmBtn.setEnabled(false);
                        if (optionType.equalsIgnoreCase("radio")) {
                            if (!saveAnswers.isEmpty()) {
                                for (int i = 0; i < saveAnswers.size(); i++) {
                                    if (saveAnswers.get(i).getPuzzleQuestionId().equals(question.getPuzzleQuestionId())) {
                                        if (question.getCorrectAnswerIds().equalsIgnoreCase(saveAnswers.get(i).getResourceGivenAnswerIds())) {
                                            Log.d("TAG", "Answer: " + question.getCorrectAnswerIds() + " Resource Given: "+ saveAnswers.get(i).getResourceGivenAnswerIds());
                                            points = points + question.getPoints();
                                            mFragmentQuizBinding.questionCounter.setText(points+"");
                                        }
                                    }
                                }
                            }
                        }

                    });
                    mFragmentQuizBinding.questionCounter.setText(points+"");
                    Log.d("TAG-Save", saveAnswers.toString());
                });
                mFragmentQuizBinding.recycleView.setAdapter(mAdapter);
            }

            if (question.getPuzzleQuestionType().equals("Video")) {
                timer.cancel();
                mFragmentQuizBinding.confirmBtn.setVisibility(View.GONE);
                mFragmentQuizBinding.timer.setVisibility(View.INVISIBLE);
                mFragmentQuizBinding.cardImage.setVisibility(View.GONE);
                mFragmentQuizBinding.imgInfoTv.setVisibility(View.GONE);
                mFragmentQuizBinding.question.setVisibility(View.GONE);
                mFragmentQuizBinding.videoQuestion.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.imageQuestionTitle.setVisibility(View.GONE);

            } else if (question.getPuzzleQuestionType().equals("Image")) {
                timer.cancel();
                Picasso.get().load(question.getPuzzleQuestionURL()).placeholder(R.drawable.sample).into(mFragmentQuizBinding.imgQuestion);
                mFragmentQuizBinding.confirmBtn.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.imageQuestionTitle.setText(question.getPuzzleQuestionTitleDisplay());
                mFragmentQuizBinding.timer.setVisibility(View.INVISIBLE);
                mFragmentQuizBinding.question.setVisibility(View.GONE);
                mFragmentQuizBinding.cardImage.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.imgInfoTv.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.videoQuestion.setVisibility(View.GONE);
                mFragmentQuizBinding.imageQuestionTitle.setVisibility(View.VISIBLE);
            } else {
                timer.cancel();
                mFragmentQuizBinding.cardImage.setVisibility(View.GONE);
                mFragmentQuizBinding.imgInfoTv.setVisibility(View.GONE);
                mFragmentQuizBinding.question.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.confirmBtn.setVisibility(View.VISIBLE);
                mFragmentQuizBinding.videoQuestion.setVisibility(View.GONE);
                mFragmentQuizBinding.imageQuestionTitle.setVisibility(View.GONE);
                mFragmentQuizBinding.question.setText(question.getPuzzleQuestionTitleDisplay());
            }
            /*mAdapter.setOnItemClickHandler(position -> {
                if (timer != null)
                    timer.cancel();
            });*/
        }
    }


    private void initializePlayer() {
        try {
            if (player == null) {
                videoSurfaceView = new PlayerView(getActivity());
                videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                // 1. Create a default TrackSelector
                LoadControl loadControl = new DefaultLoadControl(
                        new DefaultAllocator(true, 16),
                        MIN_BUFF_MS,
                        MAX_BUFF_MS,
                        BUFF_PLAYBACK_MS,
                        REBUFF_MS,
                        TARGET_BUFF_BYTES,
                        true);

                BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                TrackSelection.Factory videoTrackSelectionFactory =
                        new AdaptiveTrackSelection.Factory(bandwidthMeter);
                TrackSelector trackSelector =
                        new DefaultTrackSelector(videoTrackSelectionFactory);
                // 2. Create the player
                player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(getActivity()), trackSelector, loadControl);
                mFragmentQuizBinding.videoQuestion.setPlayer(player);

                // Bind the player to the view.
                videoSurfaceView.setUseController(true);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void buildMediaSource(Uri mUri) {
        try {
            // Measures bandwidth during playback. Can be null if not required.
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireActivity(),
                    Util.getUserAgent(getActivity(), getString(R.string.app_name)), bandwidthMeter);
            // This is the MediaSource representing the media to be played.
            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mUri);
            // Prepare the player with the source.
            player.prepare(videoSource);
            player.setPlayWhenReady(true);
            player.addListener(this);
        } catch (Exception e) {
            RealmResults<LoginResponse> mLoginRealmModels = BaseApplication.getRealm().where(LoginResponse.class).findAll();
            if (mLoginRealmModels != null && mLoginRealmModels.size() > 0) {
                assert mLoginRealmModels.get(0) != null;
                String userName = "TECHNICIAN NAME : " + Objects.requireNonNull(mLoginRealmModels.get(0)).getUserName();
                String lineNo = String.valueOf(new Exception().getStackTrace()[0].getLineNumber());
                String DeviceName = "DEVICE_NAME : " + Build.DEVICE + ", DEVICE_VERSION : " + Build.VERSION.SDK_INT;
                AppUtils.sendErrorLogs(e.toString(), getClass().getSimpleName(), "buildMediaSource", lineNo, userName, DeviceName);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            buildMediaSource(Uri.parse(question.getPuzzleQuestionURL()));
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    private void getPuzzleStatsForRes(){
        NetworkCallController controller = new NetworkCallController(this);
        controller.setListner(new NetworkResponseListner<QuizPuzzleStats>(){
            @Override
            public void onResponse(int requestCode, QuizPuzzleStats response) {
                Log.d("TAG", response+"");
                if (response != null) {
                    if (response.isSuccess()) {
                        oldScore = response.getData().getPoints();
                    }
                }
            }

            @Override
            public void onFailure(int requestCode) {

            }
        });
        controller.getPuzzleStatsForRes(202122, resourceId, LocaleHelper.getLanguage(requireContext()));
    }
}
