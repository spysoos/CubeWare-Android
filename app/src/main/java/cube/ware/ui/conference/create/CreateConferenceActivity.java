package cube.ware.ui.conference.create;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectChangeListener;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.common.mvp.base.BaseActivity;
import com.common.sdk.RouterUtil;
import com.common.utils.utils.ClickUtil;
import com.common.utils.utils.ScreenUtil;
import com.common.utils.utils.ToastUtil;
import com.common.utils.utils.log.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cube.service.CubeEngine;
import cube.service.common.model.CubeError;
import cube.service.conference.model.Conference;
import cube.service.conference.model.ConferenceConfig;
import cube.service.group.GroupType;
import cube.service.user.model.User;
import cube.ware.AppConstants;
import cube.ware.R;
import cube.ware.data.model.dataModel.enmu.CallStatus;
import cube.ware.service.conference.ConferenceHandle;
import cube.ware.service.conference.ConferenceStateListener;
import cube.ware.ui.conference.adapter.RVCreateAdapter;
import cube.ware.ui.conference.create.dialog.BottomDatePicker;
import cube.ware.ui.conference.eventbus.CreateConferenceEvent;
import cube.ware.ui.conference.eventbus.InviteConferenceEvent;
import cube.ware.ui.conference.eventbus.SelectMemberEvent;
import rx.functions.Action1;

/**
 * Created by kun
 * Des:
 * Date: 2018/8/27.
 */
@Route(path= AppConstants.Router.CreateConferenceActivity)
public class CreateConferenceActivity extends BaseActivity<CreatePresenter> implements CreateContract.View, TextWatcher, ConferenceStateListener {

    private RecyclerView mRvMenber;
    private TextView mTvComplete;
    private TextView mTvBack;
    private RVCreateAdapter mRvCreateAdapter;
    private Button mBtCreate;
    private List<User> users=new ArrayList<>();
    private EditText mEtTitle;
    private User mUser=new User();
    private String TAG="CreateConferenceActivity";
    private Conference mConference;
    private ArrayList<String> mInviteList=new ArrayList<>();
    private int type;
    private ProgressDialog mProgressDialog;
    private Conference conference;
    private LinearLayout mRlStartTime;
    private LinearLayout mRlConferenceDurtion;
    private TextView mTvStartTime;
    private TextView mTvConferenceDurtion;
    private int mIndexTime;
    private long mTimeStamps;
    private TextView mTvTitle;

    @Override
    protected int getContentViewId() {
        return R.layout.activity_create_conference;
    }

    @Override
    protected CreatePresenter createPresenter() {
        return new CreatePresenter(this,this);
    }

    @Override
    protected void initView() {
        Bundle bundleExtra = getIntent().getBundleExtra(AppConstants.Value.BUNDLE);
        type=  bundleExtra.getInt("type");
        conference= (Conference) bundleExtra.getSerializable("conference");
        EventBus.getDefault().register(this);
        mRvMenber = findViewById(R.id.rv_member);
        mTvBack = findViewById(R.id.title_back);
        mTvComplete = findViewById(R.id.title_complete);
        mEtTitle = findViewById(R.id.et_conference_title);
        mBtCreate=findViewById(R.id.bt_create);
        mTvTitle = findViewById(R.id.toolbar_title);
        mRlStartTime = findViewById(R.id.rl_start_time);
        mRlConferenceDurtion = findViewById(R.id.rl_conference_time);
        mTvStartTime = findViewById(R.id.tv_start_time);
        mTvConferenceDurtion = findViewById(R.id.tv_conference_time);

        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,6);
        mRvMenber.setLayoutManager(gridLayoutManager);
        mRvCreateAdapter = new RVCreateAdapter(this,users);
        mRvMenber.setAdapter(mRvCreateAdapter);
        //添加监听
        ConferenceHandle.getInstance().addConferenceStateListener(this);
        mUser=CubeEngine.getInstance().getSession().getUser();
        if(type==1){ //邀请
            mBtCreate.setVisibility(View.GONE);
            mTimeStamps=System.currentTimeMillis();
            //添加默认时间
            mTvStartTime.setText(mPresenter.dataToString(new Date(mTimeStamps),this));
        }else {//创建
            mTvComplete.setVisibility(View.GONE);
            mTvTitle.setText(R.string.conference_details);
            mEtTitle.setText(conference.displayName);
            mEtTitle.setEnabled(false);
            setInviteData();
            Date date=new Date(conference.startTime);
            String startTime = mPresenter.dataToString(date, this);
            mTvStartTime.setText(startTime);
            mTvConferenceDurtion.setText(conference.duration/1000/60+"分钟");
            mTvStartTime.setTextColor(getResources().getColor(R.color.C1));
            mTvConferenceDurtion.setTextColor(getResources().getColor(R.color.C1));
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("创建中。。。");
        //去除所有空格
//        mEtTitle.setFilters(new InputFilter[]{getFilter()});
    }

    private void setInviteData() {
        mPresenter.getUserDataListFromMem(conference.invites).subscribe(new Action1<List<User>>() {
            @Override
            public void call(List<User> userList) {
                //updata adapter
                getInviteDate(userList,null);
            }
        });
    }

    @Override
    protected void initListener() {
        //返回
        mTvBack.setOnClickListener(this);
        //完成
        mTvComplete.setOnClickListener(this);
        //创建
        mBtCreate.setOnClickListener(this);
        mEtTitle.addTextChangedListener(this);
        if(type==1){
            mRlStartTime.setOnClickListener(this);
            mRlConferenceDurtion.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            //创建
            case R.id.bt_create:
                //别人创建的，可以拿到会议，直接加入的会议
                Bundle bundle=new Bundle();
                if(conference.conferenceId.equals(conference.bindGroupId)){
                    //不依赖群
                    bundle.putString(AppConstants.Value.CONFERENCE_GROUP_ID,"");
                }else {
                    bundle.putString(AppConstants.Value.CONFERENCE_GROUP_ID,conference.bindGroupId);
                }
                bundle.putString(AppConstants.Value.CONFERENCE_INVITE_Id, conference.founder); //发起者
                bundle.putSerializable(AppConstants.Value.CONFERENCE_CALLSTATA, CallStatus.GROUP_CALL_JOIN);
                bundle.putSerializable(AppConstants.Value.CONFERENCE_CONFERENCE,conference);
                RouterUtil.navigation(this,bundle,AppConstants.Router.ConferenceActivity);
                finish();
                break;
                //back
            case R.id.title_back:
                closeSoftKey();
                if(mConference!=null){
                    CubeEngine.getInstance().getConferenceService().destroy(mConference.conferenceId);
                }
                finish();
                break;
                //完成
            case R.id.title_complete:
                if(ClickUtil.isFastClick()){
                    closeSoftKey();
                    if(TextUtils.isEmpty(mEtTitle.getText().toString().trim())){
                        showMessage(getString(R.string.please_add_conference_theme));
                        return;
                    }
                    if (users.size()<=2){
                        showMessage(getString(R.string.please_invite_conference_member));
                        return;
                    }
                    if (mTimeStamps==0){
                        showMessage("请选择会议时间");
                        return;
                    }
                    //(mIndexTime+1)*15 每次增加15分钟
                    ConferenceConfig conferenceConfig = mPresenter.initConferenceConfig(mEtTitle.getText().toString(),"",mTimeStamps,(mIndexTime+1)*15*60*1000,true, mInviteList);
                    CubeEngine.getInstance().getConferenceService().create(conferenceConfig);
                    mProgressDialog.show();
                }
                break;
            case R.id.rl_start_time:
                mPresenter.initTimePicker(this);
                break;
            case R.id.rl_conference_time:
                BottomDatePicker bottomDatePicker=BottomDatePicker.getInstance();
                bottomDatePicker.show(getSupportFragmentManager(),"");
                break;
        }
    }

    @Override
    public void onTimeSelect(Date date,String startTime,long timeStamps){
        mTimeStamps = timeStamps;
        mTvStartTime.setText(startTime);
        mTvStartTime.setTextColor(getResources().getColor(R.color.C1));
    }

    @Override
    public void onDurationSelect(String startTime,int index){
        mTvConferenceDurtion.setText(startTime);
        mTvConferenceDurtion.setTextColor(getResources().getColor(R.color.C1));
        mIndexTime = index;
    }

    @Override
    protected void initData() {
        if(type==1){
            addSelf();
            addDefault();
            mRvCreateAdapter.setData(users);
        }
    }

    /**
     * 由SelectMemberActivity传过来的数据
     * @param selectMemberEvent
     */
    @Subscribe
    public void getCubeUserList(SelectMemberEvent selectMemberEvent){
        users.clear();
        mPresenter.changeDate(selectMemberEvent.getCubeUserList());
    }

    @Override
    public void getInviteDate(List<User> userList, ArrayList<String> invitelist) {
        this.mInviteList=invitelist;
        if(type==1){
            addSelf();
        }
        users.addAll(userList);
        if(type==1){
            addDefault();
        }
        mRvCreateAdapter.setData(users);
        updateCompleteBtn();
    }

    @Override
    public void updateCompleteBtn() {
        if(!TextUtils.isEmpty(mEtTitle.getText().toString().trim())&&users.size()>2){
            mTvComplete.setTextColor(this.getResources().getColor(R.color.cube_primary));
        }else {
            mTvComplete.setTextColor(this.getResources().getColor(R.color.C4));
        }
    }

    protected void addSelf() {
        User user = CubeEngine.getInstance().getSession().getUser();
        users.add(user);
    }

    private void addDefault(){
        User user=new User();
        user.cubeId="default";
        user.displayName="default";
        user.avatar="default";
        users.add(user);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        ConferenceHandle.getInstance().removeConferenceStateListener(this);
    }

    @Override
    public void showMessage(String message) {
        ToastUtil.showToast(this,message);
    }

    //edittext的监听
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //更新按钮状态
        updateCompleteBtn();
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    //会议创建的监听
    @Override
    public void onConferenceCreated(Conference conference, User from) {
        EventBus.getDefault().post(new InviteConferenceEvent(conference));
        if(mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
        finish();
    }

    @Override
    public void onConferenceDestroyed(Conference conference, User from) {

    }

    @Override
    public void onConferenceInvited(Conference conference, User from, List<User> invites) {

    }

    @Override
    public void onConferenceRejectInvited(Conference conference, User from, User rejectMember) {

    }

    @Override
    public void onConferenceAcceptInvited(Conference conference, User from, User joinedMember) {

    }

    @Override
    public void onConferenceJoined(Conference conference, User joinedMember) {
           mProgressDialog.setMessage("加入成功,进入会议中。。。");
            //必须调用会议控制
            mConference=conference;
            CubeEngine.getInstance().getConferenceService().addControlAudio(conference.conferenceId,mUser.cubeId);
    }

    @Override
    public void onVideoEnabled(Conference conference, boolean videoEnabled) {
            mConference=conference;
            mProgressDialog.dismiss();
            //跳转到会议界面 c4410400
            Bundle bundle=new Bundle();
            bundle.putSerializable(AppConstants.Value.CONFERENCE_CALLSTATA, CallStatus.GROUP_VIDEO_CALLING);//创建者
            bundle.putStringArrayList(AppConstants.Value.CONFERENCE_INVITE_LIST,mInviteList);//传递对象集合
            bundle.putSerializable(AppConstants.Value.CONFERENCE_CONFERENCE,mConference);
            RouterUtil.navigation(this,bundle,AppConstants.Router.ConferenceActivity);
            finish();
    }

    @Override
    public void onAudioEnabled(Conference conference, boolean videoEnabled) {
    }

    @Override
    public void onConferenceUpdated(Conference conference) {

    }

    @Override
    public void onConferenceQuited(Conference conference, User quitMember) {

    }

    @Override
    public void onConferenceFailed(Conference conference, CubeError error) {
//        showMessage(error.toString());
        LogUtil.i(TAG,error.toString());
    }

    public void closeSoftKey(){
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
