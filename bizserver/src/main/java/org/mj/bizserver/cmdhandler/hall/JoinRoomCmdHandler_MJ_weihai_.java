package org.mj.bizserver.cmdhandler.hall;

import io.netty.channel.ChannelHandlerContext;
import org.mj.bizserver.allmsg.HallServerProtocol;
import org.mj.bizserver.allmsg.InternalServerMsg;
import org.mj.bizserver.allmsg.MJ_weihai_Protocol;
import org.mj.bizserver.cmdhandler.game.MJ_weihai_.GameBroadcaster;
import org.mj.bizserver.foundation.BizResultWrapper;
import org.mj.bizserver.mod.game.MJ_weihai_.MJ_weihai_BizLogic;
import org.mj.bizserver.mod.game.MJ_weihai_.bizdata.Player;
import org.mj.bizserver.mod.game.MJ_weihai_.bizdata.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * 加入房间指令处理器 - 威海麻将
 */
class JoinRoomCmdHandler_MJ_weihai_ {
    /**
     * 日志对象
     */
    static private final Logger LOGGER = LoggerFactory.getLogger(JoinRoomCmdHandler_MJ_weihai_.class);

    /**
     * 处理消息指令
     *
     * @param ctx             信道处理器上下文
     * @param remoteSessionId 远程会话 Id
     * @param fromUserId      来自用户 Id
     * @param cmdObj          指令对象
     */
    static void handle(
        ChannelHandlerContext ctx, int remoteSessionId, int fromUserId, HallServerProtocol.JoinRoomCmd cmdObj) {

        // 获取房间 Id
        final int roomId = cmdObj.getRoomId();

        if (!MJ_weihai_BizLogic.getInstance().hasRoom(roomId)) {
            LOGGER.error(
                "房间 Id 不存在, userId = {}, roomId = {}",
                fromUserId, roomId
            );
            return;
        }

        MJ_weihai_BizLogic.getInstance().joinRoom_async(
            fromUserId,
            roomId,
            (resultX) -> buildMsgAndSend(ctx, remoteSessionId, fromUserId, resultX)
        );
    }

    /**
     * 构建消息并发送
     *
     * @param ctx             客户端信道处理器上下文
     * @param remoteSessionId 远程会话 Id
     * @param fromUserId      来自用户 Id
     * @param resultX         业务结果
     */
    static private void buildMsgAndSend(
        ChannelHandlerContext ctx, int remoteSessionId, int fromUserId, BizResultWrapper<Room> resultX) {
        // 构建结果并发送
        buildResultAndSend(
            ctx, remoteSessionId, fromUserId, resultX
        );
        // 构建广播并发送
        buildBroadcastAndSend(
            ctx, fromUserId, resultX
        );
    }

    /**
     * 构建结果并发送
     *
     * @param ctx             信道处理器上下文
     * @param remoteSessionId 远程会话 Id
     * @param fromUserId      来自用户 Id
     * @param resultX         业务结果
     */
    static private void buildResultAndSend(
        ChannelHandlerContext ctx, int remoteSessionId, int fromUserId, BizResultWrapper<Room> resultX) {
        if (null == ctx ||
            null == resultX) {
            return;
        }

        InternalServerMsg newMsg = new InternalServerMsg();
        newMsg.setRemoteSessionId(remoteSessionId);
        newMsg.setFromUserId(fromUserId);

        if (0 != newMsg.admitError(resultX)) {
            ctx.writeAndFlush(newMsg);
            return;
        }

        // 添加到广播器
        GameBroadcaster.add(ctx, remoteSessionId, fromUserId);

        // 获取加入房间
        Room joinedRoom = resultX.getFinalResult();

        if (null == joinedRoom) {
            LOGGER.error("加入房间为空, userId = {}", fromUserId);
            return;
        }

        // 构建加入房间结果
        HallServerProtocol.JoinRoomResult.Builder b0 = HallServerProtocol.JoinRoomResult.newBuilder();
        b0.setRoomId(joinedRoom.getRoomId());
        b0.setGameType0(joinedRoom.getGameType0().getIntVal());
        b0.setGameType1(joinedRoom.getGameType1().getIntVal());

        // 获取规则字典
        final Map<Integer, Integer> ruleMap = joinedRoom.getRuleSetting().getInnerMap();

        for (Map.Entry<Integer, Integer> entry : ruleMap.entrySet()) {
            b0.addRuleItem(
                HallServerProtocol.KeyAndVal.newBuilder()
                    .setKey(entry.getKey())
                    .setVal(entry.getValue())
            );
        }

        HallServerProtocol.JoinRoomResult r = b0.build();

        newMsg.putProtoMsg(r);
        ctx.writeAndFlush(newMsg);
    }

    /**
     * 构建广播消息
     *
     * @param ctx        信道处理器上下文
     * @param fromUserId 来自用户 Id
     * @param resultX    业务结果
     */
    static private void buildBroadcastAndSend(
        ChannelHandlerContext ctx, int fromUserId, BizResultWrapper<Room> resultX) {
        if (null == ctx ||
            null == resultX ||
            0 != resultX.getErrorCode()) {
            return;
        }

        // 获取加入房间
        Room joinedRoom = resultX.getFinalResult();

        if (null == joinedRoom) {
            LOGGER.error("加入房间为空, userId = {}", fromUserId);
            return;
        }

        // 获取房间内的玩家
        Player p = joinedRoom.getPlayerByUserId(fromUserId);

        if (null == p) {
            LOGGER.error(
                "玩家没有加入该房间, userId = {}, roomId = {}",
                fromUserId,
                joinedRoom.getRoomId()
            );
            return;
        }

        // 构建加入房间广播
        MJ_weihai_Protocol.JoinRoomBroadcast.Builder b = MJ_weihai_Protocol.JoinRoomBroadcast.newBuilder()
            .setUserId(p.getUserId())
            .setUserName(Objects.requireNonNullElse(p.getUserName(), ""))
            .setHeadImg(Objects.requireNonNullElse(p.getHeadImg(), ""))
            .setSex(p.getSex())
            .setClientIpAddr(Objects.requireNonNullElse(p.getClientIpAddr(), ""))
            .setSeatIndex(p.getSeatIndex())
            .setCurrScore(p.getCurrScore())
            .setTotalScore(p.getTotalScore());

        MJ_weihai_Protocol.JoinRoomBroadcast broadcast = b.build();

        // 广播消息
        GameBroadcaster.broadcast(
            joinedRoom,
            broadcast
        );
    }
}
