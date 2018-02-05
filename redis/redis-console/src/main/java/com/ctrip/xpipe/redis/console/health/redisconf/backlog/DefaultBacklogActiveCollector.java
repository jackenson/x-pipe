package com.ctrip.xpipe.redis.console.health.redisconf.backlog;

import com.ctrip.xpipe.api.server.Server;
import com.ctrip.xpipe.endpoint.HostPort;
import com.ctrip.xpipe.redis.console.alert.ALERT_TYPE;
import com.ctrip.xpipe.redis.console.alert.AlertManager;
import com.ctrip.xpipe.redis.console.health.Sample;
import com.ctrip.xpipe.redis.console.health.redisconf.RedisInfoUtils;
import com.ctrip.xpipe.redis.core.protocal.pojo.RedisInfo;
import com.ctrip.xpipe.utils.StringUtil;
import com.ctrip.xpipe.utils.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author chen.zhu
 * <p>
 * Feb 05, 2018
 */
@Component
@Lazy
public class DefaultBacklogActiveCollector implements BacklogActiveCollector {

    private static Logger logger = LoggerFactory.getLogger(DefaultBacklogActiveCollector.class);

    @Autowired
    private AlertManager alertManager;

    @Override
    public void collect(Sample<InstanceInfoReplicationResult> sample) {

        BacklogActiveSamplePlan samplePlan = (BacklogActiveSamplePlan) sample.getSamplePlan();
        String clusterId = samplePlan.getClusterId();
        String shardId = samplePlan.getShardId();

        samplePlan.getHostPort2SampleResult().forEach((hostPort, sampleResult) -> {
            if(sampleResult.isSuccess()) {

                String context = sampleResult.getContext();
                if(context == null || StringUtil.isEmpty(context)) {
                    logger.warn("[collect]Null String of Redis info, {} {} {}", clusterId, shardId, hostPort);
                    return;
                }
                analysisInfoReplication(sampleResult.getContext(), clusterId, shardId, hostPort);
            } else {
                logger.error("[collect]get Redis info replication, execution error: {}", sampleResult.getFailReason());
            }
        });
    }

    @VisibleForTesting
    void analysisInfoReplication(String infoReplication, String cluster, String shard, HostPort hostPort) {
        boolean isBacklogActive = RedisInfoUtils.getReplBacklogActive(infoReplication);
        if(!isBacklogActive) {
            String message = "Redis replication backlog not active";
            alertManager.alert(cluster, shard, hostPort, ALERT_TYPE.REPL_BACKLOG_NOT_ACTIVE, message);
        }
    }
}