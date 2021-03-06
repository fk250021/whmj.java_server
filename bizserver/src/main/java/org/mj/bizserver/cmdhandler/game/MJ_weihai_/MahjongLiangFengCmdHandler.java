package org.mj.bizserver.cmdhandler.game.MJ_weihai_;

import io.netty.channel.ChannelHandlerContext;
import org.mj.bizserver.allmsg.MJ_weihai_Protocol;
import org.mj.bizserver.foundation.BizResultWrapper;
import org.mj.bizserver.mod.game.MJ_weihai_.MJ_weihai_BizLogic;
import org.mj.bizserver.mod.game.MJ_weihai_.bizdata.MahjongTileDef;
import org.mj.bizserver.mod.game.MJ_weihai_.report.ReporterTeam;
import org.mj.comm.cmdhandler.ICmdHandler;

/**
 * 麻将亮风指令处理器
 */
public final class MahjongLiangFengCmdHandler extends AbstractInGameCmdHandler<MJ_weihai_Protocol.MahjongLiangFengCmd>
    implements ICmdHandler<MJ_weihai_Protocol.MahjongLiangFengCmd> {

    @Override
    public void handle(
        ChannelHandlerContext ctx,
        int remoteSessionId,
        int fromUserId,
        MJ_weihai_Protocol.MahjongLiangFengCmd cmdObj) {
        // 将复杂工作交给代理, 我只执行省心的调用
        super.doProxyInvoke(ctx, remoteSessionId, fromUserId, cmdObj);
    }

    @Override
    protected void doEasyInvoke(
        int fromUserId, MJ_weihai_Protocol.MahjongLiangFengCmd cmdObj, BizResultWrapper<ReporterTeam> resultX) {
        // 麻将亮风
        MJ_weihai_BizLogic.getInstance().liangFeng(
            fromUserId,
            MahjongTileDef.valueOf(cmdObj.getT0()),
            MahjongTileDef.valueOf(cmdObj.getT1()),
            MahjongTileDef.valueOf(cmdObj.getT2()),
            resultX
        );
    }
}
