package cube.ware.ui.chat.panel.input.function;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.common.sdk.RouterUtil;
import com.common.utils.utils.ClickUtil;
import com.common.utils.utils.ToastUtil;
import com.common.utils.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.List;

import cube.data.model.reponse.WhiteBoardData;
import cube.service.CubeEngine;
import cube.service.common.CubeCallback;
import cube.service.common.model.CubeError;
import cube.service.conference.model.Conference;
import cube.service.whiteboard.model.Whiteboard;
import cube.ware.AppConstants;
import cube.ware.CubeUI;
import cube.ware.R;
import cube.ware.data.model.dataModel.enmu.CallStatus;
import cube.ware.data.model.dataModel.enmu.CubeSessionType;
import cube.ware.service.call.manager.OneOnOneCallManager;
import cube.ware.ui.chat.ChatContainer;
import cube.ware.utils.SpUtil;

/**
 * @author Wangxx
 * @date 2017/2/7
 */

public class AudioFunction extends BaseFunction {

    private CubeSessionType mCubeSessionType ;
    private ChatContainer mChatContainer;

    /**
     * 构造方法
     *
     * @param mSessionType
     */
    public AudioFunction(CubeSessionType mSessionType, ChatContainer container) {
        super(R.drawable.selector_chat_function_voice_btn, R.string.voice_chat);
        this.mCubeSessionType = mSessionType;
        this.mChatContainer =container;
    }

    @Override
    public void onClick() {
        if(mCubeSessionType.equals(CubeSessionType.P2P)){//单聊
            if (OneOnOneCallManager.getInstance().isCalling()){
                ToastUtil.showToast(getActivity(),R.string.calling_please_try_again_later);
            }else{
                Bundle bundle = new Bundle();
                bundle.putString("call_id",mChatContainer.mChatId);
                bundle.putSerializable("call_state", CallStatus.AUDIO_OUTGOING);
                bundle.putLong("call_time",0l);
                ARouter.getInstance().build(AppConstants.Router.P2PCallActivity).withBundle("call_data",bundle).navigation();
            }
        }else {//群聊
            if(ClickUtil.isFastClick()){ //重复点击
                if (CubeUI.getInstance().isCalling()){
                    ToastUtil.showToast(getActivity(),R.string.calling_please_try_again_later);
                }else{
                    LogUtil.d("===调用了这个方法吗==");
                    isHasConference();
                }
            }
        }
    }

    private void isHasConference(){

        List<String> list=new ArrayList<>();
        list.add(getChatId());
        CubeEngine.getInstance().getConferenceService().queryConferencesByGroupIds(list, new CubeCallback<List<Conference>>() {
            @Override
            public void onSucceed(List<Conference> conferenceList) {
                if(conferenceList!=null&&conferenceList.size()>0){
                    ToastUtil.showToast(getActivity(),"当前存在会议");
                }else {
                    isHasWhiteBoard();
                }
            }

            @Override
            public void onFailed(CubeError error) {
                //当前群组没有会议
                LogUtil.d("====查询会议没有===");
                isHasWhiteBoard();
            }
        });

//        CubeEngine.getInstance().getConferenceService().queryConferenceDetails(getChatId(), new CubeCallback<Conference>() {
//            @Override
//            public void onSucceed(Conference conference) {
//                if(conference!=null){
//                    ToastUtil.showToast(getActivity(),"当前存在会议");
//                }
//            }
//            @Override
//            public void onFailed(CubeError error) {
//                //当前群组没有会议
//                LogUtil.d("====查询会议没有===");
//                isHasWhiteBoard();
//            }
//        });
    }
    private void isHasWhiteBoard(){
        List<String> groupId = new ArrayList<>();
        groupId.add(getChatId());
        LogUtil.d("===群组id=="+getChatId());
        LogUtil.d("===cubid=="+SpUtil.getCubeId());
        LogUtil.d("===token=="+SpUtil.getCubeToken());
        CubeEngine.getInstance().getWhiteboardService().queryWhiteboardByGroupId(groupId, SpUtil.getCubeId(), new CubeCallback<WhiteBoardData>() {
            @Override
            public void onSucceed(WhiteBoardData whiteBoardData) {
                if (whiteBoardData.whiteboards.size() != 0 && null != whiteBoardData.whiteboards.get(0)){
                    ToastUtil.showToast(getActivity(),"当前存在白板");
                }else if (whiteBoardData.whiteboards.size() == 0){
                    //表示当前群没有白板，跳入选择成员页面
                    LogUtil.d("===没有白板==");
                    Bundle bundle=new Bundle();
                    bundle.putInt("select_type",2);//视频音频会议首次创建
                    bundle.putString("group_id",mChatContainer.mChatId); //mChatId 就是 groupId
                    RouterUtil.navigation(AppConstants.Router.SelectMemberActivity,bundle);
                }
            }
            @Override
            public void onFailed(CubeError error) {
                LogUtil.d("===查询白板失败了=="+error.code+"==="+error.desc);
//                Bundle bundle=new Bundle();
//                bundle.putInt("select_type",2);//视频音频会议首次创建
//                bundle.putString("group_id",mChatContainer.mChatId); //mChatId 就是 groupId
//                RouterUtil.navigation(AppConstants.Router.SelectMemberActivity,bundle);
            }
        });
    }



}
